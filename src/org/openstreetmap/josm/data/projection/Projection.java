// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.data.projection;

import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.Bounds;

/**
 * Classes implementing this are able to convert lat/lon values to
 * planar screen coordinates.
 *
 * @author imi
 */
public interface Projection {
    /**
     * Minimum difference in location to not be represented as the same position.
     */
    public static final double MAX_SERVER_PRECISION = 1e12;

    /**
     * List of all available projections.
     */
    public static Projection[] allProjections = new Projection[]{
        new Epsg4326(),
        new Mercator(),
        new LambertEST(),
        new Lambert(),
        new SwissGrid(),
        new UTM(),
        new UTM_20N_Guadeloupe_Ste_Anne(),
        new UTM_20N_Guadeloupe_Fort_Marigot(),
        new UTM_20N_Martinique_Fort_Desaix(),
        new GaussLaborde_Reunion()
    };

    /**
     * Convert from lat/lon to northing/easting.
     *
     * @param p     The geo point to convert. x/y members of the point are filled.
     */
    EastNorth latlon2eastNorth(LatLon p);

    /**
     * Convert from norting/easting to lat/lon.
     *
     * @param p     The geo point to convert. lat/lon members of the point are filled.
     */
    LatLon eastNorth2latlon(EastNorth p);

    /**
     * Describe the projection converter in one or two words.
     */
    String toString();

    /**
     * Return projection code.
     */
    String toCode();

    /**
     * Get a filename compatible string (for the cache directory)
     */
    String getCacheDirectoryName();

    /**
     * Get the bounds of the world
     */
    Bounds getWorldBoundsLatLon();
}
