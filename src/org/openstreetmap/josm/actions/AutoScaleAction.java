package org.openstreetmap.josm.actions;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import org.openstreetmap.josm.gui.MapFrame;
import org.openstreetmap.josm.gui.MapView;

/**
 * Toggles the autoScale feature of the mapView
 * @author imi
 */
public class AutoScaleAction extends JosmAction {
	/**
	 * The mapView this action operates on.
	 */
	private final MapView mapView;
	
	public AutoScaleAction(MapFrame mapFrame) {
		super("Auto Scale", "autoscale", "Zoom the view to show the whole layer. Disabled if the view is moved.",
				KeyEvent.VK_A, null);
		mapView = mapFrame.mapView;
	}


	public void actionPerformed(ActionEvent e) {
		mapView.setAutoScale(!mapView.isAutoScale());
	}
}