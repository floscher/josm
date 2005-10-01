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
	transient public boolean modified = false;
	
	/**
	 * If set to true, this object is currently selected.
	 */
	transient public boolean selected = false;

	/**
	 * Osm primitives are equal, when their keys are equal. 
	 */
	@Override
	public boolean equals(Object obj) {
		if (obj == null)
			return false;
		if (!(obj instanceof OsmPrimitive))
			return false;
		OsmPrimitive osm = (OsmPrimitive)obj;
		if (keys == null)
			return osm.keys == null;
		return keys.equals(osm.keys);
	}

	/**
	 * Compute the hashCode from the keys.
	 */
	@Override
	public int hashCode() {
		return keys == null ? 0 : keys.hashCode();
	}
	
	
}
