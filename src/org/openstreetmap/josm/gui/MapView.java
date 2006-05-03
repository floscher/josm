package org.openstreetmap.josm.gui;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;

import javax.swing.JOptionPane;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.AutoScaleAction;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.SelectionChangedListener;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.visitor.BoundingXYVisitor;
import org.openstreetmap.josm.data.osm.visitor.SimplePaintVisitor;
import org.openstreetmap.josm.data.projection.Projection;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.gui.layer.WmsServerLayer;
import org.openstreetmap.josm.gui.layer.OsmDataLayer.ModifiedChangedListener;

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
public class MapView extends NavigatableComponent {

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
	private boolean autoScale = true;

	/**
	 * A list of all layers currently loaded.
	 */
	private ArrayList<Layer> layers = new ArrayList<Layer>();
	/**
	 * Direct link to the edit layer (if any) in the layers list.
	 */
	public OsmDataLayer editLayer;
	/**
	 * The layer from the layers list that is currently active.
	 */
	private Layer activeLayer;
	/**
	 * The listener of the active layer changes.
	 */
	private Collection<LayerChangeListener> listeners = new LinkedList<LayerChangeListener>();

	private final AutoScaleAction autoScaleAction;


	private final class Scaler extends JSlider implements PropertyChangeListener, ChangeListener {
		boolean clicked = false;
		public Scaler() {
			super(0, 20);
			addMouseListener(new MouseAdapter(){
				@Override public void mousePressed(MouseEvent e) {
					clicked = true;
				}
				@Override public void mouseReleased(MouseEvent e) {
					clicked = false;
				}
			});
			MapView.this.addPropertyChangeListener("scale", this);
			addChangeListener(this);
		}
		public void propertyChange(PropertyChangeEvent evt) {
			if (!getModel().getValueIsAdjusting())
				setValue(zoom());
		}
		public void stateChanged(ChangeEvent e) {
			if (!clicked)
				return;
			EastNorth pos = world;
			for (int zoom = 0; zoom < getValue(); ++zoom)
				pos = new EastNorth(pos.east()/2, pos.north()/2);
			if (MapView.this.getWidth() < MapView.this.getHeight())
				zoomTo(center, pos.east()*2/(MapView.this.getWidth()-20));
			else
				zoomTo(center, pos.north()*2/(MapView.this.getHeight()-20));
		}
	}

	public MapView(AutoScaleAction autoScaleAction) {
		this.autoScaleAction = autoScaleAction;
		addComponentListener(new ComponentAdapter(){
			@Override public void componentResized(ComponentEvent e) {
				recalculateCenterScale();
			}
		});
		new MapMover(this);

		// listend to selection changes to redraw the map
		Main.ds.addSelectionChangedListener(new SelectionChangedListener(){
			public void selectionChanged(Collection<? extends OsmPrimitive> newSelection) {
				repaint();
			}
		});
		Scaler zoomScaler = new Scaler();
		zoomScaler.setOpaque(false);
		add(zoomScaler);
		zoomScaler.setBounds(0,0, 100, 30);
	}

	/**
	 * Add a layer to the current MapView. The layer will be added at topmost
	 * position.
	 */
	public void addLayer(Layer layer) {
		if (layer instanceof OsmDataLayer) {
			final OsmDataLayer dataLayer = (OsmDataLayer)layer;
			if (editLayer != null) {
				editLayer.mergeFrom(layer);
				repaint();
				return;
			}
			editLayer = dataLayer;
			dataLayer.data.addAllSelectionListener(Main.ds);
			Main.ds = dataLayer.data;
			dataLayer.listenerModified.add(new ModifiedChangedListener(){
				public void modifiedChanged(boolean value, OsmDataLayer source) {
					JOptionPane.getFrameForComponent(Main.parent).setTitle((value?"*":"")+"Java Open Street Map - Editor");
				}
			});
		}

		// add as a new layer
		if (layer instanceof WmsServerLayer)
			layers.add(layers.size(), layer);
		else
			layers.add(0, layer);

		for (LayerChangeListener l : listeners)
			l.layerAdded(layer);

		// autoselect the new layer
		setActiveLayer(layer);
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
	@Override public void paint(Graphics g) {
		g.setColor(SimplePaintVisitor.getPreferencesColor("background", Color.BLACK));
		g.fillRect(0, 0, getWidth(), getHeight());

		for (int i = layers.size()-1; i >= 0; --i) {
			Layer l = layers.get(i);
			if (l.visible)
				l.paint(g, this);
		}

		// draw world borders
		g.setColor(Color.WHITE);
		Bounds b = new Bounds();
		Point min = getPoint(getProjection().latlon2eastNorth(b.min));
		Point max = getPoint(getProjection().latlon2eastNorth(b.max));
		int x1 = Math.min(min.x, max.x);
		int y1 = Math.min(min.y, max.y);
		int x2 = Math.max(min.x, max.x);
		int y2 = Math.max(min.y, max.y);
		if (x1 > 0 || y1 > 0 || x2 < getWidth() || y2 < getHeight())
			g.drawRect(x1, y1, x2-x1+1, y2-y1+1);
		super.paint(g);
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
	public void recalculateCenterScale() {
		if (autoScale) {
			// -20 to leave some border
			int w = getWidth()-20;
			if (w < 20)
				w = 20;
			int h = getHeight()-20;
			if (h < 20)
				h = 20;

			BoundingXYVisitor v = autoScaleAction.getBoundingBox();

			boolean oldAutoScale = autoScale;
			EastNorth oldCenter = center;
			double oldScale = this.scale;

			if (v.min == null || v.max == null || v.min.equals(v.max)) {
				// no bounds means whole world 
				center = getProjection().latlon2eastNorth(new LatLon(0,0));
				EastNorth world = getProjection().latlon2eastNorth(new LatLon(Projection.MAX_LAT,Projection.MAX_LON));
				double scaleX = world.east()*2/w;
				double scaleY = world.north()*2/h;
				scale = Math.max(scaleX, scaleY); // minimum scale to see all of the screen
			} else {
				center = new EastNorth(v.min.east()/2+v.max.east()/2, v.min.north()/2+v.max.north()/2);
				double scaleX = (v.max.east()-v.min.east())/w;
				double scaleY = (v.max.north()-v.min.north())/h;
				scale = Math.max(scaleX, scaleY); // minimum scale to see all of the screen
			}

			if (!center.equals(oldCenter))
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
	 */
	public void setActiveLayer(Layer layer) {
		if (!layers.contains(layer))
			throw new IllegalArgumentException("layer must be in layerlist");
		Layer old = activeLayer;
		activeLayer = layer;
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
	 * In addition to the base class funcitonality, this keep trak of the autoscale
	 * feature.
	 */
	@Override public void zoomTo(EastNorth newCenter, double scale) {
		boolean oldAutoScale = autoScale;
		EastNorth oldCenter = center;
		double oldScale = this.scale;
		autoScale = false;

		super.zoomTo(newCenter, scale);

		recalculateCenterScale();

		if (!oldCenter.equals(center))
			firePropertyChange("center", oldCenter, center);
		if (oldAutoScale != autoScale)
			firePropertyChange("autoScale", oldAutoScale, autoScale);
		if (oldScale != scale)
			firePropertyChange("scale", oldScale, scale);
	}
}
