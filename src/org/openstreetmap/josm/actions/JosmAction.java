package org.openstreetmap.josm.actions;


import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.KeyStroke;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.ShortCutLabel;

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
		putValue("toolbar", iconName);
		Main.toolbar.register(this);
	}


	public JosmAction(String name, String iconName, String tooltip, int shortCut, int modifier) {
		super(name, ImageProvider.get(iconName));
		setHelpId();
		putValue(SHORT_DESCRIPTION, "<html>"+tooltip+" <font size='-2'>"+ShortCutLabel.name(shortCut, modifier)+"</font>&nbsp;</html>");
        Main.contentPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(shortCut, modifier), name);
        Main.contentPane.getActionMap().put(name, this);
        putValue("toolbar", iconName);
        Main.toolbar.register(this);
	}

	public JosmAction() {
		setHelpId();
	}


	private void setHelpId() {
		String helpId = "Action/"+getClass().getName().substring(getClass().getName().lastIndexOf('.')+1);
		if (helpId.endsWith("Action"))
			helpId = helpId.substring(0, helpId.length()-6);
		putValue("help", helpId);
	}
}
