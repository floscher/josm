// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.data.coor;

import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.projection.Projection;

/**
 * LatLon are unprojected latitude / longitude coordinates.
 * 
 * This class is immutable.
 * 
 * @author Imi
 */
public class LatLon extends Coordinate {

	public LatLon(double lat, double lon) {
		super(lon, lat);
	}

	public double lat() {
		return y;
	}

	public double lon() {
		return x;
	}

	/**
	 * @return <code>true</code>, if the other point has almost the same lat/lon
	 * values, only differ by no more than 1/Projection.MAX_SERVER_PRECISION.
	 */
	public boolean equalsEpsilon(LatLon other) {
		final double p = 1/Projection.MAX_SERVER_PRECISION;
		return Math.abs(lat()-other.lat()) <= p && Math.abs(lon()-other.lon()) <= p;
	}

	/**
	 * @return <code>true</code>, if the coordinate is outside the world, compared
	 * by using lat/lon.
	 */
	public boolean isOutSideWorld() {
		return lat() < -Projection.MAX_LAT || lat() > Projection.MAX_LAT || 
			lon() < -Projection.MAX_LON || lon() > Projection.MAX_LON;
	}

	/**
	 * @return <code>true</code> if this is within the given bounding box.
	 */
	public boolean isWithin(Bounds b) {
		return lat() >= b.min.lat() && lat() <= b.max.lat() && lon() > b.min.lon() && lon() < b.max.lon();
	}
	
    @Override public String toString() {
        return "LatLon[lat="+lat()+",lon="+lon()+"]";
    }
}
