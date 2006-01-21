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
import java.util.LinkedList;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.GeoPoint;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
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
public class MapView extends NavigatableComponent implements ChangeListener, PropertyChangeListener  {

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
			getProjection().init(layer.getBoundsLatLon());

		// reinitialize layer's data
		layer.init(getProjection());

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

		// draw world borders
		g.setColor(Color.WHITE);
		Bounds b = new Bounds();
		Point min = getScreenPoint(b.min);
		Point max = getScreenPoint(b.max);
		int x1 = Math.min(min.x, max.x);
		int y1 = Math.min(min.y, max.y);
		int x2 = Math.max(min.x, max.x);
		int y2 = Math.max(min.y, max.y);
		if (x1 > 0 || y1 > 0 || x2 < getWidth() || y2 < getHeight())
			g.drawRect(x1, y1, x2-x1+1, y2-y1+1);
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
				getProjection().latlon2xy(center);
				scale = 10;
			} else {
				center = bounds.centerXY();
				getProjection().xy2latlon(center);
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

	/**
	 * In addition to the base class funcitonality, this keep trak of the autoscale
	 * feature.
	 */
	@Override
	public void zoomTo(GeoPoint newCenter, double scale) {
		boolean oldAutoScale = autoScale;
		GeoPoint oldCenter = center;
		double oldScale = this.scale;
		autoScale = false;

		super.zoomTo(newCenter, scale);
		
		recalculateCenterScale();
		
		firePropertyChange("center", oldCenter, center);
		if (oldAutoScale != autoScale)
			firePropertyChange("autoScale", oldAutoScale, autoScale);
		if (oldScale != scale)
			firePropertyChange("scale", oldScale, scale);
	}

	/**
	 * Notify from the projection, that something has changed.
	 */
	public void stateChanged(ChangeEvent e) {
		// reset all datasets.
		Projection p = getProjection();
		for (Node n : Main.main.ds.nodes)
			p.latlon2xy(n.coor);
		recalculateCenterScale();
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
}
