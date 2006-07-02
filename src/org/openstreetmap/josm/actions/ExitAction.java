package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.tools.I18n.tr;

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
		super(tr("Exit"), "exit", tr("Exit the application."), KeyEvent.VK_X);
	}
	
	public void actionPerformed(ActionEvent e) {
		System.exit(0);
	}
}
