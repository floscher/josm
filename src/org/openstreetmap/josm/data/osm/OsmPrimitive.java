package org.openstreetmap.josm.data.osm;

import java.util.Collection;
import java.util.Map;

import org.openstreetmap.josm.data.osm.visitor.Visitor;


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
	transient private boolean selected = false;

	/**
	 * Return a list of all nodes, this osmPrimitive consists of. Does return
	 * an empty list, if it is an primitive that cannot have nodes (e.g. Key)
	 * TODO replace with visitor
	 */
	abstract public Collection<Node> getAllNodes();

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
	 * Mark the primitive as selected or not selected and fires a selection 
	 * changed later, if the value actualy changed.
	 * @param selected Whether the primitive should be selected or not.
	 * @param ds The dataSet, this primitive is in.
	 */
	public void setSelected(boolean selected, DataSet ds) {
		if (selected != this.selected)
			ds.fireSelectionChanged();
		this.selected = selected;
	}

	/**
	 * @return Return whether the primitive is selected on screen.
	 */
	public boolean isSelected() {
		return selected;
	}
}
