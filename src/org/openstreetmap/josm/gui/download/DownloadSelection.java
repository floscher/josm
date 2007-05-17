package org.openstreetmap.josm.gui.download;

import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.gui.preferences.PreferenceDialog;

public interface DownloadSelection {
	/**
	 * Add the GUI elements to the dialog. 
	 */
	void addGui(DownloadDialog gui);

	/** 
	 * Update or clear display when a selection is made through another
	 * DownloadSelection object
	 */
	void boundingBoxChanged(DownloadDialog gui);
	
}
