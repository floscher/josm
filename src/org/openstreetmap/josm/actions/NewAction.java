package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

import org.openstreetmap.josm.Main;

public class NewAction extends JosmAction {

	public NewAction() {
		super(tr("New"), "new", tr("Create a new map."), KeyEvent.VK_N, InputEvent.CTRL_DOWN_MASK, true);
	}

	public void actionPerformed(ActionEvent e) {
		if (Main.breakBecauseUnsavedChanges())
			return;
		if (Main.map != null)
			Main.main.removeLayer(Main.main.editLayer());
		Main.main.editLayer(); // create new if empty
	}
}
