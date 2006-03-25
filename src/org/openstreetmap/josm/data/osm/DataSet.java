package org.openstreetmap.josm.data.osm;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;

import org.openstreetmap.josm.data.SelectionTracker;

/**
 * DataSet is the data behind the application. It can consist of only a few
 * points up to the whole osm database. DataSet's can be merged together, 
 * saved, (up/down/disk)loaded etc.
 *
 * Note, that DataSet is not an osm-primitive and so has no key association 
 * but a few members to store some information.
 * 
 * @author imi
 */
public class DataSet extends SelectionTracker {

	/**
	 * All nodes goes here, even when included in other data (ways etc).
	 * This enables the instant conversion of the whole DataSet by iterating over
	 * this data structure.
	 */
	public Collection<Node> nodes = new LinkedList<Node>();

	/**
	 * All line segments goes here, even when they are in a way.
	 */
	public Collection<LineSegment> lineSegments = new LinkedList<LineSegment>();

	/**
	 * All ways (Streets etc.) in the DataSet. 
	 * 
	 * The nodes of the way segments of this way must be objects from 
	 * the nodes list, however the way segments are stored only in the 
	 * way list.
	 */
	public Collection<Way> ways = new LinkedList<Way>();

	/**
	 * @return A collection containing all primitives (except keys) of the
	 * dataset.
	 */
	public Collection<OsmPrimitive> allPrimitives() {
		Collection<OsmPrimitive> o = new LinkedList<OsmPrimitive>();
		o.addAll(nodes);
		o.addAll(lineSegments);
		o.addAll(ways);
		return o;
	}

	/**
	 * @return A collection containing all not-deleted primitives (except keys).
	 */
	public Collection<OsmPrimitive> allNonDeletedPrimitives() {
		Collection<OsmPrimitive> o = new LinkedList<OsmPrimitive>();
		for (OsmPrimitive osm : allPrimitives())
			if (!osm.isDeleted())
				o.add(osm);
		return o;
	}

	/**
	 * Remove the selection of the whole dataset.
	 */
	public void clearSelection() {
		clearSelection(nodes);
		clearSelection(lineSegments);
		clearSelection(ways);
	}

	/**
	 * Return a list of all selected objects. Even keys are returned.
	 * @return List of all selected objects.
	 */
	@Override
	public Collection<OsmPrimitive> getSelected() {
		Collection<OsmPrimitive> sel = getSelected(nodes);
		sel.addAll(getSelected(lineSegments));
		sel.addAll(getSelected(ways));
		return sel;
	}

	/**
	 * Remove the selection from every value in the collection.
	 * @param list The collection to remove the selection from.
	 */
	private void clearSelection(Collection<? extends OsmPrimitive> list) {
		if (list == null)
			return;
		for (OsmPrimitive osm : list)
			osm.setSelected(false);
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
			if (osm.isSelected() && !osm.isDeleted())
				sel.add(osm);
		return sel;
	}
}
