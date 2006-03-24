package org.openstreetmap.josm.actions;

import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

import javax.swing.KeyStroke;

import org.openstreetmap.josm.Main;


/**
 * Undoes the last command.
 * 
 * @author imi
 */
public class UndoAction extends JosmAction {

	/**
	 * Construct the action with "Undo" as label.
	 */
	public UndoAction() {
		super("Undo", "undo", "Undo the last action.", "Ctrl-Z", KeyStroke.getKeyStroke(KeyEvent.VK_Z, InputEvent.CTRL_DOWN_MASK));
		setEnabled(false);
	}

	public void actionPerformed(ActionEvent e) {
		if (Main.main.getMapFrame() == null)
			return;
		Main.main.getMapFrame().repaint();
		Main.main.getMapFrame().mapView.editLayer().undo();
	}
}
