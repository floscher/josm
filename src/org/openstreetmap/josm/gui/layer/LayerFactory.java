package org.openstreetmap.josm.gui.layer;

import org.openstreetmap.josm.command.DataSet;

/**
 * A factory class that create Layers.
 *
 * @author imi
 */
public class LayerFactory {

	/**
	 * Create a layer from a given DataSet. The DataSet cannot change over the
	 * layers lifetime (but the data in the dataset may change)
	 * 
	 * @param dataSet The dataSet this layer displays.
	 * @param rawGps  <code>true</code>, if the dataSet contain raw gps data.
	 * @return The created layer instance. 
	 */
	public static Layer create(DataSet dataSet, String name, boolean rawGps) {
		Layer layer = rawGps ? new RawGpsDataLayer(dataSet, name) : new OsmDataLayer(name);
		return layer;
	}
}
