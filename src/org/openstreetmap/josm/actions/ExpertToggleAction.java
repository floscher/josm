// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.openstreetmap.josm.Main;

/**
 * This action toggles the Expert mode.
 * @since 4840
 */
public class ExpertToggleAction extends ToggleAction {

    public interface ExpertModeChangeListener {
        void expertChanged(boolean isExpert);
    }

    private static final List<WeakReference<ExpertModeChangeListener>> listeners = new ArrayList<>();
    private static final List<WeakReference<Component>> visibilityToggleListeners = new ArrayList<>();

    private static final ExpertToggleAction INSTANCE = new ExpertToggleAction();

    private static synchronized void fireExpertModeChanged(boolean isExpert) {
        Iterator<WeakReference<ExpertModeChangeListener>> it1 = listeners.iterator();
        while (it1.hasNext()) {
            WeakReference<ExpertModeChangeListener> wr = it1.next();
            ExpertModeChangeListener listener = wr.get();
            if (listener == null) {
                it1.remove();
                continue;
            }
            listener.expertChanged(isExpert);
        }
        Iterator<WeakReference<Component>> it2 = visibilityToggleListeners.iterator();
        while (it2.hasNext()) {
            WeakReference<Component> wr = it2.next();
            Component c = wr.get();
            if (c == null) {
                it2.remove();
                continue;
            }
            c.setVisible(isExpert);
        }
    }

    /**
     * Register a expert mode change listener
     *
     * @param listener the listener. Ignored if null.
     */
    public static void addExpertModeChangeListener(ExpertModeChangeListener listener) {
        addExpertModeChangeListener(listener, false);
    }

    public static synchronized void addExpertModeChangeListener(ExpertModeChangeListener listener, boolean fireWhenAdding) {
        if (listener == null) return;
        for (WeakReference<ExpertModeChangeListener> wr : listeners) {
            // already registered ? => abort
            if (wr.get() == listener) return;
        }
        listeners.add(new WeakReference<>(listener));
        if (fireWhenAdding) {
            listener.expertChanged(isExpert());
        }
    }

    /**
     * Removes a expert mode change listener
     *
     * @param listener the listener. Ignored if null.
     */
    public static synchronized void removeExpertModeChangeListener(ExpertModeChangeListener listener) {
        if (listener == null) return;
        Iterator<WeakReference<ExpertModeChangeListener>> it = listeners.iterator();
        while (it.hasNext()) {
            WeakReference<ExpertModeChangeListener> wr = it.next();
            // remove the listener - and any other listener which god garbage
            // collected in the meantime
            if (wr.get() == null || wr.get() == listener) {
                it.remove();
            }
        }
    }

    public static synchronized void addVisibilitySwitcher(Component c) {
        if (c == null) return;
        for (WeakReference<Component> wr : visibilityToggleListeners) {
            // already registered ? => abort
            if (wr.get() == c) return;
        }
        visibilityToggleListeners.add(new WeakReference<>(c));
        c.setVisible(isExpert());
    }

    public static synchronized void removeVisibilitySwitcher(Component c) {
        if (c == null) return;
        Iterator<WeakReference<Component>> it = visibilityToggleListeners.iterator();
        while (it.hasNext()) {
            WeakReference<Component> wr = it.next();
            // remove the listener - and any other listener which god garbage
            // collected in the meantime
            if (wr.get() == null || wr.get() == c) {
                it.remove();
            }
        }
    }

    /**
     * Constructs a new {@code ExpertToggleAction}.
     */
    public ExpertToggleAction() {
        super(tr("Expert Mode"),
              "expert",
              tr("Enable/disable expert mode"),
              null,
              false /* register toolbar */
        );
        putValue("toolbar", "expertmode");
        if (Main.toolbar != null) {
            Main.toolbar.register(this);
        }
        setSelected(Main.pref.getBoolean("expert", false));
        notifySelectedState();
    }

    @Override
    protected final void notifySelectedState() {
        super.notifySelectedState();
        fireExpertModeChanged(isSelected());
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        toggleSelectedState(e);
        Main.pref.put("expert", isSelected());
        notifySelectedState();
    }

    /**
     * Replies the unique instance of this action.
     * @return The unique instance of this action
     */
    public static ExpertToggleAction getInstance() {
        return INSTANCE;
    }

    /**
     * Determines if expert mode is enabled.
     * @return {@code true} if expert mode is enabled, {@code false} otherwise.
     */
    public static boolean isExpert() {
        return INSTANCE.isSelected();
    }
}
