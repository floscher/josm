package org.openstreetmap.josm.actions;

import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

import javax.swing.KeyStroke;

import org.openstreetmap.josm.Main;


/**
 * Redoes the last command.
 * 
 * @author imi
 */
public class RedoAction extends JosmAction {

	/**
	 * Construct the action with "Undo" as label.
	 */
	public RedoAction() {
		super("Redo", "redo", "Redo the last undone action.", null, KeyStroke.getAWTKeyStroke(KeyEvent.VK_Z, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK));
		setEnabled(false);
	}

	public void actionPerformed(ActionEvent e) {
		if (Main.main.getMapFrame() == null)
			return;
		Main.main.getMapFrame().repaint();
		Main.main.getMapFrame().mapView.editLayer().redo();
	}
}
