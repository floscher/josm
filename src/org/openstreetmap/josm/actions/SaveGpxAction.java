package org.openstreetmap.josm.actions;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import javax.swing.AbstractAction;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.gui.ImageProvider;
import org.openstreetmap.josm.io.GpxWriter;

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
		super("Save GPX", ImageProvider.get("savegpx"));
		putValue(MNEMONIC_KEY, KeyEvent.VK_S);
		putValue(SHORT_DESCRIPTION, "Save the current active layer as GPX file.");
	}
	
	@SuppressWarnings("unchecked")
	public void actionPerformed(ActionEvent event) {
		if (Main.main.getMapFrame() == null) {
			JOptionPane.showMessageDialog(Main.main, "No document open so nothing to save.");
			return;
		}
		JFileChooser fc = new JFileChooser("data");
		fc.showSaveDialog(Main.main);
		File gpxFile = fc.getSelectedFile();
		if (gpxFile == null)
			return;
		
		try {
			FileWriter fileWriter = new FileWriter(gpxFile);
			GpxWriter out = new GpxWriter(fileWriter);
			out.output();
			fileWriter.close();
		} catch (IOException e) {
			e.printStackTrace();
			JOptionPane.showMessageDialog(Main.main, "An error occoured while saving.\n"+e.getMessage());
		}
	}

}
