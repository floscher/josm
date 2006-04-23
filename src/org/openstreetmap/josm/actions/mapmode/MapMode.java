package org.openstreetmap.josm.actions.mapmode;

import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

import javax.swing.KeyStroke;

import org.openstreetmap.josm.actions.JosmAction;
import org.openstreetmap.josm.gui.MapFrame;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.tools.ImageProvider;

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
	 * Constructor for mapmodes without an menu
	 */
	public MapMode(String name, String iconName, String tooltip, String keyname, int keystroke, MapFrame mapFrame) {
		super(name, "mapmode/"+iconName, tooltip, keyname, KeyStroke.getKeyStroke(keystroke, 0));
		this.mapFrame = mapFrame;
		mv = mapFrame.mapView;
		putValue("active", false);
	}

	/**
	 * Constructor for mapmodes with an menu (no shortcut will be registered)
	 */
	public MapMode(String name, String iconName, String tooltip, MapFrame mapFrame) {
		putValue(NAME, name);
		putValue(SMALL_ICON, ImageProvider.get("mapmode", iconName));
		putValue(SHORT_DESCRIPTION, tooltip);
		this.mapFrame = mapFrame;
		mv = mapFrame.mapView;
	}

	public void enterMode() {
		putValue("active", true);
	}
	public void exitMode() {
		putValue("active", false);
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
