package org.openstreetmap.josm.data.osm;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.osm.visitor.Visitor;


/**
 * An OSM primitive can be associated with a key/value pair. It can be created, deleted
 * and updated within the OSM-Server.
 *
 * Although OsmPrimitive is designed as a base class, it is not to be meant to subclass
 * it by any other than from the package org.openstreetmap.josm.data.osm (thus the
 * visibility of the constructor). The available primitives are a fixed set that are given
 * by the server environment and not an extendible data stuff. 
 *
 * @author imi
 */
abstract public class OsmPrimitive {

	/**
	 * The key/value list for this primitive.
	 */
	public Map<String, String> keys;

	/**
	 * Unique identifier in OSM. This is used to reidentify objects in the server.
	 * An id of 0 means an unknown id. The object has not been uploaded yet to 
	 * know what id it will get.
	 */
	public long id = 0;

	/**
	 * <code>true</code>, if the objects content (not the properties) has been 
	 * modified since it was loaded from the server. In this case, on next upload,
	 * this object will be updated. Deleted objects are deleted from the server.
	 * If the objects are added (id=0), the modified is ignored and the object is
	 * added to the server. 
	 */
	public boolean modified = false;

	/**
	 * <code>true</code>, if the object's keys has been changed by JOSM since
	 * last update.
	 */
	public boolean modifiedProperties = false;
	
	/**
	 * <code>true</code>, if the object has been deleted.
	 */
	private boolean deleted = false;

	/**
	 * If set to true, this object is currently selected.
	 */
	private boolean selected = false;

	/**
	 * Implementation of the visitor scheme. Subclases have to call the correct
	 * visitor function.
	 * @param visitor The visitor from which the visit() function must be called.
	 */
	abstract public void visit(Visitor visitor);

	/**
	 * Return <code>true</code>, if either <code>this.keys</code> and 
	 * <code>other.keys</code> is <code>null</code> or if they do not share Keys
	 * with different values.
	 *  
	 * @param other		The second key-set to compare with.
	 * @return	True, if the keysets are mergable
	 */
	final public boolean keyPropertiesMergable(OsmPrimitive other) {
		if ((keys == null) != (other.keys == null))
			return false;

		if (keys != null) {
			for (String k : keys.keySet())
				if (other.keys.containsKey(k) && !keys.get(k).equals(other.keys.get(k)))
					return false;
			for (String k : other.keys.keySet())
				if (keys.containsKey(k) && !other.keys.get(k).equals(keys.get(k)))
					return false;
		}
		return true;
	}

	/**
	 * Mark the primitive as selected or not selected and fires a selection 
	 * changed later, if the value actualy changed.
	 * @param selected Whether the primitive should be selected or not.
	 */
	final public void setSelected(boolean selected) {
		if (selected != this.selected)
			Main.main.ds.fireSelectionChanged();
		this.selected = selected;
	}

	/**
	 * @return Return whether the primitive is selected on screen.
	 */
	final public boolean isSelected() {
		return selected;
	}


	public void setDeleted(boolean deleted) {
		this.deleted = deleted;
		setSelected(false);
	}

	public boolean isDeleted() {
		return deleted;
	}

	/**
	 * Equal, if the id (and class) is equal. If both ids are 0, use the super classes
	 * equal instead.
	 * 
	 * An primitive is equal to its incomplete counter part.
	 */
	@Override
	public boolean equals(Object obj) {
		if (getClass() != obj.getClass() || id == 0 || obj == null || ((OsmPrimitive)obj).id == 0)
			return super.equals(obj);
		return id == ((OsmPrimitive)obj).id;
	}

	/**
	 * Return the id as hashcode or supers hashcode if 0.
	 * 
	 * An primitive has the same hashcode as its incomplete counter part.
	 */
	@Override
	public int hashCode() {
		return id == 0 ? super.hashCode() : (int)id;
	}

	/**
	 * Set the given value to the given key
	 * @param key The key, for which the value is to be set.
	 * @param value The value for the key.
	 */
	public void put(String key, String value) {
		if (keys == null)
			keys = new HashMap<String, String>();
		keys.put(key, value);
	}
	/**
	 * Remove the given key from the list.
	 */
	public void remove(String key) {
		if (keys != null) {
			keys.remove(key);
			if (keys.isEmpty())
				keys = null;
		}
	}

	public String get(String key) {
		return keys == null ? null : keys.get(key);
	}
	
	public Collection<Entry<String, String>> entrySet() {
		if (keys == null)
			return Collections.emptyList();
		return keys.entrySet();
	}

	public Collection<String> keySet() {
		if (keys == null)
			return Collections.emptyList();
		return keys.keySet();
	}

	/**
	 * Get and write all attributes from the parameter. Does not fire any listener, so
	 * use this only in the data initializing phase
	 */
	public void cloneFrom(OsmPrimitive osm) {
		keys = osm.keys;
		id = osm.id;
		modified = osm.modified;
		modifiedProperties = osm.modifiedProperties;
		deleted = osm.deleted;
		selected = osm.selected;
	}
}
