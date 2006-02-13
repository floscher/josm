package org.openstreetmap.josm.data.projection;

import org.openstreetmap.josm.data.GeoPoint;

/**
 * Implement Mercator Projection code, coded after documentation
 * from wikipedia.
 * 
 * The center of the mercator projection is always the 0 grad 
 * coordinate.
 * 
 * @author imi
 */
public class Mercator implements Projection {

	public void latlon2xy(GeoPoint p) {
		p.x = p.lon*Math.PI/180;
		p.y = Math.log(Math.tan(Math.PI/4+p.lat*Math.PI/360));
	}

	public void xy2latlon(GeoPoint p) {
		p.lon = p.x*180/Math.PI;
		p.lat = Math.atan(Math.sinh(p.y))*180/Math.PI;
		// round values to maximum server precision
		p.lon = Math.round(p.lon*MAX_SERVER_PRECISION)/MAX_SERVER_PRECISION;
		p.lat = Math.round(p.lat*MAX_SERVER_PRECISION)/MAX_SERVER_PRECISION;
	}

	@Override
	public String toString() {
		return "Mercator";
	}
}
