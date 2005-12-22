package org.openstreetmap.josm.actions;

import java.io.File;

import javax.swing.filechooser.FileFilter;

/**
 * A file filter that filters after the extension. Also includes a list of file 
 * filters used in JOSM.
 * 
 * @author imi
 */
public class ExtensionFileFilter extends FileFilter {
	
	private final String extension;
	private final String description;

	public static ExtensionFileFilter[] filters = {
		new ExtensionFileFilter(".xml", "OSM Server Version 0.2 (.xml)"),
		new ExtensionFileFilter(".gpx", "GPX Files Version 0.1 (.gpx)"),
		//new ExtensionFileFilter(".josm", "JOSM Savefiles (.josm)")
	};

	/**
	 * Construct an extension file filter by giving the extension to check after.
	 *
	 */
	private ExtensionFileFilter(String extension, String description) {
		this.extension = extension;
		this.description = description;
	}

	@Override
	public boolean accept(File pathname) {
		String name = pathname.getName().toLowerCase();
		return pathname.isDirectory() || name.endsWith(extension);
	}

	@Override
	public String getDescription() {
		return description;
	}
}
