package org.openstreetmap.josm.command;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.SelectionTracker;
import org.openstreetmap.josm.data.osm.LineSegment;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Track;

/**
 * DataSet is the data behind one window in the application. It can consist of only a few
 * points up to the whole osm database. DataSet's can be merged together, split up into
 * several different ones, saved, (up/down/disk)loaded etc.
 *
 * Note, that DataSet is not an osm-primitive, it is not within 
 * org.openstreetmap.josm.data.osm and has no key association but a few
 * members to store some information.
 * 
 * @author imi
 */
public class DataSet extends SelectionTracker implements Cloneable {

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
	Collection<LineSegment> pendingLineSegments = new LinkedList<LineSegment>();

	/**
	 * All tracks (Streets etc.) in the DataSet. 
	 * 
	 * The nodes of the track segments of this track must be objects from 
	 * the nodes list, however the track segments are stored only in the 
	 * track list.
	 */
	Collection<Track> tracks = new LinkedList<Track>();

	/**
	 * Add the track to the tracklist.
	 */
	public void addTrack(Track t) {
		tracks.add(t);
	}
	/**
	 * Remove the track from the tracklist.
	 */
	public void removeTrack(Track t) {
		t.destroy();
		tracks.remove(t);
	}
	/**
	 * Return a read-only collection of all tracks
	 */
	public Collection<Track> tracks() {
		return Collections.unmodifiableCollection(tracks);
	}

	/**
	 * Add a newly created line segment to the pending lines list.
	 */
	public void addPendingLineSegment(LineSegment ls) {
		pendingLineSegments.add(ls);
	}
	/**
	 * Remove a line segment from the pending lines list, because it has been
	 * assigned to the track.
	 * @param ls The line segment from the pending list
	 * @param t The track, that will hold the line segment
	 * @param end <code>true</code> to attach on the end. <code>false</code>
	 * 		to attach on the beginning.
	 */
	public void assignPendingLineSegment(LineSegment ls, Track t, boolean end) {
		pendingLineSegments.remove(ls);
		if (end)
			t.add(ls);
		else
			t.addStart(ls);
	}
	/**
	 * Delete the pending line segment without moving it anywhere.
	 */
	public void destroyPendingLineSegment(LineSegment ls) {
		pendingLineSegments.remove(ls);
		ls.destroy();
	}
	/**
	 * Return an read-only iterator over all pending line segments.
	 */
	public Collection<LineSegment> pendingLineSegments() {
		return Collections.unmodifiableCollection(pendingLineSegments);
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
		clearSelection(tracks);
		for (Track t : tracks)
			clearSelection(t.segments());
	}

	/**
	 * Return a list of all selected objects. Even keys are returned.
	 * @return List of all selected objects.
	 */
	@Override
	public Collection<OsmPrimitive> getSelected() {
		Collection<OsmPrimitive> sel = getSelected(nodes);
		sel.addAll(getSelected(pendingLineSegments));
		sel.addAll(getSelected(tracks));
		for (Track t : tracks)
			sel.addAll(getSelected(t.segments()));
		return sel;
	}

	/**
	 * Import the given dataset by merging all data with this dataset.
	 * The objects imported are not cloned, so from now on, these data belong
	 * to both datasets. So use mergeFrom only if you are about to abandon the
	 * other dataset or this dataset.
	 * 
	 * @param ds	The DataSet to merge into this one.
	 * @param mergeEqualNodes If <code>true</code>, nodes with the same lat/lon
	 * 		are merged together.
	 */
	public void mergeFrom(DataSet ds, boolean mergeEqualNodes) {
		if (mergeEqualNodes && !nodes.isEmpty()) {
			Map<Node, Node> mergeMap = new HashMap<Node, Node>();
			Set<Node> nodesToAdd = new HashSet<Node>();
			for (Node n : nodes) {
				for (Iterator<Node> it = ds.nodes.iterator(); it.hasNext();) {
					Node dsn = it.next();
					if (n.coor.equalsLatLon(dsn.coor)) {
						mergeMap.put(dsn, n);
						n.mergeFrom(dsn);
						it.remove();
					} else {
						nodesToAdd.add(dsn);
					}
				}
			}
			nodes.addAll(nodesToAdd);
			for (Track t : ds.tracks) {
				for (LineSegment ls : t.segments()) {
					Node n = mergeMap.get(ls.getStart());
					if (n != null)
						ls.start = n;
					n = mergeMap.get(ls.getEnd());
					if (n != null)
						ls.end = n;
				}
			}
			tracks.addAll(ds.tracks);
			for (LineSegment ls : ds.pendingLineSegments) {
				Node n = mergeMap.get(ls.getStart());
				if (n != null)
					ls.start = n;
				n = mergeMap.get(ls.getEnd());
				if (n != null)
					ls.end = n;
			}
			pendingLineSegments.addAll(ds.pendingLineSegments);
		} else {
			nodes.addAll(ds.nodes);
			tracks.addAll(ds.tracks);
			pendingLineSegments.addAll(ds.pendingLineSegments);
		}
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


	@Override
	public DataSet clone() {
		try {return (DataSet)super.clone();} catch (CloneNotSupportedException e) {}
		return null;
	}
}
