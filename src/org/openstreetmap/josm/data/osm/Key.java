package org.openstreetmap.josm.data.osm;

import java.util.HashMap;
import java.util.Map;

import org.openstreetmap.josm.data.osm.visitor.Visitor;


/**
 * A key can be associated together with a value to any osm primitive.
 *
 * @author imi
 */
public class Key extends OsmPrimitive {

	/**
	 * The key's name
	 */
	public final String name;

	/**
	 * All keys are stored here.
	 */
	private static Map<String, Key> allKeys = new HashMap<String, Key>();
	
	/**
	 * Generate a key with the given name. You cannot call this directly but
	 * have to use the static constructor. This makes sure, you get every key
	 * only once.
	 */
	private Key(String name) {
		this.name = name;
	}

	/**
	 * Get an instance of the key with the given name.
	 * @param name	The name of the key to get.
	 * @return An shared instance of the key with the given name. 
	 */
	public static Key get(String name) {
		Key key = allKeys.get(name);
		if (key == null) {
			key = new Key(name);
			allKeys.put(name, key);
		}
		return key;
	}
	
	@Override
	public void visit(Visitor visitor) {
		visitor.visit(this);
	}
}
