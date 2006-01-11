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
import org.openstreetmap.josm.data.osm.OsmPrimitive;
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
		if (isDataSetEmpty() && JOptionPane.NO_OPTION == JOptionPane.showConfirmDialog(Main.main, "The document contains no data. Save anyway?", "Empty document", JOptionPane.YES_NO_OPTION))
			return;

		JFileChooser fc = new JFileChooser("data");
		for (int i = 0; i < ExtensionFileFilter.filters.length; ++i)
			fc.addChoosableFileFilter(ExtensionFileFilter.filters[i]);
		fc.setAcceptAllFileFilterUsed(true);
		fc.showSaveDialog(Main.main);
		File file = fc.getSelectedFile();
		if (file == null)
			return;

		try {
			String fn = file.getName();
			FileWriter fileWriter;
			if (fn.endsWith(".gpx"))
				new GpxWriter(fileWriter = new FileWriter(file)).output();
			else if (fn.endsWith(".xml") || fn.endsWith(".osm"))
				new OsmWriter(fileWriter = new FileWriter(file), Main.main.ds).output();
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

	/**
	 * Check the data set if it would be empty on save. It is empty, if it contains
	 * no objects (after all objects that are created and deleted without beeing 
	 * transfered to the server have been removed).
	 *  
	 * @return <code>true</code>, if a save result in an empty data set.
	 */
	private boolean isDataSetEmpty() {
		for (OsmPrimitive osm : Main.main.ds.allPrimitives())
			if (!osm.isDeleted() || osm.id > 0)
				return false;
		return true;
	}

}
