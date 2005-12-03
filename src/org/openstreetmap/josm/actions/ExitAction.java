package org.openstreetmap.josm.actions;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

/**
 * Exit the application. May ask for permition first (if something has changed).
 *  
 * @author imi
 */
public class ExitAction extends JosmAction {

	/**
	 * Construct the action with "Exit" as label
	 */
	public ExitAction() {
		super("Exit", "exit", "Exit the application.", KeyEvent.VK_X, null);
	}
	
	public void actionPerformed(ActionEvent e) {
		// todo: check for modified windows before exiting
		System.exit(0);
	}

}
