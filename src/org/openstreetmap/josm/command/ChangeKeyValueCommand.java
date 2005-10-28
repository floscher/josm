package org.openstreetmap.josm.command;

import java.awt.Component;
import java.util.Collection;
import java.util.HashMap;

import javax.swing.JLabel;

import org.openstreetmap.josm.data.osm.Key;
import org.openstreetmap.josm.data.osm.OsmPrimitive;

/**
 * Command that manipulate the key/value structure of several objects. Manages deletion,
 * adding and modify of values and keys.
 * 
 * @author imi
 */
public class ChangeKeyValueCommand implements Command {

	/**
	 * All primitives, that are affected with this command.
	 */
	private final Collection<OsmPrimitive> objects;
	/**
	 * The key that is subject to change.
	 */
	private final Key key;
	/**
	 * The key value. If it is <code>null</code>, delete all key references with the given
	 * key. Else, change the properties of all objects to the given value or create keys of
	 * those objects that do not have the key yet.
	 */
	private final String value;

	public ChangeKeyValueCommand(Collection<OsmPrimitive> objects, Key key, String value) {
		this.objects = objects;
		this.key = key;
		this.value = value;
	}
	
	public void executeCommand() {
		if (value == null) {
			for (OsmPrimitive osm : objects) {
				if (osm.keys != null) {
					osm.keys.remove(key);
					if (osm.keys.isEmpty())
						osm.keys = null;
				}
			}
		} else {
			for (OsmPrimitive osm : objects) {
				if (osm.keys == null)
					osm.keys = new HashMap<Key, String>();
				osm.keys.put(key, value);
			}
		}
	}

	public Component commandDescription() {
		String objStr = objects.size()+" object" + (objects.size()==1?"":"s");
		if (value == null)
			return new JLabel("Delete the key '"+key+"' of "+objStr);
		return new JLabel("Change the key '"+key+"' of "+objStr+" to '"+value+"'");
	}

	public void fillModifiedData(Collection<OsmPrimitive> modified, Collection<OsmPrimitive> deleted, Collection<OsmPrimitive> added) {
		modified.addAll(objects);
	}

}
