package org.openstreetmap.josm.data.osm;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;

import org.openstreetmap.josm.data.Bounds;
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
	 * All nodes goes here, even when included in other data (waies etc).
	 * This enables the instant conversion of the whole DataSet by iterating over
	 * this data structure.
	 */
	public Collection<Node> nodes = new LinkedList<Node>();

	/**
	 * All line segments goes here, even when they are in a way.
	 */
	public Collection<LineSegment> lineSegments = new LinkedList<LineSegment>();

	/**
	 * All waies (Streets etc.) in the DataSet. 
	 * 
	 * The nodes of the way segments of this way must be objects from 
	 * the nodes list, however the way segments are stored only in the 
	 * way list.
	 */
	public Collection<Way> waies = new LinkedList<Way>();

	/**
	 * @return A collection containing all primitives (except keys) of the
	 * dataset.
	 */
	public Collection<OsmPrimitive> allPrimitives() {
		Collection<OsmPrimitive> o = new LinkedList<OsmPrimitive>();
		o.addAll(nodes);
		o.addAll(lineSegments);
		o.addAll(waies);
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
	 * Return the bounds of this DataSet, depending on X/Y values.
	 * The min of the return value is the upper left GeoPoint, the max the lower
	 * down GeoPoint, regarding to the X/Y values.
	 * 
	 * Return null, if any point not converted yet or if there are no points at all.
	 * 
	 * @return Bounding coordinate structure.
	 */
	public Bounds getBoundsXY() {
		if (nodes.isEmpty())
			return null;

		Node first = nodes.iterator().next();
		Bounds b = new Bounds(first.coor.clone(), first.coor.clone());
		for (Node w : nodes)
		{
			if (Double.isNaN(w.coor.x) || Double.isNaN(w.coor.y))
				return null;
			if (w.coor.x < b.min.x)
				b.min.x = w.coor.x;
			if (w.coor.y < b.min.y)
				b.min.y = w.coor.y;
			if (w.coor.x > b.max.x)
				b.max.x = w.coor.x;
			if (w.coor.y > b.max.y)
				b.max.y = w.coor.y;
		}
		return b;
	}

	/**
	 * Return the bounds of this DataSet, depending on lat/lon values.
	 * The min of the return value is the upper left GeoPoint, the max the lower
	 * down GeoPoint.
	 * 
	 * Return null, if any point does not have lat/lon or if there are no 
	 * points at all.
	 * 
	 * @return Bounding coordinate structure.
	 */
	public Bounds getBoundsLatLon() {
		if (nodes.isEmpty())
			return null;

		Node first = nodes.iterator().next();
		Bounds b = new Bounds(first.coor.clone(), first.coor.clone());
		for (Node w : nodes)
		{
			if (Double.isNaN(w.coor.lat) || Double.isNaN(w.coor.lon))
				return null;
			if (w.coor.lat < b.min.lat)
				b.min.lat = w.coor.lat;
			if (w.coor.lon < b.min.lon)
				b.min.lon = w.coor.lon;
			if (w.coor.lat > b.max.lat)
				b.max.lat = w.coor.lat;
			if (w.coor.lon > b.max.lon)
				b.max.lon = w.coor.lon;
		}
		return b;
	}

	/**
	 * Remove the selection of the whole dataset.
	 */
	public void clearSelection() {
		clearSelection(nodes);
		clearSelection(lineSegments);
		clearSelection(waies);
	}

	/**
	 * Return a list of all selected objects. Even keys are returned.
	 * @return List of all selected objects.
	 */
	@Override
	public Collection<OsmPrimitive> getSelected() {
		Collection<OsmPrimitive> sel = getSelected(nodes);
		sel.addAll(getSelected(lineSegments));
		sel.addAll(getSelected(waies));
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
