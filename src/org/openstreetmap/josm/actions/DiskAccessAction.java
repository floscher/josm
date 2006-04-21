package org.openstreetmap.josm.actions;

import java.io.File;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.KeyStroke;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.osm.OsmPrimitive;

/**
 * Helper class for all actions, that access the disk
 */
abstract public class DiskAccessAction extends JosmAction {

	public DiskAccessAction(String name, String iconName, String tooltip, String shortCutName, KeyStroke shortCut) {
		super(name, iconName, tooltip, shortCutName, shortCut);
	}
	
	
	/**
	 * Check the data set if it would be empty on save. It is empty, if it contains
	 * no objects (after all objects that are created and deleted without beeing 
	 * transfered to the server have been removed).
	 *  
	 * @return <code>true</code>, if a save result in an empty data set.
	 */
	protected boolean isDataSetEmpty() {
		for (OsmPrimitive osm : Main.ds.allNonDeletedPrimitives())
			if (!osm.deleted || osm.id > 0)
				return false;
		return true;
	}
	
	protected JFileChooser createAndOpenFileChooser(boolean open, boolean multiple) {
		String curDir = Main.pref.get("lastDirectory");
		if (curDir.equals(""))
			curDir = ".";
		JFileChooser fc = new JFileChooser(new File(curDir));
		fc.setMultiSelectionEnabled(multiple);
		for (int i = 0; i < ExtensionFileFilter.filters.length; ++i)
			fc.addChoosableFileFilter(ExtensionFileFilter.filters[i]);
		fc.setAcceptAllFileFilterUsed(true);
	
		int answer = open ? fc.showOpenDialog(Main.main) : fc.showSaveDialog(Main.main);
		if (answer != JFileChooser.APPROVE_OPTION)
			return null;
		
		if (!fc.getCurrentDirectory().getAbsolutePath().equals(curDir))
			Main.pref.put("lastDirectory", fc.getCurrentDirectory().getAbsolutePath());

		if (!open) {
			File file = fc.getSelectedFile();
			if (file == null || (file.exists() && JOptionPane.YES_OPTION != 
					JOptionPane.showConfirmDialog(Main.main, "File exists. Overwrite?", "Overwrite", JOptionPane.YES_NO_OPTION)))
				return null;
		}
		
		return fc;
	}}
