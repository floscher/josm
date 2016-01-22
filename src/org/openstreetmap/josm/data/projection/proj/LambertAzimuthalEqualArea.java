// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.projection.proj;

import static org.openstreetmap.josm.tools.I18n.tr;

import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.projection.ProjectionConfigurationException;

/**
 * Lambert Azimuthal Equal Area (EPSG code 9820).
 * <p>
 * This class has been derived from the implementation of the Geotools project;
 * git 8cbf52d, org.geotools.referencing.operation.projection.LambertAzimuthalEqualArea
 * at the time of migration.
 * <p>
 * <b>References:</b>
 * <ul>
 *   <li> A. Annoni, C. Luzet, E.Gubler and J. Ihde - Map Projections for Europe</li>
 *   <li> John P. Snyder (Map Projections - A Working Manual,
 *        U.S. Geological Survey Professional Paper 1395)</li>
 * </ul>
 *
 * @author Gerald Evenden  (for original code in Proj4)
 * @author Beate Stollberg
 * @author Martin Desruisseaux
 *
 * @see <A HREF="http://mathworld.wolfram.com/LambertAzimuthalEqual-AreaProjection.html">Lambert Azimuthal Equal-Area Projection</A>
 * @see <A HREF="http://www.remotesensing.org/geotiff/proj_list/lambert_azimuthal_equal_area.html">"Lambert_Azimuthal_Equal_Area"</A>
 */
public class LambertAzimuthalEqualArea extends AbstractProj {

    /** Maximum difference allowed when comparing real numbers. */
    private static final double EPSILON = 1E-7;

    /** Epsilon for the comparison of small quantities. */
    private static final double FINE_EPSILON = 1E-10;

    /** Epsilon for the comparison of latitudes. */
    private static final double EPSILON_LATITUDE = 1E-10;

    /** Constants for authalic latitude. */
    private static final double P00 = 0.33333333333333333333;
    private static final double P01 = 0.17222222222222222222;
    private static final double P02 = 0.10257936507936507936;
    private static final double P10 = 0.06388888888888888888;
    private static final double P11 = 0.06640211640211640211;
    private static final double P20 = 0.01641501294219154443;

    /** The projection mode. */
    private enum Mode { OBLIQUE, EQUATORIAL, NORTH_POLE, SOUTH_POLE }

    /** The projection mode for this particular instance. */
    private Mode mode;

    /** Constant parameters. */
    private double sinb1, cosb1, xmf, ymf, mmf, qp, dd, rq;

    /** Coefficients for authalic latitude. */
    private double APA0, APA1, APA2;

    private double latitudeOfOrigin;

    @Override
    public String getName() {
        return tr("Lambert Azimuthal Equal Area");
    }

    @Override
    public String getProj4Id() {
        return "laea";
    }

    @Override
    public void initialize(ProjParameters params) throws ProjectionConfigurationException {
        super.initialize(params);

        if (params.lat0 == null)
            throw new ProjectionConfigurationException(tr("Parameter ''{0}'' required.", "lat_0"));

        latitudeOfOrigin = Math.toRadians(params.lat0);
        /*
         * Detects the mode (oblique, etc.).
         */
        final double t = Math.abs(latitudeOfOrigin);
        if (Math.abs(t - Math.PI/2) < EPSILON_LATITUDE) {
            mode = latitudeOfOrigin < 0.0 ? Mode.SOUTH_POLE : Mode.NORTH_POLE;
        } else if (Math.abs(t) < EPSILON_LATITUDE) {
            mode = Mode.EQUATORIAL;
        } else {
            mode = Mode.OBLIQUE;
        }
        /*
         * Computes the constants for authalic latitude.
         */
        final double es2 = e2 * e2;
        final double es3 = e2 * es2;
        APA0 = P02 * es3 + P01 * es2 + P00 * e2;
        APA1 = P11 * es3 + P10 * es2;
        APA2 = P20 * es3;

        final double sinphi;
        qp     = qsfn(1);
        rq     = Math.sqrt(0.5 * qp);
        mmf    = 0.5 / (1 - e2);
        sinphi = Math.sin(latitudeOfOrigin);
        sinb1 = qsfn(sinphi) / qp;
        cosb1 = Math.sqrt(1.0 - sinb1 * sinb1);
        switch (mode) {
            case NORTH_POLE:  // Fall through
            case SOUTH_POLE: {
                dd  = 1.0;
                xmf = ymf = rq;
                break;
            }
            case EQUATORIAL: {
                dd  = 1.0 / rq;
                xmf = 1.0;
                ymf = 0.5 * qp;
                break;
            }
            case OBLIQUE: {
                dd  = Math.cos(latitudeOfOrigin) /
                        (Math.sqrt(1.0 - e2 * sinphi * sinphi) * rq * cosb1);
                xmf = rq * dd;
                ymf = rq / dd;
                break;
            }
            default: {
                throw new AssertionError(mode);
            }
        }
    }

    @Override
    public double[] project(final double phi, final double lambda) {
        final double coslam = Math.cos(lambda);
        final double sinlam = Math.sin(lambda);
        final double sinphi = Math.sin(phi);
        double q = qsfn(sinphi);
        final double sinb, cosb, b, c, x, y;
        switch (mode) {
            case OBLIQUE: {
                sinb = q / qp;
                cosb = Math.sqrt(1.0 - sinb * sinb);
                c    = 1.0 + sinb1 * sinb + cosb1 * cosb * coslam;
                b    = Math.sqrt(2.0 / c);
                y    = ymf * b * (cosb1 * sinb - sinb1 * cosb * coslam);
                x    = xmf * b * cosb * sinlam;
                break;
            }
            case EQUATORIAL: {
                sinb = q / qp;
                cosb = Math.sqrt(1.0 - sinb * sinb);
                c    = 1.0 + cosb * coslam;
                b    = Math.sqrt(2.0 / c);
                y    = ymf * b * sinb;
                x    = xmf * b * cosb * sinlam;
                break;
            }
            case NORTH_POLE: {
                c = (Math.PI / 2) + phi;
                q = qp - q;
                if (q >= 0.0) {
                    b = Math.sqrt(q);
                    x = b * sinlam;
                    y = coslam * -b;
                } else {
                    x = y = 0.;
                }
                break;
            }
            case SOUTH_POLE: {
                c = phi - (Math.PI / 2);
                q = qp + q;
                if (q >= 0.0) {
                    b = Math.sqrt(q);
                    x = b * sinlam;
                    y = coslam * +b;
                } else {
                    x = y = 0.;
                }
                break;
            }
            default: {
                throw new AssertionError(mode);
            }
        }
        if (Math.abs(c) < EPSILON_LATITUDE) {
            return new double[] {0, 0}; // this is an error, we should handle it somehow
        }
        return new double[] {x, y};
    }

    @Override
    public double[] invproject(double x, double y) {
        final double lambda, phi;
        switch (mode) {
            case EQUATORIAL: // Fall through
            case OBLIQUE: {
                x /= dd;
                y *= dd;
                final double rho = Math.hypot(x, y);
                if (rho < FINE_EPSILON) {
                    lambda = 0.0;
                    phi = latitudeOfOrigin;
                } else {
                    double sCe, cCe, q, ab;
                    sCe = 2.0 * Math.asin(0.5 * rho / rq);
                    cCe = Math.cos(sCe);
                    sCe = Math.sin(sCe);
                    x *= sCe;
                    if (mode == Mode.OBLIQUE) {
                        ab = cCe * sinb1 + y * sCe * cosb1 / rho;
                        q  = qp * ab;
                        y  = rho * cosb1 * cCe - y * sinb1 * sCe;
                    } else {
                        ab = y * sCe / rho;
                        q  = qp * ab;
                        y  = rho * cCe;
                    }
                    lambda = Math.atan2(x, y);
                    phi = authlat(Math.asin(ab));
                }
                break;
            }
            case NORTH_POLE: {
                y = -y;
                // Fall through
            }
            case SOUTH_POLE: {
                final double q = x*x + y*y;
                if (q == 0) {
                    lambda = 0.;
                    phi = latitudeOfOrigin;
                } else {
                    double ab = 1.0 - q / qp;
                    if (mode == Mode.SOUTH_POLE) {
                        ab = -ab;
                    }
                    lambda = Math.atan2(x, y);
                    phi = authlat(Math.asin(ab));
                }
                break;
            }
            default: {
                throw new AssertionError(mode);
            }
        }
        return new double[] {phi, lambda};
    }


    /**
     * Calculates <var>q</var>, Snyder equation (3-12)
     *
     * @param sinphi sin of the latitude <var>q</var> is calculated for.
     * @return <var>q</var> from Snyder equation (3-12).
     */
    private double qsfn(final double sinphi) {
        if (e >= EPSILON) {
            final double con = e * sinphi;
            return ((1.0 - e2) * (sinphi / (1.0 - con*con) -
                    (0.5 / e) * Math.log((1.0 - con) / (1.0 + con))));
        } else {
            return sinphi + sinphi;
        }
    }

    /**
     * Determines latitude from authalic latitude.
     * @param beta authalic latitude
     * @return corresponding latitude
     */
    private double authlat(final double beta) {
        final double t = beta + beta;
        return beta + APA0 * Math.sin(t) + APA1 * Math.sin(t+t) + APA2 * Math.sin(t+t+t);
    }

    @Override
    public Bounds getAlgorithmBounds() {
        return new Bounds(-89, -174, 89, 174, false);
    }
}
