package org.openstreetmap.josm.actions;

import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import javax.swing.AbstractAction;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.KeyStroke;
import javax.swing.filechooser.FileFilter;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.gui.ImageProvider;
import org.openstreetmap.josm.io.OsmWriter;

/**
 * Export the data  as OSM intern xml file.
 * 
 * TODO: This is very redundant with SaveGpxAction. Merge both actions into one!
 *  
 * @author imi
 */
public class SaveAction extends AbstractAction {

	/**
	 * Construct the action with "Save" as label.
	 */
	public SaveAction() {
		super("Save", ImageProvider.get("save"));
		putValue(ACCELERATOR_KEY, KeyStroke.getAWTKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK));
		putValue(SHORT_DESCRIPTION, "Save the current data.");
	}
	
	public void actionPerformed(ActionEvent event) {
		if (Main.main.getMapFrame() == null) {
			JOptionPane.showMessageDialog(Main.main, "No document open so nothing to save.");
			return;
		}
		JFileChooser fc = new JFileChooser("data");
		fc.setFileFilter(new FileFilter(){
			@Override
			public boolean accept(File f) {
				String name = f.getName().toLowerCase();
				return f.isDirectory() || name.endsWith(".xml");
			}
			@Override
			public String getDescription() {
				return "XML Files";
			}
		});
		fc.showSaveDialog(Main.main);
		File file = fc.getSelectedFile();
		if (file == null)
			return;
		
		try {
			FileWriter fileWriter = new FileWriter(file);
			OsmWriter out = new OsmWriter(fileWriter, Main.main.ds);
			out.output();
			fileWriter.close();
		} catch (IOException e) {
			e.printStackTrace();
			JOptionPane.showMessageDialog(Main.main, "An error occoured while saving.\n"+e.getMessage());
		}
	}

}
