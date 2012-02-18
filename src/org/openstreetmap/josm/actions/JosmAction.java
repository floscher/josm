// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.KeyEvent;
import java.util.Collection;

import javax.swing.AbstractAction;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.SelectionChangedListener;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.tools.Destroyable;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.Shortcut;

/**
 * Base class helper for all Actions in JOSM. Just to make the life easier.
 *
 * A JosmAction is a {@see LayerChangeListener} and a {@see SelectionChangedListener}. Upon
 * a layer change event or a selection change event it invokes {@see #updateEnabled()}.
 * Subclasses can override {@see #updateEnabled()} in order to update the {@see #isEnabled()}-state
 * of a JosmAction depending on the {@see #getCurrentDataSet()} and the current layers
 * (see also {@see #getEditLayer()}).
 *
 * destroy() from interface Destroyable is called e.g. for MapModes, when the last layer has
 * been removed and so the mapframe will be destroyed. For other JosmActions, destroy() may never
 * be called (currently).
 *
 * @author imi
 */
abstract public class JosmAction extends AbstractAction implements Destroyable {

    protected Shortcut sc;
    private LayerChangeAdapter layerChangeAdapter;
    private SelectionChangeAdapter selectionChangeAdapter;

    public Shortcut getShortcut() {
        if (sc == null) {
            sc = Shortcut.registerShortcut("core:none", tr("No Shortcut"), KeyEvent.CHAR_UNDEFINED, Shortcut.NONE);
            // as this shortcut is shared by all action that don't want to have a shortcut,
            // we shouldn't allow the user to change it...
            // this is handled by special name "core:none"
        }
        return sc;
    }

    /**
     * The new super for all actions.
     *
     * Use this super constructor to setup your action.
     *
     * @param name the action's text as displayed on the menu (if it is added to a menu)
     * @param iconName the filename of the icon to use
     * @param tooltip  a longer description of the action that will be displayed in the tooltip. Please note
     *           that html is not supported for menu actions on some platforms.
     * @param shortcut a ready-created shortcut object or null if you don't want a shortcut. But you always
     *            do want a shortcut, remember you can always register it with group=none, so you
     *            won't be assigned a shortcut unless the user configures one. If you pass null here,
     *            the user CANNOT configure a shortcut for your action.
     * @param register register this action for the toolbar preferences?
     * @param toolbarId identifier for the toolbar preferences. The iconName is used, if this parameter is null
     * @param installAdapters false, if you don't want to install layer changed and selection changed adapters
     */
    public JosmAction(String name, String iconName, String tooltip, Shortcut shortcut, boolean register, String toolbarId, boolean installAdapters) {
        super(name, iconName == null ? null : ImageProvider.get(iconName));
        setHelpId();
        sc = shortcut;
        if (sc != null) {
            Main.registerActionShortcut(this, sc);
        }
        setTooltip(tooltip);
        if (getValue("toolbar") == null) {
            putValue("toolbar", toolbarId == null ? iconName : toolbarId);
        }
        if (register) {
            Main.toolbar.register(this);
        }
        if (installAdapters) {
            installAdapters();
        }
    }

    public JosmAction(String name, String iconName, String tooltip, Shortcut shortcut, boolean register, boolean installAdapters) {
        this(name, iconName, tooltip, shortcut, register, null, installAdapters);
    }

    public JosmAction(String name, String iconName, String tooltip, Shortcut shortcut, boolean register) {
        this(name, iconName, tooltip, shortcut, register, null, true);
    }

    public JosmAction() {
        this(true);
    }

    public JosmAction(boolean installAdapters) {
        setHelpId();
        if (installAdapters) {
            installAdapters();
        }
    }

    public void destroy() {
        if (sc != null) {
            Main.unregisterActionShortcut(this);
        }
        MapView.removeLayerChangeListener(layerChangeAdapter);
        DataSet.removeSelectionListener(selectionChangeAdapter);
    }

    private void setHelpId() {
        String helpId = "Action/"+getClass().getName().substring(getClass().getName().lastIndexOf('.')+1);
        if (helpId.endsWith("Action")) {
            helpId = helpId.substring(0, helpId.length()-6);
        }
        putValue("help", helpId);
    }

    public void setTooltip(String tooltip) {
        putValue(SHORT_DESCRIPTION, Main.platform.makeTooltip(tooltip, sc));
    }

    /**
     * Replies the current edit layer
     *
     * @return the current edit layer. null, if no edit layer exists
     */
    protected static OsmDataLayer getEditLayer() {
        return Main.main.getEditLayer();
    }

    /**
     * Replies the current dataset
     *
     * @return the current dataset. null, if no current dataset exists
     */
    protected static DataSet getCurrentDataSet() {
        return Main.main.getCurrentDataSet();
    }

    protected void installAdapters() {
        // make this action listen to layer change and selection change events
        //
        layerChangeAdapter = new LayerChangeAdapter();
        selectionChangeAdapter = new SelectionChangeAdapter();
        MapView.addLayerChangeListener(layerChangeAdapter);
        DataSet.addSelectionListener(selectionChangeAdapter);
        initEnabledState();
    }

    /**
     * Override in subclasses to init the enabled state of an action when it is
     * created. Default behaviour is to call {@see #updateEnabledState()}
     *
     * @see #updateEnabledState()
     * @see #updateEnabledState(Collection)
     */
    protected void initEnabledState() {
        updateEnabledState();
    }

    /**
     * Override in subclasses to update the enabled state of the action when
     * something in the JOSM state changes, i.e. when a layer is removed or added.
     *
     * See {@see #updateEnabledState(Collection)} to respond to changes in the collection
     * of selected primitives.
     *
     * Default behavior is empty.
     *
     * @see #updateEnabledState(Collection)
     * @see #initEnabledState()
     */
    protected void updateEnabledState() {
    }

    /**
     * Override in subclasses to update the enabled state of the action if the
     * collection of selected primitives changes. This method is called with the
     * new selection.
     *
     * @param selection the collection of selected primitives; may be empty, but not null
     *
     * @see #updateEnabledState()
     * @see #initEnabledState()
     */
    protected void updateEnabledState(Collection<? extends OsmPrimitive> selection) {
    }

    /**
     * Adapter for layer change events
     *
     */
    private class LayerChangeAdapter implements MapView.LayerChangeListener {
        public void activeLayerChange(Layer oldLayer, Layer newLayer) {
            updateEnabledState();
        }

        public void layerAdded(Layer newLayer) {
            updateEnabledState();
        }

        public void layerRemoved(Layer oldLayer) {
            updateEnabledState();
        }
    }

    /**
     * Adapter for selection change events
     *
     */
    private class SelectionChangeAdapter implements SelectionChangedListener {
        public void selectionChanged(Collection<? extends OsmPrimitive> newSelection) {
            updateEnabledState(newSelection);
        }
    }
}
