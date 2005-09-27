package org.openstreetmap.josm.actions;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.File;

import javax.swing.AbstractAction;
import javax.swing.ImageIcon;
import javax.swing.JFileChooser;

import org.openstreetmap.josm.gui.Main;

/**
 * Export the current selected window's DataSet as gpx values. Remember, that some 
 * information could be lost. If so, an information message will ask the user to proceed.
 * (Means, if all information can be saved, no warning message appears).
 *  
 * @author imi
 */
public class SaveGpxAction extends AbstractAction {

	/**
	 * Construct the action with "Save GPX" as label.
	 */
	public SaveGpxAction() {
		super("Save GPX", new ImageIcon("images/savegpx.png"));
		putValue(MNEMONIC_KEY, KeyEvent.VK_S);
	}
	
	@SuppressWarnings("unchecked")
	public void actionPerformed(ActionEvent e) {
		JFileChooser fc = new JFileChooser("data");
		fc.showSaveDialog(Main.main);
		File gpxFile = fc.getSelectedFile();
		if (gpxFile == null)
			return;
	}

}
