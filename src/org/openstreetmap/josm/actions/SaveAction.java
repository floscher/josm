package org.openstreetmap.josm.actions;

import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.KeyStroke;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.io.GpxWriter;
import org.openstreetmap.josm.io.OsmWriter;

/**
 * Export the data  as OSM intern xml file.
 * 
 * TODO: This is very redundant with SaveGpxAction. Merge both actions into one!
 *  
 * @author imi
 */
public class SaveAction extends JosmAction {

	/**
	 * Construct the action with "Save" as label.
	 */
	public SaveAction() {
		super("Save", "save", "Save the current data.", null, KeyStroke.getAWTKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK));
	}
	
	public void actionPerformed(ActionEvent event) {
		if (Main.main.getMapFrame() == null) {
			JOptionPane.showMessageDialog(Main.main, "No document open so nothing to save.");
			return;
		}
		JFileChooser fc = new JFileChooser("data");
		for (int i = 0; i < ExtensionFileFilter.filters.length; ++i)
			fc.addChoosableFileFilter(ExtensionFileFilter.filters[i]);
		fc.setAcceptAllFileFilterUsed(true);
		fc.showSaveDialog(Main.main);
		File file = fc.getSelectedFile();
		if (file == null)
			return;
		
		try {
			FileWriter fileWriter = new FileWriter(file);
			String fn = file.getName();
			if (fn.endsWith(".gpx"))
				new GpxWriter(fileWriter).output();
			else if (fn.endsWith(".xml") || fn.endsWith(".osm"))
				new OsmWriter(fileWriter, Main.main.ds).output();
			else if (fn.endsWith(".txt") || fn.endsWith(".csv")) {
				JOptionPane.showMessageDialog(Main.main, "CSV output not supported yet.");
				return;
			} else {
				JOptionPane.showMessageDialog(Main.main, "Unknown file extension.");
				return;
			}
				
			fileWriter.close();
		} catch (IOException e) {
			e.printStackTrace();
			JOptionPane.showMessageDialog(Main.main, "An error occoured while saving.\n"+e.getMessage());
		}
	}

}
