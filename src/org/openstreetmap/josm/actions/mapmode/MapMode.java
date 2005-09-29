package org.openstreetmap.josm.actions.mapmode;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.ImageIcon;

import org.openstreetmap.josm.gui.MapFrame;
import org.openstreetmap.josm.gui.MapView;

/**
 * A class implementing MapMode is able to be selected as an mode for map editing.
 * As example scrolling the map is a MapMode, connecting Nodes to new LineSegments
 * is another.
 * 
 * MapModes should register/deregister all necessary listener on the map's view
 * control. 
 */
abstract public class MapMode extends AbstractAction {

	/**
	 * The parent mapframe this mode belongs to.
	 */
	protected final MapFrame mapFrame;

	/**
	 * Construct a mapMode with the given icon and the given MapFrame
	 *
	 * @param iconName The filename of the icon.
	 * @param mapFrame The parent MapFrame, this MapMode belongs to.
	 */
	public MapMode(String name, String iconName, String tooltip, int mnemonic, MapFrame mapFrame) {
		super(name, new ImageIcon("images/"+iconName+".png"));
		putValue(MNEMONIC_KEY, mnemonic);
		putValue(LONG_DESCRIPTION, tooltip);
		this.mapFrame = mapFrame;
	}
	
	/**
	 * Register all listener to the mapView
	 * @param mapView	The view, where the listener should be registered.
	 */
	abstract public void registerListener(MapView mapView);
	
	/**
	 * Unregister all listener previously registered. 
	 * @param mapView	The view from which the listener should be deregistered.
	 */
	abstract public void unregisterListener(MapView mapView);

	/**
	 * Call selectMapMode(this) on the parent mapFrame.
	 */
	public void actionPerformed(ActionEvent e) {
		mapFrame.selectMapMode(this);
	}
}
