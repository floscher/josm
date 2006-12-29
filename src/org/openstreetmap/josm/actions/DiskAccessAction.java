package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.File;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.osm.OsmPrimitive;

/**
 * Helper class for all actions, that access the disk
 */
abstract public class DiskAccessAction extends JosmAction {

	/**
	 * Checks whether it is ok to launch a save (whether we have data,
	 * there is no conflict etc...)
	 * @return <code>true</code>, if it is save to save.
	 */
	public boolean checkSaveConditions() {
        if (Main.map == null) {
    		JOptionPane.showMessageDialog(Main.parent, tr("No document open so nothing to save."));
    		return false;
    	}
    	if (isDataSetEmpty() && JOptionPane.NO_OPTION == JOptionPane.showConfirmDialog(Main.parent,tr("The document contains no data. Save anyway?"), tr("Empty document"), JOptionPane.YES_NO_OPTION))
    		return false;
    	if (!Main.map.conflictDialog.conflicts.isEmpty()) {
    		int answer = JOptionPane.showConfirmDialog(Main.parent, 
    				tr("There are unresolved conflicts. Conflicts will not be saved and handled as if you rejected all. Continue?"),tr("Conflicts"), JOptionPane.YES_NO_OPTION);
    		if (answer != JOptionPane.YES_OPTION)
    			return false;
    	}
    	return true;
    }


	public DiskAccessAction(String name, String iconName, String tooltip, int shortCut, int modifiers) {
		super(name, iconName, tooltip, shortCut, modifiers, true);
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
	
	protected static JFileChooser createAndOpenFileChooser(boolean open, boolean multiple) {
		String curDir = Main.pref.get("lastDirectory");
		if (curDir.equals(""))
			curDir = ".";
		JFileChooser fc = new JFileChooser(new File(curDir));
		fc.setMultiSelectionEnabled(multiple);
		for (int i = 0; i < ExtensionFileFilter.filters.length; ++i)
			fc.addChoosableFileFilter(ExtensionFileFilter.filters[i]);
		fc.setAcceptAllFileFilterUsed(true);
	
		int answer = open ? fc.showOpenDialog(Main.parent) : fc.showSaveDialog(Main.parent);
		if (answer != JFileChooser.APPROVE_OPTION)
			return null;
		
		if (!fc.getCurrentDirectory().getAbsolutePath().equals(curDir))
			Main.pref.put("lastDirectory", fc.getCurrentDirectory().getAbsolutePath());

		if (!open) {
			File file = fc.getSelectedFile();
			if (file == null || (file.exists() && JOptionPane.YES_OPTION != 
					JOptionPane.showConfirmDialog(Main.parent, tr("File exists. Overwrite?"), tr("Overwrite"), JOptionPane.YES_NO_OPTION)))
				return null;
		}
		
		return fc;
	}}
