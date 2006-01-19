package org.openstreetmap.josm.gui;

import java.awt.Point;

import javax.swing.JComponent;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.GeoPoint;

/**
 * An component that can be navigated by a mapmover. Used as map view and for the
 * zoomer in the download dialog.
 * 
 * @author imi
 */
public class NavigatableComponent extends JComponent {

	/**
	 * The scale factor in meter per pixel.
	 */
	protected double scale;
	/**
	 * Center n/e coordinate of the desired screen center.
	 */
	protected GeoPoint center;

	/**
	 * Return the current scale value.
	 * @return The scale value currently used in display
	 */
	public double getScale() {
		return scale;
	}

	/**
	 * @return Returns the center point. A copy is returned, so users cannot
	 * 		change the center by accessing the return value. Use zoomTo instead.
	 */
	public GeoPoint getCenter() {
		return center.clone();
	}

	/**
	 * Get geographic coordinates from a specific pixel coordination
	 * on the screen.
	 * 
	 * If you don't need it, provide false at third parameter to speed
	 * up the calculation.
	 *  
	 * @param x X-Pixelposition to get coordinate from
	 * @param y Y-Pixelposition to get coordinate from
	 * @param latlon If set, the return value will also have the 
	 * 				 latitude/longitude filled.
	 * 
	 * @return The geographic coordinate, filled with x/y (northing/easting)
	 * 		settings and, if requested with latitude/longitude.
	 */
	public GeoPoint getPoint(int x, int y, boolean latlon) {
		GeoPoint p = new GeoPoint();
		p.x = (x - getWidth()/2.0)*scale + center.x;
		p.y = (getHeight()/2.0 - y)*scale + center.y;
		if (latlon)
			Main.pref.getProjection().xy2latlon(p);
		return p;
	}

	/**
	 * Return the point on the screen where this GeoPoint would be.
	 * @param point The point, where this geopoint would be drawn.
	 * @return The point on screen where "point" would be drawn, relative
	 * 		to the own top/left.
	 */
	public Point getScreenPoint(GeoPoint point) {
		GeoPoint p;
		if (!Double.isNaN(point.x) && !Double.isNaN(point.y))
			p = point;
		else {
			if (Double.isNaN(point.lat) || Double.isNaN(point.lon))
				throw new IllegalArgumentException("point: Either lat/lon or x/y must be set.");
			p = point.clone();
			Main.pref.getProjection().latlon2xy(p);
		}
		int x = ((int)Math.round((p.x-center.x) / scale + getWidth()/2));
		int y = ((int)Math.round((center.y-p.y) / scale + getHeight()/2));
		return new Point(x,y);
	}

	/**
	 * Zoom to the given coordinate.
	 * @param centerX The center x-value (easting) to zoom to.
	 * @param centerY The center y-value (northing) to zoom to.
	 * @param scale The scale to use.
	 */
	public void zoomTo(GeoPoint newCenter, double scale) {
		System.out.println(scale);
		center = newCenter.clone();
		Main.pref.getProjection().xy2latlon(center);
		this.scale = scale;
		repaint();
	}
}
