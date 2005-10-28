package org.openstreetmap.josm.actions;

import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

import javax.swing.AbstractAction;
import javax.swing.KeyStroke;

import org.openstreetmap.josm.gui.ImageProvider;
import org.openstreetmap.josm.gui.PreferenceDialog;

/**
 * Open the Preferences dialog.
 *
 * @author imi
 */
public class PreferencesAction extends AbstractAction {

	/**
	 * Create the preference action with "&Preferences" as label.
	 */
	public PreferencesAction() {
		super("Preferences", ImageProvider.get("preference"));
		putValue(ACCELERATOR_KEY, KeyStroke.getAWTKeyStroke(KeyEvent.VK_P, InputEvent.CTRL_DOWN_MASK));
		putValue(MNEMONIC_KEY, KeyEvent.VK_P);
		putValue(SHORT_DESCRIPTION, "Open a preferences page for global settings.");
	}

	/**
	 * Launch the preferences dialog.
	 */
	public void actionPerformed(ActionEvent e) {
		PreferenceDialog dlg = new PreferenceDialog();
		dlg.setVisible(true);
	}
}
