package org.openstreetmap.josm.gui.dialogs;

import java.awt.event.KeyEvent;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JLabel;
import javax.swing.border.Border;

import org.openstreetmap.josm.gui.Main;
import org.openstreetmap.josm.gui.MapFrame;

/**
 * Open a Property dialog for the current visible map. When saving to own josm-
 * data format, the properties are saved along.
 * 
 * @author imi
 */
public class PropertiesDialog extends ToggleDialog {

	/**
	 * Create a new PropertiesDialog
	 */
	public PropertiesDialog(MapFrame mapFrame) {
		super(mapFrame, "Properties of "+Main.main.getNameOfLoadedMapFrame(), "Properties Dialog", "properties", KeyEvent.VK_P, "Property page for this map.");
		putValue(MNEMONIC_KEY, KeyEvent.VK_P);

		final Border panelBorder = BorderFactory.createEmptyBorder(5,0,0,0);
		Box panel = Box.createVerticalBox();
		
		JLabel todo = new JLabel("Nothing implemented yet.");
		todo.setBorder(panelBorder);
		panel.add(todo);
		
		panel.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
		setContentPane(panel);
		pack();
		setResizable(false);
	}
}