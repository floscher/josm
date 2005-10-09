package org.openstreetmap.josm.actions;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.KeyStroke;

import org.openstreetmap.josm.gui.ImageProvider;
import org.openstreetmap.josm.gui.MapFrame;
import org.openstreetmap.josm.gui.MapView;

/**
 * Toggles the autoScale feature of the mapView
 * @author imi
 */
public class AutoScaleAction extends AbstractAction {
	/**
	 * The mapView this action operates on.
	 */
	private final MapView mapView;
	
	public AutoScaleAction(MapFrame mapFrame) {
		super("Auto Scale", ImageProvider.get("autoscale"));
		mapView = mapFrame.mapView;
		putValue(MNEMONIC_KEY, KeyEvent.VK_A);
		putValue(SHORT_DESCRIPTION, "Zoom the view to show the whole layer. Disabled if the view is moved.");
		KeyStroke ks = KeyStroke.getKeyStroke(KeyEvent.VK_A, 0);
		putValue(ACCELERATOR_KEY, ks);
		mapFrame.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(ks, this);
		mapFrame.getActionMap().put(this, this);
	}


	public void actionPerformed(ActionEvent e) {
		mapView.setAutoScale(!mapView.isAutoScale());
	}
}