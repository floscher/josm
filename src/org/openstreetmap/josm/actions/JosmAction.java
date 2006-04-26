package org.openstreetmap.josm.actions;


import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.KeyStroke;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.tools.ImageProvider;

/**
 * Base class helper for all Actions in JOSM. Just to make the life easier.
 * @author imi
 */
abstract public class JosmAction extends AbstractAction {

	/**
	 * Construct the action as menu action entry.
	 * 
	 * @param name		Name of the action (entry name in menu)
	 * @param iconName	Name of the icon (without extension)
	 * @param tooltip	Short tooltip description
	 * @param mnemonic	Mnemonic in the menu
	 */
	public JosmAction(String name, String iconName, String tooltip, int mnemonic) {
		super(name, ImageProvider.get(iconName));
		putValue(SHORT_DESCRIPTION, tooltip);
		putValue(MNEMONIC_KEY, mnemonic);
	}


	public JosmAction(String name, String iconName, String tooltip, String shortCutName, KeyStroke shortCut) {
		super(name, ImageProvider.get(iconName));
		putValue(SHORT_DESCRIPTION, "<html>"+tooltip+" <font size='-2'>"+shortCutName+"</font>&nbsp;</html>");
		//Main.panel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(shortCut, name);
        //Main.panel.getActionMap().put(name, this);
        Main.contentPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(shortCut, name);
        Main.contentPane.getActionMap().put(name, this);
	}

	public JosmAction() {
	}
}
