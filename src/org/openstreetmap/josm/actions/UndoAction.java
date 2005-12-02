package org.openstreetmap.josm.actions;

import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.LinkedList;

import javax.swing.AbstractAction;
import javax.swing.KeyStroke;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.gui.ImageProvider;


/**
 * Undoes the last command.
 * 
 * @author imi
 */
public class UndoAction extends AbstractAction {

	/**
	 * Construct the action with "Undo" as label.
	 */
	public UndoAction() {
		super("Undo", ImageProvider.get("undo"));
		putValue(ACCELERATOR_KEY, KeyStroke.getAWTKeyStroke(KeyEvent.VK_Z, InputEvent.CTRL_DOWN_MASK));
		putValue(SHORT_DESCRIPTION, "Undo the last action.");
	}
	
	public void actionPerformed(ActionEvent e) {
		if (Main.main.getMapFrame() == null)
			return;
		LinkedList<Command> commands = Main.main.getMapFrame().mapView.editLayer().commands;
		if (commands.isEmpty())
			return;
		Command c = commands.getLast();
		//c.undoCommand();
		commands.removeLast();
		Main.main.getMapFrame().repaint();
	}
}
