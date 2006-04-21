package org.openstreetmap.josm.data;

import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.projection.Projection;

/**
 * This is a simple data class for "rectangular" areas of the world, given in 
 * lat/lon min/max values.
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
	public LatLon min, max;

	/**
	 * Construct bounds out of two points
	 */
	public Bounds(LatLon min, LatLon max) {
		this.min = min;
		this.max = max;
	}

	/**
	 * Construct bounds that span the whole world.
	 */
	public Bounds() {
		min = new LatLon(-Projection.MAX_LAT, -Projection.MAX_LON);
		max = new LatLon(Projection.MAX_LAT, Projection.MAX_LON);
	}
	
	@Override public String toString() {
		return "Bounds["+min.lat()+","+min.lon()+","+max.lat()+","+max.lon()+"]";
	}

	/**
	 * Extend the bounds if necessary to include the given point.
	 */
	public void extend(LatLon ll) {
		if (ll.lat() < min.lat() || ll.lon() < min.lon())
			min = new LatLon(Math.min(ll.lat(), min.lat()), Math.min(ll.lon(), min.lon()));
		if (ll.lat() > max.lat() || ll.lon() > max.lon())
			max = new LatLon(Math.max(ll.lat(), max.lat()), Math.max(ll.lon(), max.lon()));
	}
}
