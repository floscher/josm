package org.openstreetmap.josm.data.osm;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;

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
	 * All nodes goes here, even when included in other data (tracks etc).
	 * This enables the instant conversion of the whole DataSet by iterating over
	 * this data structure.
	 */
	public Collection<Node> nodes = new LinkedList<Node>();

	/**
	 * All line segments goes here, even when they are in a track.
	 */
	public Collection<LineSegment> lineSegments = new LinkedList<LineSegment>();

	/**
	 * All tracks (Streets etc.) in the DataSet. 
	 * 
	 * The nodes of the track segments of this track must be objects from 
	 * the nodes list, however the track segments are stored only in the 
	 * track list.
	 */
	public Collection<Track> tracks = new LinkedList<Track>();

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
		clearSelection(tracks);
	}

	/**
	 * Return a list of all selected objects. Even keys are returned.
	 * @return List of all selected objects.
	 */
	@Override
	public Collection<OsmPrimitive> getSelected() {
		Collection<OsmPrimitive> sel = getSelected(nodes);
		sel.addAll(getSelected(lineSegments));
		sel.addAll(getSelected(tracks));
		return sel;
	}

	/**
	 * Import the given dataset by merging all data with this dataset.
	 * The objects imported are not cloned, so from now on, these data belong
	 * to both datasets. So use mergeFrom only if you are about to abandon the
	 * other dataset.
	 *
	 * Elements are tried to merged. 
	 * Nodes are merged first, if their lat/lon are equal.
	 * Line segments are merged, if they have the same nodes.
	 * Tracks are merged, if they consist of the same line segments.
	 *
	 * TODO Additional to that, every two objects with the same id are merged.
	 *
	 * @param ds	The DataSet to merge into this one.
	 */
	public void mergeFrom(DataSet ds) {
		// merge nodes
		
		Map<Node, Node> nodeMap = new HashMap<Node, Node>();

		// find mergable
		for (Node otherNode : ds.nodes)
			for (Node myNode : nodes)
				if (otherNode.coor.equalsLatLon(myNode.coor))
					nodeMap.put(otherNode, myNode);
		// add
		for (Node n : ds.nodes)
			if (!nodeMap.containsKey(n))
				nodes.add(n);
		// reassign
		for (LineSegment ls : ds.lineSegments) {
			Node n = nodeMap.get(ls.start);
			if (n != null)
				ls.start = n;
			n = nodeMap.get(ls.end);
			if (n != null)
				ls.end = n;
		}


		// merge line segments

		Map<LineSegment, LineSegment> lsMap = new HashMap<LineSegment, LineSegment>();
		// find mergable
		for (LineSegment otherLS : ds.lineSegments)
			for (LineSegment myLS : lineSegments)
				if (otherLS.start == myLS.start && otherLS.end == myLS.end)
					lsMap.put(otherLS, myLS);
		// add ls
		for (LineSegment ls : ds.lineSegments)
			if (!lsMap.containsKey(ls))
				lineSegments.add(ls);
		// reassign
		for (Track t : ds.tracks) {
			for (int i = 0; i < t.segments.size(); ++i) {
				LineSegment newLS = lsMap.get(t.segments.get(i));
				if (newLS != null)
					t.segments.set(i, newLS);
			}
		}


		// merge tracks
		
		LinkedList<Track> trackToAdd = new LinkedList<Track>();
		for (Track otherTrack : ds.tracks) {
			boolean found = false;
			for (Track myTrack : tracks) {
				if (myTrack.segments.equals(otherTrack.segments)) {
					found = true;
					break;
				}
			}
			if (!found)
				trackToAdd.add(otherTrack);
		}
		tracks.addAll(trackToAdd);
	}

	/**
	 * Remove the selection from every value in the collection.
	 * @param list The collection to remove the selection from.
	 */
	private void clearSelection(Collection<? extends OsmPrimitive> list) {
		if (list == null)
			return;
		for (OsmPrimitive osm : list) {
			osm.setSelected(false);
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
			if (osm.isSelected())
				sel.add(osm);
			if (osm.keys != null)
				sel.addAll(getSelected(osm.keys.keySet()));
		}
		return sel;
	}
}
