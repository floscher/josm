package org.openstreetmap.josm.data.osm;

import java.util.Collection;
import java.util.LinkedList;


/**
 * A key can be associated together with a value to any osm primitive.
 *
 * @author imi
 */
public class Key extends OsmPrimitive {

	/**
	 * The key's name
	 */
	public String name;

	/**
	 * Return an empty list, since keys cannot have nodes. 
	 */
	@Override
	public Collection<Node> getAllNodes() {
		return new LinkedList<Node>();
	}

	/**
	 * Keys are equal, when their name is equal, regardless of their other keys.
	 */
	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof Key))
			return false;
		return name.equals(((Key)obj).name);
	}

	@Override
	public int hashCode() {
		return name.hashCode();
	}
}
