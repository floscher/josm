package org.openstreetmap.josm.data.projection;

import javax.swing.JComponent;

import org.openstreetmap.josm.data.GeoPoint;

/**
 * Implement Mercator Projection code, coded after documentation
 * from wikipedia.
 * 
 * The center of the mercator projection is always the 0° 
 * coordinate.
 * 
 * @author imi
 */
public class Mercator extends Projection {

	@Override
	public void latlon2xy(GeoPoint p) {
		p.x = p.lon*Math.PI/180;
		p.y = Math.log(Math.tan(Math.PI/4+p.lat*Math.PI/360));
	}

	@Override
	public void xy2latlon(GeoPoint p) {
		p.lon = p.x*180/Math.PI;
		p.lat = Math.atan(Math.sinh(p.y))*180/Math.PI;
	}

	@Override
	public String toString() {
		return "Mercator";
	}

	@Override
	public JComponent getConfigurationPanel() {
		return null;
	}

	@Override
	public void commitConfigurationPanel() {
	}
}
