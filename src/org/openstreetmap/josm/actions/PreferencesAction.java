package org.openstreetmap.josm.actions;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.KeyStroke;

import org.openstreetmap.josm.gui.PreferenceDialog;

/**
 * Open the Preferences dialog.
 *
 * @author imi
 */
public class PreferencesAction extends JosmAction {

	/**
	 * Create the preference action with "&Preferences" as label.
	 */
	public PreferencesAction() {
		super("Preferences", "preference", "Open a preferences page for global settings.", "F12", KeyStroke.getKeyStroke(KeyEvent.VK_F12, 0));
	}

	/**
	 * Launch the preferences dialog.
	 */
	public void actionPerformed(ActionEvent e) {
		PreferenceDialog dlg = new PreferenceDialog();
		dlg.setVisible(true);
	}
}
