// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm.event;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Stream;

import javax.swing.SwingUtilities;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.SelectionChangedListener;
import org.openstreetmap.josm.data.osm.DataSelectionListener;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.event.DatasetEventManager.FireMode;
import org.openstreetmap.josm.gui.layer.MainLayerManager;
import org.openstreetmap.josm.gui.layer.MainLayerManager.ActiveLayerChangeEvent;
import org.openstreetmap.josm.gui.layer.MainLayerManager.ActiveLayerChangeListener;

/**
 * Similar like {@link DatasetEventManager}, just for selection events.
 *
 * It allows to register listeners to global selection events for the selection in the current edit layer.
 *
 * If you want to listen to selections to a specific data layer,
 * you can register a listener to that layer by using {@link DataSet#addSelectionListener(DataSelectionListener)}
 *
 * @since 2912
 */
public class SelectionEventManager implements DataSelectionListener, ActiveLayerChangeListener {

    private static final SelectionEventManager instance = new SelectionEventManager();

    /**
     * Returns the unique instance.
     * @return the unique instance
     */
    public static SelectionEventManager getInstance() {
        return instance;
    }

    private static class ListenerInfo {
        private final SelectionChangedListener listener;

        ListenerInfo(SelectionChangedListener listener) {
            this.listener = listener;
        }

        @Override
        public int hashCode() {
            return Objects.hash(listener);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ListenerInfo that = (ListenerInfo) o;
            return Objects.equals(listener, that.listener);
        }
    }

    private Collection<? extends OsmPrimitive> selection;
    private final CopyOnWriteArrayList<ListenerInfo> inEDTListeners = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<ListenerInfo> normalListeners = new CopyOnWriteArrayList<>();

    /**
     * Constructs a new {@code SelectionEventManager}.
     */
    protected SelectionEventManager() {
        MainLayerManager layerManager = Main.getLayerManager();
        // We do not allow for destructing this object.
        // Currently, this is a singleton class, so this is not required.
        layerManager.addAndFireActiveLayerChangeListener(this);
    }

    /**
     * Registers a new {@code SelectionChangedListener}.
     * @param listener listener to add
     * @param fireMode EDT firing mode
     */
    public void addSelectionListener(SelectionChangedListener listener, FireMode fireMode) {
        if (fireMode == FireMode.IN_EDT)
            throw new UnsupportedOperationException("IN_EDT mode not supported, you probably want to use IN_EDT_CONSOLIDATED.");
        if (fireMode == FireMode.IN_EDT || fireMode == FireMode.IN_EDT_CONSOLIDATED) {
            inEDTListeners.addIfAbsent(new ListenerInfo(listener));
        } else {
            normalListeners.addIfAbsent(new ListenerInfo(listener));
        }
    }

    /**
     * Unregisters a {@code SelectionChangedListener}.
     * @param listener listener to remove
     */
    public void removeSelectionListener(SelectionChangedListener listener) {
        ListenerInfo searchListener = new ListenerInfo(listener);
        inEDTListeners.remove(searchListener);
        normalListeners.remove(searchListener);
    }

    @Override
    public void activeOrEditLayerChanged(ActiveLayerChangeEvent e) {
        DataSet oldDataSet = e.getPreviousEditDataSet();
        if (oldDataSet != null) {
            // Fake a selection removal
            // Relying on this allows components to not have to monitor layer changes.
            // If we would not do this, e.g. the move command would have a hard time tracking which layer
            // the last moved selection was in.
            SelectionReplaceEvent event = new SelectionReplaceEvent(oldDataSet,
                    new HashSet<>(oldDataSet.getAllSelected()), Stream.empty());
            selectionChanged(event);
            oldDataSet.removeSelectionListener(this);
        }
        DataSet newDataSet = e.getSource().getEditDataSet();
        if (newDataSet != null) {
            newDataSet.addSelectionListener(this);
            // Fake a selection add
            SelectionReplaceEvent event = new SelectionReplaceEvent(newDataSet,
                    Collections.emptySet(), newDataSet.getAllSelected().stream());
            selectionChanged(event);
        }
    }

    @Override
    public void selectionChanged(SelectionChangeEvent e) {
        Set<OsmPrimitive> newSelection = e.getSelection();
        fireEvents(normalListeners, newSelection);
        selection = newSelection;
        SwingUtilities.invokeLater(edtRunnable);
    }

    private static void fireEvents(List<ListenerInfo> listeners, Collection<? extends OsmPrimitive> newSelection) {
        for (ListenerInfo listener: listeners) {
            listener.listener.selectionChanged(newSelection);
        }
    }

    private final Runnable edtRunnable = () -> {
        if (selection != null) {
            fireEvents(inEDTListeners, selection);
        }
    };
}
