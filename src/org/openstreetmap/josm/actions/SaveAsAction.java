package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.File;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;

import org.openstreetmap.josm.Main;

/**
 * Export the data  as OSM intern xml file.
 * 
 * @author imi
 */
public class SaveAsAction extends DiskAccessAction {
    
	/**
	 * Construct the action with "Save" as label.
	 * @param layer Save only this layer. If <code>null</code>, save the whole Main 
	 * 		data set.
	 */
	public SaveAsAction() {
		super(tr("Save as"), "save_as", tr("Save the current data to a new file."), KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK);
	}
	
	public void actionPerformed(ActionEvent event) {
		if (!checkSaveConditions())
			return;

		File file = openFileDialog();
		if (file == null)
			return;

		SaveAction.save(file);
		Main.main.editLayer().name = file.getName();
		Main.main.editLayer().associatedFile = file;
		Main.parent.repaint();
	}

	public static File openFileDialog() {
	    JFileChooser fc = createAndOpenFileChooser(false, false);
		if (fc == null)
			return null;

		File file = fc.getSelectedFile();

		String fn = file.getPath();
		if (fn.indexOf('.') == -1) {
			FileFilter ff = fc.getFileFilter();
			if (ff instanceof ExtensionFileFilter)
				fn = "." + ((ExtensionFileFilter)ff).defaultExtension;
			else
				fn += ".osm";
			file = new File(fn);
		}
	    return file;
    }
}
