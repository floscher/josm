package org.openstreetmap.josm.data.osm;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;

import org.openstreetmap.josm.data.SelectionChangedListener;

/**
 * DataSet is the data behind the application. It can consists of only a few
 * points up to the whole osm database. DataSet's can be merged together, 
 * saved, (up/down/disk)loaded etc.
 *
 * Note, that DataSet is not an osm-primitive and so has no key association 
 * but a few members to store some information.
 * 
 * @author imi
 */
public class DataSet {

	/**
	 * All nodes goes here, even when included in other data (ways etc).
	 * This enables the instant conversion of the whole DataSet by iterating over
	 * this data structure.
	 */
	public Collection<Node> nodes = new LinkedList<Node>();

	/**
	 * All segments goes here, even when they are in a way.
	 */
	public Collection<Segment> segments = new LinkedList<Segment>();

	/**
	 * All ways (Streets etc.) in the DataSet. 
	 * 
	 * The nodes of the way segments of this way must be objects from 
	 * the nodes list, however the way segments are stored only in the 
	 * way list.
	 */
	public Collection<Way> ways = new LinkedList<Way>();

	/**
	 * A list of listeners to selection changed events.
	 */
	transient Collection<SelectionChangedListener> listeners = new LinkedList<SelectionChangedListener>();

	/**
	 * @return A collection containing all primitives (except keys) of the
	 * dataset.
	 */
	public Collection<OsmPrimitive> allPrimitives() {
		Collection<OsmPrimitive> o = new LinkedList<OsmPrimitive>();
		o.addAll(nodes);
		o.addAll(segments);
		o.addAll(ways);
		return o;
	}

	/**
	 * @return A collection containing all not-deleted primitives (except keys).
	 */
	public Collection<OsmPrimitive> allNonDeletedPrimitives() {
		Collection<OsmPrimitive> o = new LinkedList<OsmPrimitive>();
		for (OsmPrimitive osm : allPrimitives())
			if (!osm.deleted)
				o.add(osm);
		return o;
	}

	/**
	 * Remove the selection of the whole dataset.
	 */
	public void clearSelection() {
		clearSelection(nodes);
		clearSelection(segments);
		clearSelection(ways);
		Collection<OsmPrimitive> sel = Collections.emptyList();
		fireSelectionChanged(sel);
	}

	/**
	 * Return a list of all selected objects. Even keys are returned.
	 * @return List of all selected objects.
	 */
	public Collection<OsmPrimitive> getSelected() {
		Collection<OsmPrimitive> sel = getSelected(nodes);
		sel.addAll(getSelected(segments));
		sel.addAll(getSelected(ways));
		return sel;
	}

	public void setSelected(Collection<? extends OsmPrimitive> selection) {
		clearSelection();
		for (OsmPrimitive osm : selection)
			osm.selected = true;
		fireSelectionChanged(selection);
	}

	public void setSelected(OsmPrimitive osm) {
		clearSelection();
		if (osm == null)
			return;
		osm.selected = true;
		fireSelectionChanged(Arrays.asList(new OsmPrimitive[]{osm}));
	}

	/**
	 * Remove the selection from every value in the collection.
	 * @param list The collection to remove the selection from.
	 */
	private void clearSelection(Collection<? extends OsmPrimitive> list) {
		if (list == null)
			return;
		for (OsmPrimitive osm : list)
			osm.selected = false;
	}

	/**
	 * Return all selected items in the collection.
	 * @param list The collection from which the selected items are returned.
	 */
	private Collection<OsmPrimitive> getSelected(Collection<? extends OsmPrimitive> list) {
		Collection<OsmPrimitive> sel = new HashSet<OsmPrimitive>();
		if (list == null)
			return sel;
		for (OsmPrimitive osm : list)
			if (osm.selected && !osm.deleted)
				sel.add(osm);
		return sel;
	}

	/**
	 * Remember to fire an selection changed event. A call to this will not fire
	 * the event immediately. For more, @see SelectionChangedListener
	 */
	public void fireSelectionChanged(Collection<? extends OsmPrimitive> sel) {
		for (SelectionChangedListener l : listeners)
			l.selectionChanged(sel);
	}

	/**
	 * Add a listener to the selection changed listener list. If <code>null</code>
	 * is passed, nothing happens.
	 * @param listener The listener to add to the list.
	 */
	public void addSelectionChangedListener(SelectionChangedListener listener) {
		if (listener != null)
			listeners.add(listener);
	}

	/**
	 * Remove a listener from the selection changed listener list. 
	 * If <code>null</code> is passed, nothing happens.
	 * @param listener The listener to remove from the list.
	 */
	public void removeSelectionChangedListener(SelectionChangedListener listener) {
		if (listener != null)
			listeners.remove(listener);
	}

	public void addAllSelectionListener(DataSet ds) {
		listeners.addAll(ds.listeners);
	}

	/**
	 * Compares this and the parameter dataset and return <code>true</code> if both
	 * contain the same data primitives (ignoring the selection)
	 */
	public boolean realEqual(Collection<OsmPrimitive> other) {
		Collection<OsmPrimitive> my = allPrimitives();

		if (my.size() != other.size())
			return false;

		Iterator<OsmPrimitive> it = other.iterator();
		for (OsmPrimitive osm : my)
			if (!osm.realEqual(it.next()))
				return false;

		return true;
	}
}
