package org.openstreetmap.josm.data.projection;

import javax.swing.JPanel;

import org.openstreetmap.josm.data.GeoPoint;

/**
 * Directly use latitude / longitude values as x/y.
 *
 * @author imi
 */
public class LatitudeLongitude extends Projection {

	@Override
	public void latlon2xy(GeoPoint p) {
		p.x = p.lon;
		p.y = p.lat;
	}

	@Override
	public void xy2latlon(GeoPoint p) {
		p.lat = p.y;
		p.lon = p.x;
	}

	@Override
	public String toString() {
		return "Latitude/Longitude";
	}

	@Override
	public String description() {
		return "Use lat/lon values directly.";
	}

	@Override
	public JPanel getConfigurationPanel() {
		return null;
	}
}
