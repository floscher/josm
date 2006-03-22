package org.openstreetmap.josm.data.osm.visitor;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;

import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.LineSegment;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Way;

/**
 * A visitor that get a data set at construction time and merge every visited object
 * into it.
 * 
 * @author imi
 */
public class MergeVisitor implements Visitor {

	private final DataSet ds;
	
	/**
	 * A list of all nodes that got replaced with other nodes.
	 * Key is the node in the other's dataset and the value is the one that is now
	 * in ds.nodes instead.
	 */
	private final Map<Node, Node> mergedNodes = new HashMap<Node, Node>();
	/**
	 * A list of all line segments that got replaced with others.
	 * Key is the segment in the other's dataset and the value is the one that is now
	 * in ds.lineSegments.
	 */
	private final Map<LineSegment, LineSegment> mergedLineSegments = new HashMap<LineSegment, LineSegment>();
	
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
			mergedNodes.put(otherNode, myNode);
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
		else if (myLs.incomplete && !otherLs.incomplete) {
			mergedLineSegments.put(otherLs, myLs);
			myLs.cloneFrom(otherLs);
		} else if (!otherLs.incomplete) {
			mergedLineSegments.put(otherLs, myLs);
			mergeCommon(myLs, otherLs);
			if (myLs.modified && !otherLs.modified)
				return;
			if (!match(myLs.from, otherLs.from)) {
				myLs.from = otherLs.from;
				myLs.modified = otherLs.modified;
			}
			if (!match(myLs.to, otherLs.to)) {
				myLs.to = otherLs.to;
				myLs.modified = otherLs.modified;
			}
		}
	}

	/**
	 * Merge the way if id matches or if all line segments matches and the
	 * id is zero of either way.
	 */
	public void visit(Way otherWay) {
		Way myWay = null;
		for (Way t : ds.waies) {
			if (match(otherWay, t)) {
				myWay = t;
				break;
			}
		}
		if (myWay == null)
			ds.waies.add(otherWay);
		else {
			mergeCommon(myWay, otherWay);
			if (myWay.modified && !otherWay.modified)
				return;
			boolean same = true;
			Iterator<LineSegment> it = otherWay.segments.iterator();
			for (LineSegment ls : myWay.segments) {
				if (!match(ls, it.next()))
					same = false;
			}
			if (!same) {
				myWay.segments.clear();
				myWay.segments.addAll(otherWay.segments);
				myWay.modified = otherWay.modified;
			}
		}
	}

	/**
	 * Postprocess the dataset and fix all merged references to point to the actual
	 * data.
	 */
	public void fixReferences() {
		for (LineSegment ls : ds.lineSegments) {
			if (mergedNodes.containsKey(ls.from))
				ls.from = mergedNodes.get(ls.from);
			if (mergedNodes.containsKey(ls.to))
				ls.to = mergedNodes.get(ls.to);
		}
		for (Way t : ds.waies) {
			boolean replacedSomething = false;
			LinkedList<LineSegment> newSegments = new LinkedList<LineSegment>();
			for (LineSegment ls : t.segments) {
				LineSegment otherLs = mergedLineSegments.get(ls);
				newSegments.add(otherLs == null ? ls : otherLs);
				if (otherLs != null)
					replacedSomething = true;
			}
			if (replacedSomething) {
				t.segments.clear();
				t.segments.addAll(newSegments);
			}
			for (LineSegment ls : t.segments) {
				if (mergedNodes.containsKey(ls.from))
					ls.from = mergedNodes.get(ls.from);
				if (mergedNodes.containsKey(ls.to))
					ls.to = mergedNodes.get(ls.to);
			}
		}
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
		if (ls1.id == ls2.id)
			return true;
		if (ls1.incomplete || ls2.incomplete)
			return false;
		return match(ls1.from, ls2.from) && match(ls1.to, ls2.to);
	}

	/**
	 * @return Whether the waies matches (in sense of "be mergable").
	 */
	private boolean match(Way t1, Way t2) {
		if (t1.id == 0 || t2.id == 0) {
			if (t1.segments.size() != t2.segments.size())
				return false;
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
