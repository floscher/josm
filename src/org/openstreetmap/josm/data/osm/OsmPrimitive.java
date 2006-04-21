package org.openstreetmap.josm.data.osm;

import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

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
	 * <code>true</code>, if the object has been modified since it was loaded from
	 * the server. In this case, on next upload, this object will be updated.
	 * Deleted objects are deleted from the server. If the objects are added (id=0),
	 * the modified is ignored and the object is added to the server. 
	 */
	public boolean modified = false;

	/**
	 * <code>true</code>, if the object has been deleted.
	 */
	public boolean deleted = false;

	/**
	 * If set to true, this object is currently selected.
	 */
	public volatile boolean selected = false;

	/**
	 * Time of last modification to this object. This is not set by JOSM but
	 * read from the server and delivered back to the server unmodified. It is
	 * used to check against edit conflicts.
	 */
	public Date timestamp = null;

	/**
	 * Implementation of the visitor scheme. Subclases have to call the correct
	 * visitor function.
	 * @param visitor The visitor from which the visit() function must be called.
	 */
	abstract public void visit(Visitor visitor);

	public final void delete(boolean deleted) {
		this.deleted = deleted;
		selected = false;
		modified = true;
	}

	/**
	 * Equal, if the id (and class) is equal. If both ids are 0, use the super classes
	 * equal instead.
	 * 
	 * An primitive is equal to its incomplete counter part.
	 */
	@Override public final boolean equals(Object obj) {
		if (obj == null || getClass() != obj.getClass() || id == 0 || ((OsmPrimitive)obj).id == 0)
			return super.equals(obj);
		return id == ((OsmPrimitive)obj).id;
	}

	/**
	 * Return the id as hashcode or supers hashcode if 0.
	 * 
	 * An primitive has the same hashcode as its incomplete counter part.
	 */
	@Override public final int hashCode() {
		return id == 0 ? super.hashCode() : (int)id;
	}

	/**
	 * Set the given value to the given key
	 * @param key The key, for which the value is to be set.
	 * @param value The value for the key.
	 */
	public final void put(String key, String value) {
		if (value == null)
			remove(key);
		else {
			if (keys == null)
				keys = new HashMap<String, String>();
			keys.put(key, value);
		}
	}
	/**
	 * Remove the given key from the list.
	 */
	public final void remove(String key) {
		if (keys != null) {
			keys.remove(key);
			if (keys.isEmpty())
				keys = null;
		}
	}

	public final String get(String key) {
		return keys == null ? null : keys.get(key);
	}

	public final Collection<Entry<String, String>> entrySet() {
		if (keys == null)
			return Collections.emptyList();
		return keys.entrySet();
	}

	public final Collection<String> keySet() {
		if (keys == null)
			return Collections.emptyList();
		return keys.keySet();
	}

	/**
	 * Get and write all attributes from the parameter. Does not fire any listener, so
	 * use this only in the data initializing phase
	 */
	public void cloneFrom(OsmPrimitive osm) {
		keys = osm.keys == null ? null : new HashMap<String, String>(osm.keys);
		id = osm.id;
		modified = osm.modified;
		deleted = osm.deleted;
		selected = osm.selected;
		timestamp = osm.timestamp;
	}

	/**
	 * Perform an equality compare for all non-volatile fields not only for the id 
	 * but for the whole object (for conflict resolving etc)
	 */
	public boolean realEqual(OsmPrimitive osm) {
		return
		id == osm.id && 
		modified == osm.modified && 
		deleted == osm.deleted &&
		(timestamp == null ? osm.timestamp==null : timestamp.equals(osm.timestamp)) &&
		(keys == null ? osm.keys==null : keys.equals(osm.keys));
	}
	
	public String getTimeStr() {
		return timestamp == null ? null : new SimpleDateFormat("y-M-d H:m:s").format(timestamp);
	}
}
