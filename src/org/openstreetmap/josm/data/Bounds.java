package org.openstreetmap.josm.data;

/**
 * This is a simple data class for "rectangular" areas of the world, given in lat/lon/min/max
 * values.
 * 
 * Do not confuse this with "Area", which is an OSM-primitive for a vector of nodes, 
 * describing some area (like a sea).
 * 
 * @author imi
 */
public class Bounds {
	/**
	 * The minimum and maximum coordinates.
	 */
	public GeoPoint min, max;

	/**
	 * Return the center point based on the lat/lon values.
	 *
	 * @return The center of these bounds.
	 */
	public GeoPoint centerLatLon() {
		GeoPoint p = new GeoPoint((min.lat+max.lat) / 2, (min.lon+max.lon) / 2);
		return p;
	}

	/**
	 * Return the center point based on the x/y values.
	 *
	 * @return The center of these bounds.
	 */
	public GeoPoint centerXY() {
		GeoPoint p = new GeoPoint();
		p.x = (min.x+max.x) / 2;
		p.y = (min.y+max.y) / 2;
		return p;
	}
	
	/**
	 * Construct bounds out of two geopoints
	 */
	public Bounds(GeoPoint min, GeoPoint max) {
		this.min = min;
		this.max = max;
	}
}
