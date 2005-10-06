package org.openstreetmap.josm.actions;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.AbstractAction;
import javax.swing.ImageIcon;

import org.openstreetmap.josm.gui.Layer;
import org.openstreetmap.josm.gui.Main;

/**
 * Toggles the autoScale feature of the layer
 * @author imi
 */
public class AutoScaleAction extends AbstractAction {
	/**
	 * The Layer, that belongs to this AutoScaleSction.
	 */
	private final Layer layer;
	
	public AutoScaleAction(Layer layer) {
		super("Auto Scale", new ImageIcon(Main.class.getResource("/images/autoscale.png")));
		this.layer = layer;
		putValue(MNEMONIC_KEY, KeyEvent.VK_A);
	}
	public void actionPerformed(ActionEvent e) {
		layer.setAutoScale(!layer.isAutoScale());
	}
}