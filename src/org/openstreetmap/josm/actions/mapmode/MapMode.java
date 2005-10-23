package org.openstreetmap.josm.actions.mapmode;

import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.KeyStroke;

import org.openstreetmap.josm.gui.ImageProvider;
import org.openstreetmap.josm.gui.MapFrame;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.gui.MapView.LayerChangeListener;
import org.openstreetmap.josm.gui.layer.Layer;

/**
 * A class implementing MapMode is able to be selected as an mode for map editing.
 * As example scrolling the map is a MapMode, connecting Nodes to new LineSegments
 * is another.
 * 
 * MapModes should register/deregister all necessary listener on the map's view
 * control. 
 */
abstract public class MapMode extends AbstractAction implements MouseListener, MouseMotionListener {

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
	 *
	 * @param iconName The filename of the icon.
	 * @param mapFrame The parent MapFrame, this MapMode belongs to.
	 */
	public MapMode(String name, String iconName, String tooltip, int mnemonic, MapFrame mapFrame) {
		super(name, ImageProvider.get("mapmode", iconName));
		putValue(MNEMONIC_KEY, mnemonic);
		putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(mnemonic,0));
		putValue(LONG_DESCRIPTION, tooltip);
		mapFrame.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(mnemonic,0), this);
		mapFrame.getActionMap().put(this, this);
		this.mapFrame = mapFrame;
		mv = mapFrame.mapView;
		mv.addLayerChangeListener(new LayerChangeListener(){
			public void activeLayerChange(Layer oldLayer, Layer newLayer) {
				setEnabled(!isEditMode() || newLayer.isEditable());
			}
			public void layerAdded(Layer newLayer) {}
			public void layerRemoved(Layer oldLayer) {}
		});
	}

	/**
	 * Subclasses should return whether they want to edit the map data or
	 * whether they are read-only.
	 */
	abstract protected boolean isEditMode();

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

	/**
	 * Does nothing. Only to subclass.
	 */
	public void mouseClicked(MouseEvent e) {}
	/**
	 * Does nothing. Only to subclass.
	 */
	public void mousePressed(MouseEvent e) {}
	/**
	 * Does nothing. Only to subclass.
	 */
	public void mouseReleased(MouseEvent e) {}
	/**
	 * Does nothing. Only to subclass.
	 */
	public void mouseEntered(MouseEvent e) {}
	/**
	 * Does nothing. Only to subclass.
	 */
	public void mouseExited(MouseEvent e) {}
	/**
	 * Does nothing. Only to subclass.
	 */
	public void mouseMoved(MouseEvent e) {}
	/**
	 * Does nothing. Only to subclass.
	 */
	public void mouseDragged(MouseEvent e) {}
}
