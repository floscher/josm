package org.openstreetmap.josm.data.projection;

import org.openstreetmap.josm.data.GeoPoint;

/**
 * Directly use latitude / longitude values as x/y.
 *
 * @author imi
 */
public class Epsg4263 implements Projection {

	public void latlon2xy(GeoPoint p) {
		p.x = p.lon;
		p.y = p.lat;
	}

	public void xy2latlon(GeoPoint p) {
		p.lat = Math.round(p.y*MAX_SERVER_PRECISION)/MAX_SERVER_PRECISION;
		p.lon = Math.round(p.x*MAX_SERVER_PRECISION)/MAX_SERVER_PRECISION;
	}

	@Override
	public String toString() {
		return "EPSG:4263";
	}
}
