package org.openstreetmap.josm.data.osm;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;

import org.openstreetmap.josm.data.Bounds;

/**
 * DataSet is the data behind one window in the application. It can consist of only a few
 * points up to the whole osm database. DataSet's can be merged together, split up into
 * several different ones, saved, (up/down/disk)loaded etc.
 *
 * Note, that DataSet is not an osm-primitive, so it has no key association but a few
 * members to store some information.
 * 
 * @author imi
 */
public class DataSet implements Cloneable {

	/**
	 * All nodes goes here, even when included in other data (tracks etc).
	 * This enables the instant conversion of the whole DataSet by iterating over
	 * this data structure.
	 */
	public Collection<Node> nodes = new LinkedList<Node>();

	/**
	 * All pending line segments goes here. Pending line segments are those, that 
	 * are in this list but are in no track.
	 */
	public Collection<LineSegment> pendingLineSegments = new LinkedList<LineSegment>();

	/**
	 * All tracks (Streets etc.) in the DataSet. 
	 * 
	 * The nodes of the track segments of this track must be objects from 
	 * the nodes list, however the track segments are stored only in the 
	 * track list.
	 */
	public Collection<Track> tracks;

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
	 * Return all tracks that contain the node. If nothing found, an empty array
	 * is returned.
	 * 
	 * @param node This node is searched.
	 * @return All tracks, that reference the node in one of its line segments.
	 */
	public Collection<Track> getReferencedTracks(Node n) {
		Collection<Track> all = new LinkedList<Track>();
		for (Track t : tracks) {
			for (LineSegment ls : t.segments) {
				if (ls.start == n || ls.end == n) {
					all.add(t);
					break;
				}
			}
		}
		return all;
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
		clearSelection(tracks);
		for (Track t : tracks)
			clearSelection(t.segments);
	}

	/**
	 * Return a list of all selected objects. Even keys are returned.
	 * @return List of all selected objects.
	 */
	public Collection<OsmPrimitive> getSelected() {
		Collection<OsmPrimitive> sel = getSelected(nodes);
		sel.addAll(getSelected(pendingLineSegments));
		sel.addAll(getSelected(tracks));
		for (Track t : tracks)
			sel.addAll(getSelected(t.segments));
		return sel;
	}
	
	/**
	 * Remove the selection from every value in the collection.
	 * @param list The collection to remove the selection from.
	 */
	private void clearSelection(Collection<? extends OsmPrimitive> list) {
		if (list == null)
			return;
		for (OsmPrimitive osm : list) {
			osm.selected = false;
			if (osm.keys != null)
				clearSelection(osm.keys.keySet());
		}
	}

	/**
	 * Return all selected items in the collection.
	 * @param list The collection from which the selected items are returned.
	 */
	private Collection<OsmPrimitive> getSelected(Collection<? extends OsmPrimitive> list) {
		Collection<OsmPrimitive> sel = new HashSet<OsmPrimitive>();
		if (list == null)
			return sel;
		for (OsmPrimitive osm : list) {
			if (osm.selected)
				sel.add(osm);
			if (osm.keys != null)
				sel.addAll(getSelected(osm.keys.keySet()));
		}
		return sel;
	}


	@Override
	public DataSet clone() {
		try {return (DataSet)super.clone();} catch (CloneNotSupportedException e) {}
		return null;
	}
}
