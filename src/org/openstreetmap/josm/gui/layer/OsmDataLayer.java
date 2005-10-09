package org.openstreetmap.josm.gui.layer;

import javax.swing.Icon;

import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.gui.ImageProvider;
import org.openstreetmap.josm.gui.engine.SimpleEngine;

/**
 * A layer holding data imported from the osm server.
 * 
 * The data can be fully edited.
 * 
 * @author imi
 */
public class OsmDataLayer extends DataLayer {

	private static Icon icon;
	
	/**
	 * Construct a OsmDataLayer.
	 */
	protected OsmDataLayer(DataSet dataSet, String name) {
		super(dataSet, new SimpleEngine(), name);
	}

	/**
	 * TODO: @return Return a dynamic drawn icon of the map data. The icon is
	 * 		updated by a background thread to not disturb the running programm.
	 */
	public Icon getIcon() {
		if (icon == null)
			icon = ImageProvider.get("layer", "osmdata");
		return icon;
	}

	public boolean isEditable() {
		return true;
	}
}
