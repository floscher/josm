package org.openstreetmap.josm.gui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.KeyEvent;
import java.util.LinkedList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.ImageIcon;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.GeoPoint;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.LineSegment;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.Track;
import org.openstreetmap.josm.data.projection.Projection;

/**
 * This is a component used in the MapFrame for browsing the map. It use is to
 * provide the MapMode's enough capabilities to operate. 
 * 
 * MapView holds the map data, organize it, convert it, provide access to it.
 * 
 * MapView hold meta-data about the data set currently displayed, as scale level,
 * center point viewed, what scrolling mode or editing mode is selected or with
 * what projection the map is viewed etc..
 *
 * @author imi
 */
public class MapView extends Component implements ComponentListener, ChangeListener {

	/**
	 * Toggles the autoScale feature of the mapView
	 * @author imi
	 */
	public class AutoScaleAction extends AbstractAction {
		public AutoScaleAction() {
			super("Auto Scale", new ImageIcon("images/autoscale.png"));
			putValue(MNEMONIC_KEY, KeyEvent.VK_A);
		}
		public void actionPerformed(ActionEvent e) {
			autoScale = !autoScale;
			recalculateCenterScale();
		}
	}

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
	 * Used projection method in this map
	 */
	private Projection projection = Main.pref.projection.clone();

	/**
	 * The underlying DataSet.
	 */
	public final DataSet dataSet;

	
	// Event handling functions and data
	
	/**
	 * The event list with all state chaned listener
	 */
	List<ChangeListener> listener = new LinkedList<ChangeListener>();
	/**
	 * Add an event listener to the state changed event queue. If passed 
	 * <code>null</code>, nothing happens.
	 */
	public final void addChangeListener(ChangeListener l) {
		if (l != null)
			listener.add(l);
	}
	/**
	 * Remove an event listener from the event queue. If passed 
	 * <code>null</code>, nothing happens.
	 */
	public final void removeChangeListener(ChangeListener l) {
		listener.remove(l);
	}
	/**
	 * Fires an ChangeEvent. Occours, when an non-data value changed, as example
	 * the autoScale - state or the center. Is not fired, dataSet internal things
	 * changes.
	 */
	public final void fireStateChanged() {
		ChangeEvent e = null;
		for(ChangeListener l : listener) {
			if (e == null)
				e = new ChangeEvent(this);
			l.stateChanged(e);
		}
	}
	
	
	
	
	
	// other functions
	
	/**
	 * Construct a MapView and attach it to a frame.
	 */
	public MapView(DataSet dataSet) {
		this.dataSet = dataSet;
		addComponentListener(this);
		
		// initialize the movement listener
		new MapMover(this);
		
		// initialize the projection
		setProjection(Main.pref.projection.clone());
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
			getProjection().xy2latlon(p);
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
			projection.latlon2xy(p);
		}
		return new Point(toScreenX(p.x), toScreenY(p.y));
	}
	
	/**
	 * Zoom to the given coordinate.
	 * @param centerX The center x-value (easting) to zoom to.
	 * @param centerY The center y-value (northing) to zoom to.
	 * @param scale The scale to use.
	 */
	public void zoomTo(GeoPoint newCenter, double scale) {
		autoScale = false;
		center = newCenter.clone();
		projection.xy2latlon(center);
		this.scale = scale;
		recalculateCenterScale();
		fireStateChanged(); // in case of autoScale, recalculate fired.
	}
	
	/**
	 * Return the current scale value.
	 * @return The scale value currently used in display
	 */
	public double getScale() {
		return scale;
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
			if (bounds == null) {
				// no bounds means standard scale and center 
				center = new GeoPoint(51.526447, -0.14746371);
				getProjection().latlon2xy(center);
				scale = 10000; // nice view from 1:10000, eh?
			} else {
				center = bounds.centerXY();
				getProjection().xy2latlon(center);
				double scaleX = (bounds.max.x-bounds.min.x)/w;
				double scaleY = (bounds.max.y-bounds.min.y)/h;
				scale = Math.max(scaleX, scaleY); // minimum scale to see all of the screen
			}
			fireStateChanged();
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
	 * Draw the component.
	 */
	@Override
	public void paint(Graphics g) {
		// empty out everything
		g.setColor(Color.BLACK);
		g.fillRect(0,0,getWidth(),getHeight());
		
		// draw tracks
		if (dataSet.tracks != null)
			for (Track track : dataSet.tracks)
				for (LineSegment ls : track.segments) {
					g.setColor(ls.selected ? Color.WHITE : Color.GRAY);
					g.drawLine(toScreenX(ls.start.coor.x), toScreenY(ls.start.coor.y),
							toScreenX(ls.end.coor.x), toScreenY(ls.end.coor.y));
				}

		// draw nodes
		for (Node w : dataSet.allNodes) {
			g.setColor(w.selected ? Color.WHITE : Color.RED);
			g.drawArc(toScreenX(w.coor.x), toScreenY(w.coor.y), 3, 3, 0, 360);
		}
	}

	/**
	 * Return the x-screen coordinate for the given point.
	 * @param x The X-position (easting) of the desired point
	 * @return The screen coordinate for the point.
	 */
	public int toScreenX(double x) {
		return (int)Math.round((x-center.x) / scale + getWidth()/2);
	}

	/**
	 * Return the y-screen coordinate for the given point.
	 * @param y The Y-position (northing) of the desired point
	 * @return The screen coordinate for the point.
	 */
	public int toScreenY(double y) {
		return (int)Math.round((center.y-y) / scale + getHeight()/2);
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

	/**
	 * Notify from the projection, that something has changed.
	 * @param e
	 */
	public void stateChanged(ChangeEvent e) {
		projection.init(dataSet);
		recalculateCenterScale();
	}

	/**
	 * Set a new projection method. This call is not cheap, as it will
	 * transform the whole underlying dataSet and repaint itself.
	 * 
	 * @param projection The new projection method to set.
	 */
	public void setProjection(Projection projection) {
		if (projection == this.projection)
			return;
		if (this.projection != null)
			this.projection.removeChangeListener(this);
		this.projection = projection;
		projection.addChangeListener(this);
		stateChanged(new ChangeEvent(this));
	}

	/**
	 * Return the projection method for this map view.
	 * @return The projection method.
	 */
	public Projection getProjection() {
		return projection;
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
			fireStateChanged();
		}
	}
	/**
	 * @return Returns the center point. A copy is returned, so users cannot
	 * 		change the center by accessing the return value. Use zoomTo instead.
	 */
	public GeoPoint getCenter() {
		return center.clone();
	}
}
