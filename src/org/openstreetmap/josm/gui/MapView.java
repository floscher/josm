package org.openstreetmap.josm.gui;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;

import javax.swing.JComponent;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.GeoPoint;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.LineSegment;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Track;
import org.openstreetmap.josm.data.projection.Projection;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;

/**
 * This is a component used in the MapFrame for browsing the map. It use is to
 * provide the MapMode's enough capabilities to operate. 
 * 
 * MapView hold meta-data about the data set currently displayed, as scale level,
 * center point viewed, what scrolling mode or editing mode is selected or with
 * what projection the map is viewed etc..
 *
 * MapView is able to administrate several layers, but there must be always at
 * least one layer with a dataset in it (Layer.getDataSet returning non-null).
 *
 * @author imi
 */
public class MapView extends JComponent implements ChangeListener, PropertyChangeListener {

	/**
	 * Interface to notify listeners of the change of the active layer.
	 * @author imi
	 */
	public interface LayerChangeListener {
		void activeLayerChange(Layer oldLayer, Layer newLayer);
		void layerAdded(Layer newLayer);
		void layerRemoved(Layer oldLayer);
	}

	/**
	 * Whether to adjust the scale property on every resize.
	 */
	boolean autoScale = true;

	/**
	 * The scale factor in meter per pixel.
	 */
	private double scale;
	/**
	 * Center n/e coordinate of the desired screen center.
	 */
	private GeoPoint center;

	/**
	 * A list of all layers currently loaded.
	 */
	private ArrayList<Layer> layers = new ArrayList<Layer>();
	/**
	 * Direct link to the edit layer (if any) in the layers list.
	 */
	private OsmDataLayer editLayer;
	/**
	 * The layer from the layers list that is currently active.
	 */
	private Layer activeLayer;
	/**
	 * The listener of the active layer changes.
	 */
	private Collection<LayerChangeListener> listeners = new LinkedList<LayerChangeListener>();
	
	/**
	 * Construct a MapView.
	 * @param layer The first layer in the view.
	 */
	public MapView(Layer layer) {
		addComponentListener(new ComponentAdapter(){
			@Override
			public void componentResized(ComponentEvent e) {
				recalculateCenterScale();
			}
		});

		// initialize the movement listener
		new MapMover(this);

		// initialize the projection
		addLayer(layer);
		Main.pref.addPropertyChangeListener(this);
	}

	/**
	 * Add a layer to the current MapView. The layer will be added at topmost
	 * position.
	 */
	public void addLayer(Layer layer) {
		// initialize the projection if it is the first layer
		if (layers.isEmpty())
			Main.pref.getProjection().init(layer.getBoundsLatLon());

		// reinitialize layer's data
		layer.init(Main.pref.getProjection());

		if (layer instanceof OsmDataLayer) {
			if (editLayer != null) {
				// merge the layer into the existing one
				if (!editLayer.isMergable(layer))
					throw new IllegalArgumentException("Cannot merge argument");
				editLayer.mergeFrom(layer);
				repaint();
				return;
			}
			editLayer = (OsmDataLayer)layer;
		}

		// add as a new layer
		layers.add(0,layer);

		for (LayerChangeListener l : listeners)
			l.layerAdded(layer);

		// autoselect the new layer
		setActiveLayer(layer);
		recalculateCenterScale();
	}

	/**
	 * Remove the layer from the mapview. If the layer was in the list before,
	 * an LayerChange event is fired.
	 */
	public void removeLayer(Layer layer) {
		if (layers.remove(layer))
			for (LayerChangeListener l : listeners)
				l.layerRemoved(layer);
		if (layer == editLayer)
			editLayer = null;
	}

	/**
	 * Moves the layer to the given new position. No event is fired.
	 * @param layer		The layer to move
	 * @param pos		The new position of the layer
	 */
	public void moveLayer(Layer layer, int pos) {
		int curLayerPos = layers.indexOf(layer);
		if (curLayerPos == -1)
			throw new IllegalArgumentException("layer not in list.");
		if (pos == curLayerPos)
			return; // already in place.
		layers.remove(curLayerPos);
		if (pos >= layers.size())
			layers.add(layer);
		else
			layers.add(pos, layer);
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
	 * @param p				 The point on screen.
	 * @param lsInsteadTrack Whether the line segment (true) or only the whole
	 * 					 	 track should be returned.
	 * @return	The primitive, that is nearest to the point p.
	 */
	public OsmPrimitive getNearest(Point p, boolean lsInsteadTrack) {
		double minDistanceSq = Double.MAX_VALUE;
		OsmPrimitive minPrimitive = null;

		// nodes
		for (Node n : Main.main.ds.nodes) {
			if (n.isDeleted())
				continue;
			Point sp = getScreenPoint(n.coor);
			double dist = p.distanceSq(sp);
			if (minDistanceSq > dist && dist < 100) {
				minDistanceSq = p.distanceSq(sp);
				minPrimitive = n;
			}
		}
		if (minPrimitive != null)
			return minPrimitive;
		
		// for whole tracks, try the tracks first
		minDistanceSq = Double.MAX_VALUE;
		if (!lsInsteadTrack) {
			for (Track t : Main.main.ds.tracks) {
				if (t.isDeleted())
					continue;
				for (LineSegment ls : t.segments) {
					if (ls.isDeleted())
						continue;
					Point A = getScreenPoint(ls.start.coor);
					Point B = getScreenPoint(ls.end.coor);
					double c = A.distanceSq(B);
					double a = p.distanceSq(B);
					double b = p.distanceSq(A);
					double perDist = a-(a-b+c)*(a-b+c)/4/c; // perpendicular distance squared
					if (perDist < 100 && minDistanceSq > perDist && a < c+100 && b < c+100) {
						minDistanceSq = perDist;
						minPrimitive = t;
					}
				}			
			}
			if (minPrimitive != null)
				return minPrimitive;
		}
		
		minDistanceSq = Double.MAX_VALUE;
		// line segments
		for (LineSegment ls : Main.main.ds.lineSegments) {
			if (ls.isDeleted())
				continue;
			Point A = getScreenPoint(ls.start.coor);
			Point B = getScreenPoint(ls.end.coor);
			double c = A.distanceSq(B);
			double a = p.distanceSq(B);
			double b = p.distanceSq(A);
			double perDist = a-(a-b+c)*(a-b+c)/4/c; // perpendicular distance squared
			if (perDist < 100 && minDistanceSq > perDist && a < c+100 && b < c+100) {
				minDistanceSq = perDist;
				minPrimitive = ls;
			}
		}

		return minPrimitive;
	}

	/**
	 * @return A list of all objects that are nearest to 
	 * the mouse. To do this, first the nearest object is 
	 * determined.
	 * 
	 * If its a node, return all line segments and
	 * streets the node is part of, as well as all nodes
	 * (with their line segments and tracks) with the same
	 * location.
	 * 
	 * If its a line segment, return all tracks this segment 
	 * belongs to as well as all line segments that are between
	 * the same nodes (in both direction) with all their tracks.
	 * 
	 * @return A collection of all items or <code>null</code>
	 * 		if no item under or near the point. The returned
	 * 		list is never empty.
	 */
	public Collection<OsmPrimitive> getAllNearest(Point p) {
		OsmPrimitive osm = getNearest(p, true);
		if (osm == null)
			return null;
		Collection<OsmPrimitive> c = new HashSet<OsmPrimitive>();
		c.add(osm);
		if (osm instanceof Node) {
			Node node = (Node)osm;
			for (Node n : Main.main.ds.nodes)
				if (!n.isDeleted() && n.coor.equalsLatLon(node.coor))
					c.add(n);
			for (LineSegment ls : Main.main.ds.lineSegments)
				// line segments never match nodes, so they are skipped by contains
				if (!ls.isDeleted() && (c.contains(ls.start) || c.contains(ls.end)))
					c.add(ls);
		} 
		if (osm instanceof LineSegment) {
			LineSegment line = (LineSegment)osm;
			for (LineSegment ls : Main.main.ds.lineSegments)
				if (!ls.isDeleted() && ls.equalPlace(line))
					c.add(ls);
		}
		if (osm instanceof Node || osm instanceof LineSegment) {
			for (Track t : Main.main.ds.tracks) {
				if (t.isDeleted())
					continue;
				for (LineSegment ls : t.segments) {
					if (!ls.isDeleted() && c.contains(ls)) {
						c.add(t);
						break;
					}
				}
			}
		}
		return c;
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
		g.setColor(Color.BLACK);
		g.fillRect(0, 0, getWidth(), getHeight());

		for (int i = layers.size()-1; i >= 0; --i) {
			Layer l = layers.get(i);
			if (l.visible)
				l.paint(g, this);
		}
	}

	/**
	 * Notify from the projection, that something has changed.
	 * @param e
	 */
	public void stateChanged(ChangeEvent e) {
		// reset all datasets.
		Projection p = Main.pref.getProjection();
		for (Node n : Main.main.ds.nodes)
			p.latlon2xy(n.coor);
		recalculateCenterScale();
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
			firePropertyChange("autoScale", !autoScale, autoScale);
			recalculateCenterScale();
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
	 * Change to the new projection. Recalculate the dataset and zoom, if autoZoom
	 * is active.
	 * @param oldProjection The old projection. Unregister from this.
	 * @param newProjection	The new projection. Register as state change listener.
	 */
	public void propertyChange(PropertyChangeEvent evt) {
		if (!evt.getPropertyName().equals("projection"))
			return;
		if (evt.getOldValue() != null)
			((Projection)evt.getOldValue()).removeChangeListener(this);
		if (evt.getNewValue() != null) {
			Projection p = (Projection)evt.getNewValue();
			p.addChangeListener(this);

			stateChanged(new ChangeEvent(this));
		}
	}
	
	/**
	 * Set the new dimension to the projection class. Also adjust the components 
	 * scale, if in autoScale mode.
	 */
	void recalculateCenterScale() {
		if (autoScale) {
			// -20 to leave some border
			int w = getWidth()-20;
			if (w < 20)
				w = 20;
			int h = getHeight()-20;
			if (h < 20)
				h = 20;
			
			Bounds bounds = null;
			for (Layer l : layers) {
				if (bounds == null)
					bounds = l.getBoundsXY();
				else {
					Bounds lb = l.getBoundsXY();
					if (lb != null)
						bounds = bounds.mergeXY(lb);
				}
			}

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
	 * Add a listener for changes of active layer.
	 * @param listener The listener that get added.
	 */
	public void addLayerChangeListener(LayerChangeListener listener) {
		if (listener != null)
			listeners.add(listener);
	}

	/**
	 * Remove the listener.
	 * @param listener The listener that get removed from the list.
	 */
	public void removeLayerChangeListener(LayerChangeListener listener) {
		listeners.remove(listener);
	}

	/**
	 * @return An unmodificable list of all layers
	 */
	public Collection<Layer> getAllLayers() {
		return Collections.unmodifiableCollection(layers);
	}

	/**
	 * Set the active selection to the given value and raise an layerchange event.
	 * Also, swap the active dataset in Main.main if it is a datalayer.
	 */
	public void setActiveLayer(Layer layer) {
		if (!layers.contains(layer))
			throw new IllegalArgumentException("layer must be in layerlist");
		Layer old = activeLayer;
		activeLayer = layer;
		if (layer instanceof OsmDataLayer)
			Main.main.ds = ((OsmDataLayer)layer).data;
		if (old != layer) {
			for (LayerChangeListener l : listeners)
				l.activeLayerChange(old, layer);
			recalculateCenterScale();
		}
	}

	/**
	 * @return The current active layer
	 */
	public Layer getActiveLayer() {
		return activeLayer;
	}

	/**
	 * @return The current edit layer. If no edit layer exist, one is created.
	 * 		So editLayer does never return <code>null</code>.
	 */
	public OsmDataLayer editLayer() {
		if (editLayer == null)
			addLayer(new OsmDataLayer(new DataSet(), "unnamed"));
		return editLayer;
	}
}
