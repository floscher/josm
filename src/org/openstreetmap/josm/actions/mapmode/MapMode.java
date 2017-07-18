// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions.mapmode;

import java.awt.Cursor;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.Collection;
import java.util.Collections;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.JosmAction;
import org.openstreetmap.josm.data.Preferences.PreferenceChangeEvent;
import org.openstreetmap.josm.data.Preferences.PreferenceChangedListener;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.gui.MapFrame;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.Shortcut;

/**
 * A class implementing MapMode is able to be selected as an mode for map editing.
 * As example scrolling the map is a MapMode, connecting Nodes to new Ways is another.
 *
 * MapModes should register/deregister all necessary listeners on the map's view control.
 */
public abstract class MapMode extends JosmAction implements MouseListener, MouseMotionListener, PreferenceChangedListener {
    protected final Cursor cursor;
    protected boolean ctrl;
    protected boolean alt;
    protected boolean shift;

    /**
     * Constructor for mapmodes without a menu
     * @param name the action's text
     * @param iconName icon filename in {@code mapmode} directory
     * @param tooltip  a longer description of the action that will be displayed in the tooltip.
     * @param shortcut a ready-created shortcut object or null if you don't want a shortcut.
     * @param cursor cursor displayed when map mode is active
     * @since 11713
     */
    public MapMode(String name, String iconName, String tooltip, Shortcut shortcut, Cursor cursor) {
        super(name, "mapmode/"+iconName, tooltip, shortcut, false);
        this.cursor = cursor;
        putValue("active", Boolean.FALSE);
    }

    /**
     * Constructor for mapmodes with a menu (no shortcut will be registered)
     * @param name the action's text
     * @param iconName icon filename in {@code mapmode} directory
     * @param tooltip  a longer description of the action that will be displayed in the tooltip.
     * @param cursor cursor displayed when map mode is active
     * @since 11713
     */
    public MapMode(String name, String iconName, String tooltip, Cursor cursor) {
        putValue(NAME, name);
        new ImageProvider("mapmode", iconName).getResource().attachImageIcon(this);
        putValue(SHORT_DESCRIPTION, tooltip);
        this.cursor = cursor;
    }

    /**
     * Constructor for mapmodes without a menu
     * @param name the action's text
     * @param iconName icon filename in {@code mapmode} directory
     * @param tooltip  a longer description of the action that will be displayed in the tooltip.
     * @param shortcut a ready-created shortcut object or null if you don't want a shortcut.
     * @param mapFrame unused but kept for plugin compatibility. Can be {@code null}
     * @param cursor cursor displayed when map mode is active
     * @deprecated use {@link #MapMode(String, String, String, Shortcut, Cursor) instead}
     */
    @Deprecated
    public MapMode(String name, String iconName, String tooltip, Shortcut shortcut, MapFrame mapFrame, Cursor cursor) {
        this(name, iconName, tooltip, shortcut, cursor);
    }

    /**
     * Constructor for mapmodes with a menu (no shortcut will be registered)
     * @param name the action's text
     * @param iconName icon filename in {@code mapmode} directory
     * @param tooltip  a longer description of the action that will be displayed in the tooltip.
     * @param mapFrame unused but kept for plugin compatibility. Can be {@code null}
     * @param cursor cursor displayed when map mode is active
     * @deprecated use {@link #MapMode(String, String, String, Cursor) instead}
     */
    @Deprecated
    public MapMode(String name, String iconName, String tooltip, MapFrame mapFrame, Cursor cursor) {
        this(name, iconName, tooltip, cursor);
    }

    /**
     * Makes this map mode active.
     */
    public void enterMode() {
        putValue("active", Boolean.TRUE);
        Main.pref.addPreferenceChangeListener(this);
        readPreferences();
        Main.map.mapView.setNewCursor(cursor, this);
        updateStatusLine();
    }

    /**
     * Makes this map mode inactive.
     */
    public void exitMode() {
        putValue("active", Boolean.FALSE);
        Main.pref.removePreferenceChangeListener(this);
        Main.map.mapView.resetCursor(this);
    }

    protected void updateStatusLine() {
        if (Main.map != null && Main.map.statusLine != null) {
            Main.map.statusLine.setHelpText(getModeHelpText());
            Main.map.statusLine.repaint();
        }
    }

    /**
     * Returns a short translated help message describing how this map mode can be used, to be displayed in status line.
     * @return a short translated help message describing how this map mode can be used
     */
    public String getModeHelpText() {
        return "";
    }

    protected void readPreferences() {}

    /**
     * Call selectMapMode(this) on the parent mapFrame.
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        if (Main.isDisplayingMapView()) {
            Main.map.selectMapMode(this);
        }
    }

    /**
     * Determines if layer {@code l} is supported by this map mode.
     * By default, all tools will work with all layers.
     * Can be overwritten to require a special type of layer
     * @param l layer
     * @return {@code true} if the layer is supported by this map mode
     */
    public boolean layerIsSupported(Layer l) {
        return l != null;
    }

    protected void updateKeyModifiers(InputEvent e) {
        updateKeyModifiers(e.getModifiers());
    }

    protected void updateKeyModifiers(MouseEvent e) {
        updateKeyModifiers(e.getModifiers());
    }

    protected void updateKeyModifiers(int modifiers) {
        ctrl = (modifiers & ActionEvent.CTRL_MASK) != 0;
        alt = (modifiers & (ActionEvent.ALT_MASK | InputEvent.ALT_GRAPH_MASK)) != 0;
        shift = (modifiers & ActionEvent.SHIFT_MASK) != 0;
    }

    protected void requestFocusInMapView() {
        if (isEnabled()) {
            // request focus in order to enable the expected keyboard shortcuts (see #8710)
            Main.map.mapView.requestFocus();
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        requestFocusInMapView();
    }

    @Override
    public void mouseExited(MouseEvent e) {
        // Do nothing
    }

    @Override
    public void mousePressed(MouseEvent e) {
        requestFocusInMapView();
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        // Do nothing
    }

    @Override
    public void mouseEntered(MouseEvent e) {
        // Do nothing
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        // Do nothing
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        // Do nothing
    }

    @Override
    public void preferenceChanged(PreferenceChangeEvent e) {
        readPreferences();
    }

    /**
     * Gets a collection of primitives that should not be hidden by the filter.
     * @return The primitives that the filter should not hide.
     * @since 11993
     */
    public Collection<? extends OsmPrimitive> getPreservedPrimitives() {
        return Collections.emptySet();
    }
}
