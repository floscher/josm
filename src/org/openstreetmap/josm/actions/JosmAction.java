package org.openstreetmap.josm.actions;

import java.awt.AWTKeyStroke;

import javax.swing.AbstractAction;

import org.openstreetmap.josm.gui.ImageProvider;

/**
 * Base class helper for all Actions in JOSM. Just to make the life easier.
 * @author imi
 */
abstract public class JosmAction extends AbstractAction {

	/**
	 * Construct the action.
	 * 
	 * @param name		Name of the action (entry name in menu)
	 * @param iconName	Name of the icon (without extension)
	 * @param desc		Short tooltip description
	 * @param mnemonic	If non-<code>null</code>, the Mnemonic in menu
	 * @param shortCut	If non-<code>null</code>, the shortcut keystroke
	 */
	public JosmAction(String name, String iconName, String desc, Integer mnemonic, AWTKeyStroke shortCut) {
		super(name, ImageProvider.get(iconName));
		putValue(SHORT_DESCRIPTION, desc);
		if (mnemonic != null)
			putValue(MNEMONIC_KEY, mnemonic);
		if (shortCut != null)
			putValue(ACCELERATOR_KEY, shortCut);
	}
}
