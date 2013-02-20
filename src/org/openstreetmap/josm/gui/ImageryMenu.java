// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui;

import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trc;

import java.awt.Component;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.MenuElement;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.AddImageryLayerAction;
import org.openstreetmap.josm.actions.JosmAction;
import org.openstreetmap.josm.actions.Map_Rectifier_WMSmenuAction;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.imagery.ImageryInfo;
import org.openstreetmap.josm.data.imagery.ImageryLayerInfo;
import org.openstreetmap.josm.data.imagery.Shape;
import org.openstreetmap.josm.gui.layer.ImageryLayer;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.preferences.imagery.ImageryPreference;
import org.openstreetmap.josm.tools.ImageProvider;

public class ImageryMenu extends JMenu implements MapView.LayerChangeListener {

    private Action offsetAction = new JosmAction(
            tr("Imagery offset"), "mapmode/adjustimg", tr("Adjust imagery offset"), null, false, false) {
        {
            putValue("toolbar", "imagery-offset");
            Main.toolbar.register(this);
        }
        @Override
        public void actionPerformed(ActionEvent e) {
            List<ImageryLayer> layers = Main.map.mapView.getLayersOfType(ImageryLayer.class);
            if (layers.isEmpty()) {
                setEnabled(false);
                return;
            }
            Component source = null;
            if (e.getSource() instanceof Component) {
                source = (Component)e.getSource();
            }
            JPopupMenu popup = new JPopupMenu();
            if (layers.size() == 1) {
                JComponent c = layers.get(0).getOffsetMenuItem(popup);
                if (c instanceof JMenuItem) {
                    ((JMenuItem) c).getAction().actionPerformed(e);
                } else {
                    if (source == null) return;
                    popup.show(source, source.getWidth()/2, source.getHeight()/2);
                }
                return;
            }
            if (source == null) return;
            for (ImageryLayer layer : layers) {
                JMenuItem layerMenu = layer.getOffsetMenuItem();
                layerMenu.setText(layer.getName());
                layerMenu.setIcon(layer.getIcon());
                popup.add(layerMenu);
            }
            popup.show(source, source.getWidth()/2, source.getHeight()/2);
        }
    };

    private JMenuItem singleOffset = new JMenuItem(offsetAction);
    private JMenuItem offsetMenuItem = singleOffset;
    private Map_Rectifier_WMSmenuAction rectaction = new Map_Rectifier_WMSmenuAction();

    public ImageryMenu() {
        super(tr("Imagery"));
        setupMenuScroller();
        MapView.addLayerChangeListener(this);
        // build dynamically
        addMenuListener(new MenuListener() {
            @Override
            public void menuSelected(MenuEvent e) {
                refreshImageryMenu();
            }

            @Override
            public void menuDeselected(MenuEvent e) {
            }

            @Override
            public void menuCanceled(MenuEvent e) {
            }
        });
    }
    
    private void setupMenuScroller() {
        int screenHeight = Toolkit.getDefaultToolkit().getScreenSize().height;
        int menuItemHeight = singleOffset.getPreferredSize().height;
        MenuScroller.setScrollerFor(this, (screenHeight / menuItemHeight)-1);
    }

    /**
     * Refresh imagery menu.
     *
     * Outside this class only called in {@link ImageryPreference#initialize()}.
     * (In order to have actions ready for the toolbar, see #8446.)
     */
    public void refreshImageryMenu() {
        removeAll();

        // for each configured ImageryInfo, add a menu entry.
        for (final ImageryInfo u : ImageryLayerInfo.instance.getLayers()) {
            add(new AddImageryLayerAction(u));
        }

        // list all imagery entries where the current map location
        // is within the imagery bounds
        if (Main.isDisplayingMapView()) {
            MapView mv = Main.map.mapView;
            LatLon pos = mv.getProjection().eastNorth2latlon(mv.getCenter());
            final Set<ImageryInfo> inViewLayers = new HashSet<ImageryInfo>();

            for (ImageryInfo i : ImageryLayerInfo.instance.getDefaultLayers()) {
                if (i.getBounds() != null && i.getBounds().contains(pos)) {
                    inViewLayers.add(i);
                }
            }
            // Do not suggest layers already in use
            inViewLayers.removeAll(ImageryLayerInfo.instance.getLayers());
            // For layers containing complex shapes, check that center is in one
            // of its shapes (fix #7910)
            for (Iterator<ImageryInfo> iti = inViewLayers.iterator(); iti.hasNext(); ) {
                List<Shape> shapes = iti.next().getBounds().getShapes();
                if (shapes != null && !shapes.isEmpty()) {
                    boolean found = false;
                    for (Iterator<Shape> its = shapes.iterator(); its.hasNext() && !found; ) {
                        found = its.next().contains(pos);
                    }
                    if (!found) {
                        iti.remove();
                    }
                }
            }
            if (!inViewLayers.isEmpty()) {
                addSeparator();
                for (ImageryInfo i : inViewLayers) {
                    add(new AddImageryLayerAction(i));
                }
            }
        }

        addSeparator();
        add(new JMenuItem(rectaction));

        addSeparator();
        add(offsetMenuItem);
    }

    private JMenuItem getNewOffsetMenu(){
        if (Main.map == null || Main.map.mapView == null) {
            offsetAction.setEnabled(false);
            return singleOffset;
        }
        List<ImageryLayer> layers = Main.map.mapView.getLayersOfType(ImageryLayer.class);
        if (layers.isEmpty()) {
            offsetAction.setEnabled(false);
            return singleOffset;
        }
        offsetAction.setEnabled(true);
        JMenu newMenu = new JMenu(trc("layer","Offset")) {
            // Hack to prevent ToolbarPreference from tracing this menu
            // TODO: Modify ToolbarPreference to not to trace such dynamic submenus?
            @Override
            public MenuElement[] getSubElements() {
                return new MenuElement[0];
            }
        };
        newMenu.setIcon(ImageProvider.get("mapmode", "adjustimg"));
        newMenu.setAction(offsetAction);
        if (layers.size() == 1)
            return (JMenuItem)layers.get(0).getOffsetMenuItem(newMenu);
        for (ImageryLayer layer : layers) {
            JMenuItem layerMenu = layer.getOffsetMenuItem();
            layerMenu.setText(layer.getName());
            layerMenu.setIcon(layer.getIcon());
            newMenu.add(layerMenu);
        }
        return newMenu;
    }

    public void refreshOffsetMenu() {
        offsetMenuItem = getNewOffsetMenu();
    }

    @Override
    public void activeLayerChange(Layer oldLayer, Layer newLayer) {
    }

    @Override
    public void layerAdded(Layer newLayer) {
        if (newLayer instanceof ImageryLayer) {
            refreshOffsetMenu();
        }
    }

    @Override
    public void layerRemoved(Layer oldLayer) {
        if (oldLayer instanceof ImageryLayer) {
            refreshOffsetMenu();
        }
    }
}
