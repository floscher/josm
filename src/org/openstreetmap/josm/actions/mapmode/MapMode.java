package org.openstreetmap.josm.actions.mapmode;

import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

import javax.swing.KeyStroke;

import org.openstreetmap.josm.actions.JosmAction;
import org.openstreetmap.josm.gui.MapFrame;
import org.openstreetmap.josm.gui.MapView;

/**
 * A class implementing MapMode is able to be selected as an mode for map editing.
 * As example scrolling the map is a MapMode, connecting Nodes to new Segments
 * is another.
 * 
 * MapModes should register/deregister all necessary listener on the map's view
 * control. 
 */
abstract public class MapMode extends JosmAction implements MouseListener, MouseMotionListener {

	/**
	 * The parent mapframe this mode belongs to.
	 */
	protected final MapFrame mapFrame;
	/**
	 * Shortcut to the MapView.
	 */
	protected final MapView mv;

	/**
	 * Construct a mapMode with the given icon and the given MapFrame
	 */
	public MapMode(String name, String iconName, String tooltip, String keyname, int keystroke, MapFrame mapFrame) {
		super(name, "mapmode/"+iconName, tooltip, keyname, KeyStroke.getKeyStroke(keystroke, 0));
		this.mapFrame = mapFrame;
		mv = mapFrame.mapView;
	}

	/**
	 * Register all listener to the mapView
	 * @param mapView	The view, where the listener should be registered.
	 */
	public void registerListener() {
		firePropertyChange("active", false, true);
	}
	
	/**
	 * Unregister all listener previously registered. 
	 * @param mapView	The view from which the listener should be deregistered.
	 */
	public void unregisterListener() {
		firePropertyChange("active", true, false);
	}

	/**
	 * Call selectMapMode(this) on the parent mapFrame.
	 */
	public void actionPerformed(ActionEvent e) {
		mapFrame.selectMapMode(this);
	}

	public void mouseReleased(MouseEvent e) {}
	public void mouseExited(MouseEvent e) {}
	public void mousePressed(MouseEvent e) {}
	public void mouseClicked(MouseEvent e) {}
	public void mouseEntered(MouseEvent e) {}
	public void mouseMoved(MouseEvent e) {}
	public void mouseDragged(MouseEvent e) {}
}
