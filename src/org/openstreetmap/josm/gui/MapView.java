// License: GPL. See LICENSE file for details.

package org.openstreetmap.josm.gui;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.geom.GeneralPath;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedList;

import javax.swing.AbstractButton;
import javax.swing.JComponent;
import javax.swing.JOptionPane;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.AutoScaleAction;
import org.openstreetmap.josm.actions.JosmAction;
import org.openstreetmap.josm.actions.MoveAction;
import org.openstreetmap.josm.actions.mapmode.MapMode;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.ProjectionBounds;
import org.openstreetmap.josm.data.SelectionChangedListener;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.DataSource;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.visitor.BoundingXYVisitor;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.MapViewPaintable;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.gui.layer.OsmDataLayer.ModifiedChangedListener;
import org.openstreetmap.josm.gui.layer.markerlayer.MarkerLayer;
import org.openstreetmap.josm.gui.layer.markerlayer.PlayHeadMarker;
import org.openstreetmap.josm.tools.AudioPlayer;

/**
 * This is a component used in the MapFrame for browsing the map. It use is to
 * provide the MapMode's enough capabilities to operate.
 *
 * MapView hold meta-data about the data set currently displayed, as scale level,
 * center point viewed, what scrolling mode or editing mode is selected or with
 * what projection the map is viewed etc..
 *
 * MapView is able to administrate several layers.
 *
 * @author imi
 */
public class MapView extends NavigatableComponent {


    /**
     * A list of all layers currently loaded.
     */
    private ArrayList<Layer> layers = new ArrayList<Layer>();
    /**
     * The play head marker: there is only one of these so it isn't in any specific layer
     */
    public PlayHeadMarker playHeadMarker = null;

    /**
     * The layer from the layers list that is currently active.
     */
    private Layer activeLayer;

    /**
     * The last event performed by mouse.
     */
    public MouseEvent lastMEvent;

    private LinkedList<MapViewPaintable> temporaryLayers = new LinkedList<MapViewPaintable>();

    private BufferedImage offscreenBuffer;

    public MapView() {
        addComponentListener(new ComponentAdapter(){
            @Override public void componentResized(ComponentEvent e) {
                removeComponentListener(this);

                MapSlider zoomSlider = new MapSlider(MapView.this);
                add(zoomSlider);
                zoomSlider.setBounds(3, 0, 114, 30);

                MapScaler scaler = new MapScaler(MapView.this);
                add(scaler);
                scaler.setLocation(10,30);

                if (!zoomToEditLayerBoundingBox()) {
                    new AutoScaleAction("data").actionPerformed(null);
                }

                new MapMover(MapView.this, Main.contentPane);
                JosmAction mv;
                mv = new MoveAction(MoveAction.Direction.UP);
                if (mv.getShortcut() != null) {
                    Main.contentPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(mv.getShortcut().getKeyStroke(), "UP");
                    Main.contentPane.getActionMap().put("UP", mv);
                }
                mv = new MoveAction(MoveAction.Direction.DOWN);
                if (mv.getShortcut() != null) {
                    Main.contentPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(mv.getShortcut().getKeyStroke(), "DOWN");
                    Main.contentPane.getActionMap().put("DOWN", mv);
                }
                mv = new MoveAction(MoveAction.Direction.LEFT);
                if (mv.getShortcut() != null) {
                    Main.contentPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(mv.getShortcut().getKeyStroke(), "LEFT");
                    Main.contentPane.getActionMap().put("LEFT", mv);
                }
                mv = new MoveAction(MoveAction.Direction.RIGHT);
                if (mv.getShortcut() != null) {
                    Main.contentPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(mv.getShortcut().getKeyStroke(), "RIGHT");
                    Main.contentPane.getActionMap().put("RIGHT", mv);
                }
            }
        });

        // listend to selection changes to redraw the map
        DataSet.selListeners.add(new SelectionChangedListener(){
            public void selectionChanged(Collection<? extends OsmPrimitive> newSelection) {
                repaint();
            }
        });

        //store the last mouse action
        this.addMouseMotionListener(new MouseMotionListener() {
            public void mouseDragged(MouseEvent e) {
                mouseMoved(e);
            }
            public void mouseMoved(MouseEvent e) {
                lastMEvent = e;
            }
        });
    }

    /**
     * Add a layer to the current MapView. The layer will be added at topmost
     * position.
     */
    public void addLayer(Layer layer) {
        if (layer instanceof OsmDataLayer) {
            OsmDataLayer editLayer = (OsmDataLayer)layer;
            editLayer.listenerModified.add(new ModifiedChangedListener(){
                public void modifiedChanged(boolean value, OsmDataLayer source) {
                    JOptionPane.getFrameForComponent(Main.parent).setTitle((value?"*":"")
                            +tr("Java OpenStreetMap Editor"));
                }
            });
        }
        if (layer instanceof MarkerLayer && playHeadMarker == null) {
            playHeadMarker = PlayHeadMarker.create();
        }
        int pos = layers.size();
        while(pos > 0 && layers.get(pos-1).background) {
            --pos;
        }
        layers.add(pos, layer);

        for (Layer.LayerChangeListener l : Layer.listeners) {
            l.layerAdded(layer);
        }
        if (layer instanceof OsmDataLayer || activeLayer == null) {
            // autoselect the new layer
            Layer old = activeLayer;
            setActiveLayer(layer);
            for (Layer.LayerChangeListener l : Layer.listeners) {
                l.activeLayerChange(old, layer);
            }
        }
        AudioPlayer.reset();
        repaint();
    }

    @Override
    protected DataSet getCurrentDataSet() {
        if(activeLayer != null && activeLayer instanceof OsmDataLayer)
            return ((OsmDataLayer)activeLayer).data;
        return null;
    }

    /**
     * Replies true if the active layer is drawable.
     * 
     * @return true if the active layer is drawable, false otherwise
     */
    public boolean isActiveLayerDrawable() {
        return activeLayer != null && activeLayer instanceof OsmDataLayer;
    }

    /**
     * Replies true if the active layer is visible.
     * 
     * @return true if the active layer is visible, false otherwise
     */
    public boolean isActiveLayerVisible() {
        return isActiveLayerDrawable() && activeLayer.visible;
    }

    /**
     * Remove the layer from the mapview. If the layer was in the list before,
     * an LayerChange event is fired.
     */
    public void removeLayer(Layer layer) {
        if (layer == activeLayer) {
            activeLayer = null;
        }
        if (layers.remove(layer)) {
            for (Layer.LayerChangeListener l : Layer.listeners) {
                l.layerRemoved(layer);
            }
        }
        layer.destroy();
        AudioPlayer.reset();
    }

    private boolean virtualNodesEnabled = false;
    public void setVirtualNodesEnabled(boolean enabled) {
        if(virtualNodesEnabled != enabled) {
            virtualNodesEnabled = enabled;
            repaint();
        }
    }
    public boolean isVirtualNodesEnabled() {
        return virtualNodesEnabled;
    }

    /**
     * Moves the layer to the given new position. No event is fired.
     * @param layer     The layer to move
     * @param pos       The new position of the layer
     */
    public void moveLayer(Layer layer, int pos) {
        int curLayerPos = layers.indexOf(layer);
        if (curLayerPos == -1)
            throw new IllegalArgumentException(tr("layer not in list."));
        if (pos == curLayerPos)
            return; // already in place.
        layers.remove(curLayerPos);
        if (pos >= layers.size()) {
            layers.add(layer);
        } else {
            layers.add(pos, layer);
        }
        AudioPlayer.reset();
    }


    public int getLayerPos(Layer layer) {
        int curLayerPos = layers.indexOf(layer);
        if (curLayerPos == -1)
            throw new IllegalArgumentException(tr("layer not in list."));
        return curLayerPos;
    }

    /**
     * Draw the component.
     */
    @Override public void paint(Graphics g) {
        if (center == null)
            return; // no data loaded yet.

        // re-create offscreen-buffer if we've been resized, otherwise
        // just re-use it.
        if (null == offscreenBuffer || offscreenBuffer.getWidth() != getWidth()
                || offscreenBuffer.getHeight() != getHeight()) {
            offscreenBuffer = new BufferedImage(getWidth(), getHeight(),
                    BufferedImage.TYPE_INT_ARGB);
        }

        Graphics2D tempG = offscreenBuffer.createGraphics();
        tempG.setColor(Main.pref.getColor("background", Color.BLACK));
        tempG.fillRect(0, 0, getWidth(), getHeight());

        for (int i = layers.size()-1; i >= 0; --i) {
            Layer l = layers.get(i);
            if (l.visible/* && l != getActiveLayer()*/) {
                l.paint(tempG, this);
            }
        }

        /*if (getActiveLayer() != null && getActiveLayer().visible)
            getActiveLayer().paint(tempG, this);*/

        for (MapViewPaintable mvp : temporaryLayers) {
            mvp.paint(tempG, this);
        }

        // draw world borders
        tempG.setColor(Color.WHITE);
        GeneralPath path = new GeneralPath();
        Bounds b = getProjection().getWorldBoundsLatLon();
        double lat = b.min.lat();
        double lon = b.min.lon();

        Point p = getPoint(b.min);
        path.moveTo(p.x, p.y);

        double max = b.max.lat();
        for(; lat <= max; lat += 1.0)
        {
            p = getPoint(new LatLon(lat >= max ? max : lat, lon));
            path.lineTo(p.x, p.y);
        }
        lat = max; max = b.max.lon();
        for(; lon <= max; lon += 1.0)
        {
            p = getPoint(new LatLon(lat, lon >= max ? max : lon));
            path.lineTo(p.x, p.y);
        }
        lon = max; max = b.min.lat();
        for(; lat >= max; lat -= 1.0)
        {
            p = getPoint(new LatLon(lat <= max ? max : lat, lon));
            path.lineTo(p.x, p.y);
        }
        lat = max; max = b.min.lon();
        for(; lon >= max; lon -= 1.0)
        {
            p = getPoint(new LatLon(lat, lon <= max ? max : lon));
            path.lineTo(p.x, p.y);
        }

        if (playHeadMarker != null) {
            playHeadMarker.paint(tempG, this);
        }
        tempG.draw(path);

        g.drawImage(offscreenBuffer, 0, 0, null);
        super.paint(g);
    }

    /**
     * Set the new dimension to the view.
     */
    public void recalculateCenterScale(BoundingXYVisitor box) {
        if (box == null) {
            box = new BoundingXYVisitor();
        }
        if (box.getBounds() == null) {
            box.visit(getProjection().getWorldBoundsLatLon());
        }
        if (!box.hasExtend()) {
            box.enlargeBoundingBox();
        }

        zoomTo(box.getBounds());
    }

    /**
     * @return An unmodifiable collection of all layers
     */
    public Collection<Layer> getAllLayers() {
        return Collections.unmodifiableCollection(layers);
    }

    /**
     * Sets the active layer to <code>layer</code>. If <code>layer</code> is an instance
     * of {@see OsmDataLayer} also sets {@see #editLayer} to <code>layer</code>.
     * 
     * @param layer the layer to be activate; must be one of the layers in the list of layers
     * @exception IllegalArgumentException thrown if layer is not in the lis of layers
     */
    public void setActiveLayer(Layer layer) {
        if (!layers.contains(layer))
            throw new IllegalArgumentException(tr("Layer ''{0}'' must be in list of layers", layer.toString()));
        if (! (layer instanceof OsmDataLayer)) {
            if (getCurrentDataSet() != null) {
                getCurrentDataSet().setSelected();
                DataSet.fireSelectionChanged(getCurrentDataSet().getSelected());
            }
        }
        Layer old = activeLayer;
        activeLayer = layer;
        if (old != layer) {
            for (Layer.LayerChangeListener l : Layer.listeners) {
                l.activeLayerChange(old, layer);
            }
        }

        /* This only makes the buttons look disabled. Disabling the actions as well requires
         * the user to re-select the tool after i.e. moving a layer. While testing I found
         * that I switch layers and actions at the same time and it was annoying to mind the
         * order. This way it works as visual clue for new users */
        for (Enumeration<AbstractButton> e = Main.map.toolGroup.getElements() ; e.hasMoreElements() ;) {
            AbstractButton x=e.nextElement();
            x.setEnabled(((MapMode)x.getAction()).layerIsSupported(layer));
        }
        AudioPlayer.reset();
        repaint();
    }

    /**
     * Replies the currently active layer
     * 
     * @return the currently active layer (may be null)
     */
    public Layer getActiveLayer() {
        return activeLayer;
    }

    /**
     * Replies the current edit layer, if any
     * 
     * @return the current edit layer. May be null.
     */
    public OsmDataLayer getEditLayer() {
        if (activeLayer instanceof OsmDataLayer)
            return (OsmDataLayer)activeLayer;

        // the first OsmDataLayer is the edit layer
        //
        for (Layer layer : layers) {
            if (layer instanceof OsmDataLayer)
                return (OsmDataLayer)layer;
        }
        return null;
    }

    /**
     * replies true if the list of layers managed by this map view contain layer
     * 
     * @param layer the layer
     * @return true if the list of layers managed by this map view contain layer
     */
    public boolean hasLayer(Layer layer) {
        return layers.contains(layer);
    }

    /**
     * Tries to zoom to the download boundingbox[es] of the current edit layer
     * (aka {@link OsmDataLayer}). If the edit layer has multiple download bounding
     * boxes it zooms to a large virtual bounding box containing all smaller ones.
     * This implementation can be used for resolving ticket #1461.
     *
     * @return <code>true</code> if a zoom operation has been performed
     */
    public boolean zoomToEditLayerBoundingBox() {
        // workaround for #1461 (zoom to download bounding box instead of all data)
        // In case we already have an existing data layer ...
        OsmDataLayer layer= getEditLayer();
        if (layer == null)
            return false;
        Collection<DataSource> dataSources = layer.data.dataSources;
        // ... with bounding box[es] of data loaded from OSM or a file...
        BoundingXYVisitor bbox = new BoundingXYVisitor();
        for (DataSource ds : dataSources) {
            bbox.visit(ds.bounds);
            if (bbox.hasExtend()) {
                // ... we zoom to it's bounding box
                recalculateCenterScale(bbox);
                return true;
            }
        }
        return false;
    }

    public boolean addTemporaryLayer(MapViewPaintable mvp) {
        if (temporaryLayers.contains(mvp)) return false;
        return temporaryLayers.add(mvp);
    }

    public boolean removeTemporaryLayer(MapViewPaintable mvp) {
        return temporaryLayers.remove(mvp);
    }
}
