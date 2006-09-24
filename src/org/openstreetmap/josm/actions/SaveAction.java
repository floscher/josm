package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import javax.swing.JOptionPane;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
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
		super(tr("Save"), "save", tr("Save the current data."), KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK);
	}
	
	public void actionPerformed(ActionEvent event) {
		if (!checkSaveConditions())
			return;

		File file = Main.main.editLayer().associatedFile;
		if (file == null)
			file = SaveAsAction.openFileDialog();
		if (file == null)
			return;

		save(file);
		Main.main.editLayer().name = file.getName();
		Main.main.editLayer().associatedFile = file;
		Main.parent.repaint();
	}

	public static void save(File file) {
	    try {
			OsmDataLayer layer = Main.main.editLayer();
			if (ExtensionFileFilter.filters[ExtensionFileFilter.GPX].acceptName(file.getPath())) {
				GpxExportAction.exportGpx(file, layer);
			} else if (ExtensionFileFilter.filters[ExtensionFileFilter.OSM].acceptName(file.getPath())) {
				OsmWriter.output(new FileOutputStream(file), Main.ds, false);
			} else if (ExtensionFileFilter.filters[ExtensionFileFilter.CSV].acceptName(file.getPath())) {
				JOptionPane.showMessageDialog(Main.parent, tr("CSV output not supported yet."));
				return;
			} else {
				JOptionPane.showMessageDialog(Main.parent, tr("Unknown file extension."));
				return;
			}
			layer.cleanData(null, false);
		} catch (IOException e) {
			e.printStackTrace();
			JOptionPane.showMessageDialog(Main.parent, tr("An error occoured while saving.")+"\n"+e.getMessage());
		}
    }
}
