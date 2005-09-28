package org.openstreetmap.josm.data.osm;

import java.util.Map;


/**
 * An OSM primitive can be associated with a key/value pair. It can be created, deleted
 * and updated within the OSM-Server.
 * 
 * @author imi
 */
public class OsmPrimitive {

	/**
	 * The key/value list for this primitive.
	 */
	public Map<Key, String> keys;
	
	/**
	 * If set to true, this object has been modified in the current session.
	 */
	public boolean modified = false;
	
	/**
	 * If set to true, this object is currently selected.
	 */
	public boolean selected = false;
}
