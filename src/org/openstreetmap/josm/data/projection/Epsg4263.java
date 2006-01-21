package org.openstreetmap.josm.data.projection;

import javax.swing.JComponent;

import org.openstreetmap.josm.data.GeoPoint;

/**
 * Directly use latitude / longitude values as x/y.
 *
 * @author imi
 */
public class Epsg4263 extends Projection {

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
		return "EPSG:4263";
	}

	@Override
	public JComponent getConfigurationPanel() {
		return null;
	}

	@Override
	public void commitConfigurationPanel() {
	}
}
