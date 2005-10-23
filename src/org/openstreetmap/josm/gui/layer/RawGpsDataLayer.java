package org.openstreetmap.josm.gui.layer;

import javax.swing.Icon;

import org.openstreetmap.josm.command.DataSet;
import org.openstreetmap.josm.gui.ImageProvider;

/**
 * A layer holding data from a gps source.
 * The data is read only.
 * 
 * @author imi
 */
public class RawGpsDataLayer extends Layer {

	private static Icon icon;

	protected RawGpsDataLayer(DataSet dataSet, String name) {
		super(name);
	}

	/**
	 * Return a static icon.
	 */
	@Override
	public Icon getIcon() {
		if (icon == null)
			icon = ImageProvider.get("layer", "rawgps");
		return icon;
	}
}
