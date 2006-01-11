package org.openstreetmap.josm.data;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.projection.Projection;

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

	/**
	 * Construct bounds that span the whole world.
	 */
	public Bounds() {
		min = new GeoPoint(-Projection.MAX_LAT, -Projection.MAX_LON);
		Main.pref.getProjection().latlon2xy(min);
		max = new GeoPoint(Projection.MAX_LAT, Projection.MAX_LON);
		Main.pref.getProjection().latlon2xy(max);
	}

	/**
	 * @return The bounding rectangle that covers <code>this</code> and 
	 * 		the <code>other</code> bounds, regarding the x/y values.
	 */
	public Bounds mergeXY(Bounds other) {
		GeoPoint nmin = new GeoPoint();
		nmin.x = Math.min(min.x, other.min.x);
		nmin.y = Math.min(min.y, other.min.y);
		GeoPoint nmax = new GeoPoint();
		nmax.x = Math.max(max.x, other.max.x);
		nmax.y = Math.max(max.y, other.max.y);
		return new Bounds(nmin, nmax);
	}

	/**
	 * @return The bounding rectangle that covers <code>this</code> and 
	 * 		the <code>other</code> bounds, regarding the lat/lon values.
	 */
	public Bounds mergeLatLon(Bounds other) {
		GeoPoint nmin = new GeoPoint(
				Math.min(min.lat, other.min.lat),
				Math.min(min.lon, other.min.lon));
		GeoPoint nmax = new GeoPoint(
				Math.max(max.lat, other.max.lat),
				Math.max(max.lon, other.max.lon));
		return new Bounds(nmin, nmax);
	}
}
