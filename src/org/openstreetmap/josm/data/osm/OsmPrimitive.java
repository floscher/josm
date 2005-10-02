package org.openstreetmap.josm.data.osm;

import java.util.Collection;
import java.util.Map;


/**
 * An OSM primitive can be associated with a key/value pair. It can be created, deleted
 * and updated within the OSM-Server.
 * 
 * @author imi
 */
abstract public class OsmPrimitive {

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
	 * Return a list of all nodes, this osmPrimitive consists of. Does return
	 * an empty list, if it is an primitive that cannot have nodes (e.g. Key)
	 */
	abstract public Collection<Node> getAllNodes();

	/**
	 * Return <code>true</code>, if either <code>this.keys</code> and 
	 * <code>other.keys</code> is <code>null</code> or if they do not share Keys
	 * with different values.
	 *  
	 * @param other		The second key-set to compare with.
	 * @return	True, if the keysets are mergable
	 */
	public boolean keyPropertiesMergable(OsmPrimitive other) {
		if ((keys == null) != (other.keys == null))
			return false;

		if (keys != null) {
			for (Key k : keys.keySet())
				if (other.keys.containsKey(k) && !keys.get(k).equals(other.keys.get(k)))
					return false;
			for (Key k : other.keys.keySet())
				if (keys.containsKey(k) && !other.keys.get(k).equals(keys.get(k)))
					return false;
		}
		return true;
	}
	
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
