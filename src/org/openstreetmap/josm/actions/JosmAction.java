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

	public JosmAction(String name, String iconName, String tooltip, int shortCut, int modifier, boolean register) {
		super(name, ImageProvider.get(iconName));
		setHelpId();
		putValue(SHORT_DESCRIPTION, "<html>"+tooltip+" <font size='-2'>"+ShortCutLabel.name(shortCut, modifier)+"</font>&nbsp;</html>");
        Main.contentPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(shortCut, modifier), name);
        Main.contentPane.getActionMap().put(name, this);
        putValue("toolbar", iconName);
        if (register)
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
