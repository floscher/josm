package org.openstreetmap.josm.data.osm.visitor;

import java.util.Iterator;

import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Key;
import org.openstreetmap.josm.data.osm.LineSegment;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Track;

/**
 * A visitor that get a data set at construction time and merge every visited object
 * into it.
 * 
 * @author imi
 */
public class MergeVisitor implements Visitor {

	private final DataSet ds;
	
	public MergeVisitor(DataSet ds) {
		this.ds = ds;
	}

	/**
	 * Merge the node if the id matches with any of the internal set or if
	 * either id is zero, merge if lat/lon matches.
	 */
	public void visit(Node otherNode) {
		Node myNode = null;
		for (Node n : ds.nodes) {
			if (match(n, otherNode)) {
				myNode = n;
				break;
			}
		}
		if (myNode == null)
			ds.nodes.add(otherNode);
		else {
			mergeCommon(myNode, otherNode);
			if (myNode.modified && !otherNode.modified)
				return;
			if (!myNode.coor.equalsLatLonEpsilon(otherNode.coor)) {
				myNode.coor = otherNode.coor;
				myNode.modified = otherNode.modified;
			}
		}
	}

	/**
	 * Merge the line segment if id matches or if both nodes are the same (and the
	 * id is zero of either segment). Nodes are the "same" when they @see match
	 */
	public void visit(LineSegment otherLs) {
		LineSegment myLs = null;
		for (LineSegment ls : ds.lineSegments) {
			if (match(otherLs, ls)) {
				myLs = ls;
				break;
			}
		}
		if (myLs == null)
			ds.lineSegments.add(otherLs);
		else {
			mergeCommon(myLs, otherLs);
			if (myLs.modified && !otherLs.modified)
				return;
			if (!match(myLs.start, otherLs.start)) {
				myLs.start = otherLs.start;
				myLs.modified = otherLs.modified;
			}
			if (!match(myLs.end, otherLs.end)) {
				myLs.end = otherLs.end;
				myLs.modified = otherLs.modified;
			}
		}
	}

	/**
	 * Merge the track if id matches or if all line segments matches and the
	 * id is zero of either track.
	 */
	public void visit(Track otherTrack) {
		Track myTrack = null;
		for (Track t : ds.tracks) {
			if (match(otherTrack, t)) {
				myTrack = t;
				break;
			}
		}
		if (myTrack == null)
			ds.tracks.add(otherTrack);
		else {
			mergeCommon(myTrack, otherTrack);
			if (myTrack.modified && !otherTrack.modified)
				return;
			boolean same = true;
			Iterator<LineSegment> it = otherTrack.segments.iterator();
			for (LineSegment ls : myTrack.segments) {
				if (!match(ls, it.next())) {
					same = false;
				}
			}
			if (!same) {
				myTrack.segments.clear();
				myTrack.segments.addAll(otherTrack.segments);
				myTrack.modified = otherTrack.modified;
			}
		}
	}

	public void visit(Key k) {
		//TODO: Key doesn't really fit the OsmPrimitive concept!
	}
	
	/**
	 * @return Whether the nodes matches (in sense of "be mergable").
	 */
	private boolean match(Node n1, Node n2) {
		if (n1.id == 0 || n2.id == 0)
			return n1.coor.equalsLatLonEpsilon(n2.coor);
		return n1.id == n2.id;
	}
	
	/**
	 * @return Whether the line segments matches (in sense of "be mergable").
	 */
	private boolean match(LineSegment ls1, LineSegment ls2) {
		if (ls1.id == 0 || ls2.id == 0)
			return match(ls1.start, ls2.start) && match(ls1.end, ls2.end);
		return ls1.id == ls2.id;
	}

	/**
	 * @return Whether the tracks matches (in sense of "be mergable").
	 */
	private boolean match(Track t1, Track t2) {
		if (t1.id == 0 || t2.id == 0) {
			Iterator<LineSegment> it = t1.segments.iterator();
			for (LineSegment ls : t2.segments)
				if (!match(ls, it.next()))
					return false;
			return true;
		}
		return t1.id == t2.id;
	}

	/**
	 * Merge the common parts of an osm primitive.
	 * @param myOsm The object, the information gets merged into
	 * @param otherOsm The object, the information gets merged from
	 */
	private void mergeCommon(OsmPrimitive myOsm, OsmPrimitive otherOsm) {
		if (otherOsm.isDeleted())
			myOsm.setDeleted(true);
		if (!myOsm.modified || otherOsm.modified) {
			if (myOsm.id == 0 && otherOsm.id != 0)
				myOsm.id = otherOsm.id; // means not ncessary the object is now modified
			else if (myOsm.id != 0 && otherOsm.id != 0 && otherOsm.modified)
				myOsm.modified = true;
		}
		if (myOsm.modifiedProperties && !otherOsm.modifiedProperties)
			return;
		if (otherOsm.keys == null)
			return;
		if (myOsm.keys != null && myOsm.keys.entrySet().containsAll(otherOsm.keys.entrySet()))
			return;
		if (myOsm.keys == null)
			myOsm.keys = otherOsm.keys;
		else
			myOsm.keys.putAll(otherOsm.keys);
		myOsm.modifiedProperties = true;
	}
}
