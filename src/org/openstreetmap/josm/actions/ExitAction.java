package org.openstreetmap.josm.actions;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.AbstractAction;
import javax.swing.ImageIcon;

/**
 * Exit the application. May ask for permition first (if something has changed).
 *  
 * @author imi
 */
public class ExitAction extends AbstractAction {

	/**
	 * Construct the action with "Exit" as label
	 */
	public ExitAction() {
		super("Exit", new ImageIcon("images/exit.png"));
		putValue(MNEMONIC_KEY, KeyEvent.VK_X);
	}
	
	public void actionPerformed(ActionEvent e) {
		// todo: check for modified windows before exiting
		System.exit(0);
	}

}
