// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.coor;

import org.openstreetmap.josm.tools.Utils;

public final class QuadTiling {

    private QuadTiling() {
        // Hide default constructor for utils classes
    }

    public static final int NR_LEVELS = 24;
    public static final double WORLD_PARTS = 1 << NR_LEVELS;

    public static final int TILES_PER_LEVEL_SHIFT = 2; // Has to be 2. Other parts of QuadBuckets code rely on it
    public static final int TILES_PER_LEVEL = 1 << TILES_PER_LEVEL_SHIFT;
    public static final int X_PARTS = 360;
    public static final int X_BIAS = -180;

    public static final int Y_PARTS = 180;
    public static final int Y_BIAS = -90;

    public static LatLon tile2LatLon(long quad) {
        // The world is divided up into X_PARTS,Y_PARTS.
        // The question is how far we move for each bit being set.
        // In the case of the top level, we move half of the world.
        double xUnit = X_PARTS/2d;
        double yUnit = Y_PARTS/2d;
        long shift = (NR_LEVELS*2L)-2L;

        double x = 0;
        double y = 0;
        for (int i = 0; i < NR_LEVELS; i++) {
            long bits = (quad >> shift) & 0x3;
            // remember x is the MSB
            if ((bits & 0x2) != 0) {
                x += xUnit;
            }
            if ((bits & 0x1) != 0) {
                y += yUnit;
            }
            xUnit /= 2;
            yUnit /= 2;
            shift -= 2;
        }
        x += X_BIAS;
        y += Y_BIAS;
        return new LatLon(y, x);
    }

    static long lon2x(double lon) {
        long ret = (long) ((lon + 180.0) * WORLD_PARTS / 360.0);
        if (Utils.equalsEpsilon(ret, WORLD_PARTS)) {
            ret--;
        }
        return ret;
    }

    static long lat2y(double lat) {
        long ret = (long) ((lat + 90.0) * WORLD_PARTS / 180.0);
        if (Utils.equalsEpsilon(ret, WORLD_PARTS)) {
            ret--;
        }
        return ret;
    }

    /**
     * Returns quad tiling index for given coordinates and level.
     *
     * @param lat latitude
     * @param lon longitude
     * @param level level
     *
     * @return quad tiling index for given coordinates and level.
     * @since 6171
     */
    public static byte index(final double lat, final double lon, final int level) {
        long x = lon2x(lon);
        long y = lat2y(lat);
        int shift = NR_LEVELS-level-1;
        return (byte) ((x >> shift & 1) * 2 + (y >> shift & 1));
    }
}
