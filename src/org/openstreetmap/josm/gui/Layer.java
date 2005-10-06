package org.openstreetmap.josm.gui;

import java.awt.Graphics;
import java.awt.Point;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JComponent;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.GeoPoint;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.LineSegment;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Track;
import org.openstreetmap.josm.data.projection.Projection;
import org.openstreetmap.josm.gui.engine.Engine;
import org.openstreetmap.josm.gui.engine.SimpleEngine;

/**
 * This is a component used in the MapFrame for browsing the map. It use is to
 * provide the MapMode's enough capabilities to operate.
 * 
 * Layer holds one dataset. There can be more than one Layer active.
 * 
 * Layer hold data of the current displayed graphics as scale level and
 * center point view.
 * 
 * @author imi
 */
public class Layer extends JComponent implements ComponentListener, ChangeListener, PropertyChangeListener {

	/**
	 * Whether to adjust the scale property on every resize.
	 */
	private boolean autoScale = true;

	/**
	 * The scale factor in meter per pixel.
	 */
	private double scale;
	/**
	 * Center n/e coordinate of the desired screen center.
	 */
	private GeoPoint center;

	/**
	 * The underlying DataSet.
	 */
	public final DataSet dataSet;

	/**
	 * The drawing engine.
	 */
	private Engine engine;
	
	/**
	 * Construct a Layer.
	 */
	public Layer(DataSet dataSet) {
		this.dataSet = dataSet;
		addComponentListener(this);

		// initialize the movement listener
		new MapMover(this);

		// initialize the drawing engine
		engine = new SimpleEngine(this);
		
		// initialize on the preferences for projection changes.
		Main.pref.addPropertyChangeListener(this);
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
	 * Return the object, that is nearest to the given screen point.
	 * 
	 * First, a node will be searched. If a node within 10 pixel is found, the
	 * nearest node is returned.
	 * 
	 * If no node is found, search for pending line segments.
	 * 
	 * If no such line segment is found, and a non-pending line segment is 
	 * within 10 pixel to p, this segment is returned, except when 
	 * <code>wholeTrack</code> is <code>true</code>, in which case the 
	 * corresponding Track is returned.
	 * 
	 * If no line segment is found and the point is within an area, return that
	 * area.
	 * 
	 * If no area is found, return <code>null</code>.
	 * 
	 * @param p				The point on screen.
	 * @param wholeTrack	Whether the whole track or only the line segment
	 * 					 	should be returned.
	 * @return	The primitive, that is nearest to the point p.
	 */
	public OsmPrimitive getNearest(Point p, boolean wholeTrack) {
		double minDistanceSq = Double.MAX_VALUE;
		OsmPrimitive minPrimitive = null;
		
		// nodes
		for (Node n : dataSet.nodes) {
			Point sp = getScreenPoint(n.coor);
			double dist = p.distanceSq(sp);
			if (minDistanceSq > dist && dist < 100) {
				minDistanceSq = p.distanceSq(sp);
				minPrimitive = n;
			}
		}
		if (minPrimitive != null)
			return minPrimitive;
		
		// pending line segments
		for (LineSegment ls : dataSet.pendingLineSegments()) {
			Point A = getScreenPoint(ls.getStart().coor);
			Point B = getScreenPoint(ls.getEnd().coor);
			double c = A.distanceSq(B);
			double a = p.distanceSq(B);
			double b = p.distanceSq(A);
			double perDist = a-(a-b+c)*(a-b+c)/4/c; // perpendicular distance squared
			if (perDist < 100 && minDistanceSq > perDist && a < c+100 && b < c+100) {
				minDistanceSq = perDist;
				minPrimitive = ls;
			}
		}

		// tracks & line segments
		minDistanceSq = Double.MAX_VALUE;
		for (Track t : dataSet.tracks()) {
			for (LineSegment ls : t.segments()) {
				Point A = getScreenPoint(ls.getStart().coor);
				Point B = getScreenPoint(ls.getEnd().coor);
				double c = A.distanceSq(B);
				double a = p.distanceSq(B);
				double b = p.distanceSq(A);
				double perDist = a-(a-b+c)*(a-b+c)/4/c; // perpendicular distance squared
				if (perDist < 100 && minDistanceSq > perDist && a < c+100 && b < c+100) {
					minDistanceSq = perDist;
					minPrimitive = wholeTrack ? t : ls;
				}
			}			
		}
		if (minPrimitive != null)
			return minPrimitive;
		
		// TODO areas
		
		return null; // nothing found
	}

	
	/**
	 * Zoom to the given coordinate.
	 * @param centerX The center x-value (easting) to zoom to.
	 * @param centerY The center y-value (northing) to zoom to.
	 * @param scale The scale to use.
	 */
	public void zoomTo(GeoPoint newCenter, double scale) {
		boolean oldAutoScale = autoScale;
		GeoPoint oldCenter = center;
		double oldScale = this.scale;
		
		autoScale = false;
		center = newCenter.clone();
		Main.pref.getProjection().xy2latlon(center);
		this.scale = scale;
		recalculateCenterScale();

		firePropertyChange("center", oldCenter, center);
		if (oldAutoScale != autoScale)
			firePropertyChange("autoScale", oldAutoScale, autoScale);
		if (oldScale != scale)
			firePropertyChange("scale", oldScale, scale);
	}
	
	/**
	 * Draw the component.
	 */
	@Override
	public void paint(Graphics g) {
		engine.init(g);
		engine.drawBackground(getPoint(0,0,true), getPoint(getWidth(), getHeight(), true));

		for (Track t : dataSet.tracks())
			engine.drawTrack(t);
		for (LineSegment ls : dataSet.pendingLineSegments())
			engine.drawPendingLineSegment(ls);
		for (Node n : dataSet.nodes)
			engine.drawNode(n);
	}

	/**
	 * Notify from the projection, that something has changed.
	 * @param e
	 */
	public void stateChanged(ChangeEvent e) {
		initDataSet();
	}

	/**
	 * Called when a property, as example the projection of the Main preferences
	 * changes.
	 */
	public void propertyChange(PropertyChangeEvent evt) {
		if (evt.getPropertyName().equals("projection")) {
			Projection oldProjection = (Projection)evt.getOldValue();
			if (oldProjection != null)
				oldProjection.removeChangeListener(this);

			Projection newProjection = (Projection)evt.getNewValue();
			if (newProjection != null)
				newProjection.addChangeListener(this);

			initDataSet();
		}
	}

	/**
	 * Return the current scale value.
	 * @return The scale value currently used in display
	 */
	public double getScale() {
		return scale;
	}

	/**
	 * @return Returns the autoScale.
	 */
	public boolean isAutoScale() {
		return autoScale;
	}

	/**
	 * @param autoScale The autoScale to set.
	 */
	public void setAutoScale(boolean autoScale) {
		if (this.autoScale != autoScale) {
			this.autoScale = autoScale;
			if (autoScale)
				recalculateCenterScale();
			firePropertyChange("autoScale", !autoScale, autoScale);
		}
	}
	/**
	 * @return Returns the center point. A copy is returned, so users cannot
	 * 		change the center by accessing the return value. Use zoomTo instead.
	 */
	public GeoPoint getCenter() {
		return center.clone();
	}

	/**
	 * Initialize the DataSet with the projection taken from the preference
	 * settings.
	 */
	public void initDataSet() {
		for (Node n : dataSet.nodes)
			Main.pref.getProjection().latlon2xy(n.coor);
		recalculateCenterScale();
	}
	
	
	/**
	 * Set the new dimension to the projection class. Also adjust the components 
	 * scale, if in autoScale mode.
	 */
	private void recalculateCenterScale() {
		if (autoScale) {
			// -20 to leave some border
			int w = getWidth()-20;
			if (w < 20)
				w = 20;
			int h = getHeight()-20;
			if (h < 20)
				h = 20;
			Bounds bounds = dataSet.getBoundsXY();
			
			boolean oldAutoScale = autoScale;
			GeoPoint oldCenter = center;
			double oldScale = this.scale;
			
			if (bounds == null) {
				// no bounds means standard scale and center 
				center = new GeoPoint(51.526447, -0.14746371);
				Main.pref.getProjection().latlon2xy(center);
				scale = 10;
			} else {
				center = bounds.centerXY();
				Main.pref.getProjection().xy2latlon(center);
				double scaleX = (bounds.max.x-bounds.min.x)/w;
				double scaleY = (bounds.max.y-bounds.min.y)/h;
				scale = Math.max(scaleX, scaleY); // minimum scale to see all of the screen
			}
	
			firePropertyChange("center", oldCenter, center);
			if (oldAutoScale != autoScale)
				firePropertyChange("autoScale", oldAutoScale, autoScale);
			if (oldScale != scale)
				firePropertyChange("scale", oldScale, scale);
		}
		repaint();
	}

	/**
	 * Call to recalculateCenterScale.
	 */
	public void componentResized(ComponentEvent e) {
		recalculateCenterScale();
	}

	/**
	 * Does nothing. Just to satisfy ComponentListener.
	 */
	public void componentMoved(ComponentEvent e) {}
	/**
	 * Does nothing. Just to satisfy ComponentListener.
	 */
	public void componentShown(ComponentEvent e) {}
	/**
	 * Does nothing. Just to satisfy ComponentListener.
	 */
	public void componentHidden(ComponentEvent e) {}
}
