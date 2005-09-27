package org.openstreetmap.josm.actions;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.AbstractAction;
import javax.swing.ImageIcon;

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
		super("Preferences", new ImageIcon("images/preference.png"));
		putValue(MNEMONIC_KEY, KeyEvent.VK_P);
	}

	/**
	 * Launch the preferences dialog.
	 */
	public void actionPerformed(ActionEvent e) {
		PreferenceDialog dlg = new PreferenceDialog();
		dlg.setVisible(true);
	}
}
