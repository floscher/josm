package org.openstreetmap.josm.io;

import java.io.IOException;

import org.jdom.JDOMException;
import org.openstreetmap.josm.data.osm.DataSet;

/**
 * Class that uploades all changes to the osm server.
 * 
 * This is done like this:
 * - All objects with id = 0 are uploaded as new, except those in deleted, 
 *   which are ignored
 * - All objects in deleted list are deleted.
 * - All remaining objects with modified flag set are updated.
 * 
 * @author imi
 */
public class OsmServerWriter extends OsmConnection {

	
	/**
	 * Send the dataset to the server. Ask the user first and does nothing if
	 * he does not want to send the data.
	 */
	public void uploadOsm(DataSet dataSet) throws IOException, JDOMException {
		initAuthentication();
	}
}
