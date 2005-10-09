package org.openstreetmap.josm.gui.layer;

import javax.swing.Icon;

import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.gui.ImageProvider;
import org.openstreetmap.josm.gui.engine.RawGpsEngine;

/**
 * A layer holding data from a gps source.
 * The data is read only.
 * 
 * @author imi
 */
public class RawGpsDataLayer extends DataLayer {

	private static Icon icon;

	protected RawGpsDataLayer(DataSet dataSet, String name) {
		super(dataSet, new RawGpsEngine(), name);
	}

	/**
	 * Return a static icon.
	 */
	public Icon getIcon() {
		if (icon == null)
			icon = ImageProvider.get("layer", "rawgps");
		return icon;
	}

	public boolean isEditable() {
		return false;
	}
}
