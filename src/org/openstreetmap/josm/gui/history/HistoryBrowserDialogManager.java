// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.history;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Dimension;
import java.awt.Point;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.history.History;
import org.openstreetmap.josm.data.osm.history.HistoryDataSet;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.tools.BugReportExceptionHandler;
import org.openstreetmap.josm.tools.Predicate;
import org.openstreetmap.josm.tools.Utils;
import org.openstreetmap.josm.tools.WindowGeometry;

public class HistoryBrowserDialogManager implements MapView.LayerChangeListener {
    static private HistoryBrowserDialogManager instance;
    static public HistoryBrowserDialogManager getInstance() {
        if (instance == null) {
            instance = new HistoryBrowserDialogManager();
        }
        return instance;
    }

    private Map<Long, HistoryBrowserDialog> dialogs;

    protected HistoryBrowserDialogManager() {
        dialogs = new HashMap<Long, HistoryBrowserDialog>();
        MapView.addLayerChangeListener(this);
    }

    public boolean existsDialog(long id) {
        return dialogs.containsKey(id);
    }

    public void show(long id, HistoryBrowserDialog dialog) {
        if (dialogs.values().contains(dialog)) {
            show(id);
        } else {
            placeOnScreen(dialog);
            dialog.setVisible(true);
            dialogs.put(id, dialog);
        }
    }

    public void show(long id) {
        if (dialogs.keySet().contains(id)) {
            dialogs.get(id).toFront();
        }
    }

    protected boolean hasDialogWithCloseUpperLeftCorner(Point p) {
        for (HistoryBrowserDialog dialog: dialogs.values()) {
            Point corner = dialog.getLocation();
            if (p.x >= corner.x -5 && corner.x + 5 >= p.x
                    && p.y >= corner.y -5 && corner.y + 5 >= p.y)
                return true;
        }
        return false;
    }

    public void placeOnScreen(HistoryBrowserDialog dialog) {
        WindowGeometry geometry = WindowGeometry.centerOnScreen(new Dimension(800,500));
        geometry.applySafe(dialog);
        Point p = dialog.getLocation();
        while(hasDialogWithCloseUpperLeftCorner(p)) {
            p.x +=20;
            p.y += 20;
        }
        dialog.setLocation(p);
    }

    public void hide(HistoryBrowserDialog dialog) {
        long id = 0;
        for (long i: dialogs.keySet()) {
            if (dialogs.get(i) == dialog) {
                id = i;
                break;
            }
        }
        if (id > 0) {
            dialogs.remove(id);
        }
        dialog.setVisible(false);
        dialog.dispose();
    }

    /**
     * Hides and destroys all currently visible history browser dialogs
     *
     */
    public void hideAll() {
        List<HistoryBrowserDialog> dialogs = new ArrayList<HistoryBrowserDialog>();
        dialogs.addAll(this.dialogs.values());
        for (HistoryBrowserDialog dialog: dialogs) {
            dialog.unlinkAsListener();
            hide(dialog);
        }
    }

    public void show(History h) {
        if (h == null)
            return;
        if (existsDialog(h.getId())) {
            show(h.getId());
        } else {
            HistoryBrowserDialog dialog = new HistoryBrowserDialog(h);
            show(h.getId(), dialog);
        }
    }

    /* ----------------------------------------------------------------------------- */
    /* LayerChangeListener                                                           */
    /* ----------------------------------------------------------------------------- */
    @Override
    public void activeLayerChange(Layer oldLayer, Layer newLayer) {}
    @Override
    public void layerAdded(Layer newLayer) {}

    @Override
    public void layerRemoved(Layer oldLayer) {
        // remove all history browsers if the number of layers drops to 0
        //
        if (Main.isDisplayingMapView() && Main.map.mapView.getNumLayers() == 0) {
            hideAll();
        }
    }

    public void showHistory(final Collection<OsmPrimitive> primitives) {
        final Collection<OsmPrimitive> notNewPrimitives = Utils.filter(primitives, notNewPredicate);
        if (notNewPrimitives.isEmpty()) {
            JOptionPane.showMessageDialog(
                    Main.parent,
                    tr("Please select at least one already uploaded node, way, or relation."),
                    tr("Warning"),
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        Collection<OsmPrimitive> toLoad = Utils.filter(primitives, unloadedHistoryPredicate);
        if (!toLoad.isEmpty()) {
            HistoryLoadTask task = new HistoryLoadTask();
            task.add(notNewPrimitives);
            Main.worker.submit(task);
        }

        Runnable r = new Runnable() {

            @Override
            public void run() {
                try {
                    for (OsmPrimitive p : notNewPrimitives) {
                        final History h = HistoryDataSet.getInstance().getHistory(p.getPrimitiveId());
                        if (h == null) {
                            continue;
                        }
                        SwingUtilities.invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                show(h);
                            }
                        });
                    }
                } catch (final Exception e) {
                    BugReportExceptionHandler.handleException(e);
                }

            }
        };
        Main.worker.submit(r);
    }

    private final Predicate<OsmPrimitive> unloadedHistoryPredicate = new Predicate<OsmPrimitive>() {

        HistoryDataSet hds = HistoryDataSet.getInstance();

        @Override
        public boolean evaluate(OsmPrimitive p) {
            History h = hds.getHistory(p.getPrimitiveId());
            if (h == null)
                // reload if the history is not in the cache yet
                return true;
            else if (!p.isNew() && h.getByVersion(p.getUniqueId()) == null)
                // reload if the history object of the selected object is not in the cache yet
                return true;
            else
                return false;
        }
    };

    private final Predicate<OsmPrimitive> notNewPredicate = new Predicate<OsmPrimitive>() {

        @Override
        public boolean evaluate(OsmPrimitive p) {
            return !p.isNew();
        }
    };

}
