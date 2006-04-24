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
import javax.swing.filechooser.FileFilter;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.osm.Segment;
import org.openstreetmap.josm.io.GpxWriter;
import org.openstreetmap.josm.io.OsmWriter;

/**
 * Export the data  as OSM intern xml file.
 * 
 * @author imi
 */
public class SaveAction extends DiskAccessAction {

	/**
	 * Construct the action with "Save" as label.
	 * @param layer Save only this layer. If <code>null</code>, save the whole Main 
	 * 		data set.
	 */
	public SaveAction() {
		super("Save", "save", "Save the current data.", "Ctrl-S", KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK));
	}
	
	public void actionPerformed(ActionEvent event) {
		if (Main.map == null) {
			JOptionPane.showMessageDialog(Main.parent, "No document open so nothing to save.");
			return;
		}
		if (isDataSetEmpty() && JOptionPane.NO_OPTION == JOptionPane.showConfirmDialog(Main.parent, "The document contains no data. Save anyway?", "Empty document", JOptionPane.YES_NO_OPTION))
			return;
		if (!Main.map.conflictDialog.conflicts.isEmpty()) {
			int answer = JOptionPane.showConfirmDialog(Main.parent, 
					"There are unresolved conflicts. Conflicts will not be saved and handled as if you rejected all. Continue?", "Conflicts", JOptionPane.YES_NO_OPTION);
			if (answer != JOptionPane.YES_OPTION)
				return;
		}

		JFileChooser fc = createAndOpenFileChooser(false, false);
		if (fc == null)
			return;

		File file = fc.getSelectedFile();

		try {
			String fn = file.getPath();
			if (fn.indexOf('.') == -1) {
				FileFilter ff = fc.getFileFilter();
				if (ff instanceof ExtensionFileFilter)
					fn = "." + ((ExtensionFileFilter)ff).defaultExtension;
				else
					fn += ".osm";
				file = new File(fn);
			}
			FileWriter fileWriter;
			if (ExtensionFileFilter.filters[ExtensionFileFilter.GPX].acceptName(fn)) {
				for (Segment ls : Main.ds.segments) {
					if (ls.incomplete) {
						JOptionPane.showMessageDialog(Main.parent, "Export of data containing incomplete ways to GPX is not implemented.\nBe aware, that in future versions of JOSM, GPX support will be kept at a minimum.\nPlease use .osm or .xml as extension for the better OSM support.");
						return;
					}
				}
				new GpxWriter(fileWriter = new FileWriter(file), Main.ds).output();
			} else if (ExtensionFileFilter.filters[ExtensionFileFilter.OSM].acceptName(fn))
				OsmWriter.output(fileWriter = new FileWriter(file), Main.ds, false);
			else if (ExtensionFileFilter.filters[ExtensionFileFilter.CSV].acceptName(fn)) {
				JOptionPane.showMessageDialog(Main.parent, "CSV output not supported yet.");
				return;
			} else {
				JOptionPane.showMessageDialog(Main.parent, "Unknown file extension.");
				return;
			}
			fileWriter.close();
			Main.main.editLayer().cleanData(null, false);
		} catch (IOException e) {
			e.printStackTrace();
			JOptionPane.showMessageDialog(Main.parent, "An error occoured while saving.\n"+e.getMessage());
		}
	}
}
