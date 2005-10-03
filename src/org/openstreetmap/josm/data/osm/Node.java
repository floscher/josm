package org.openstreetmap.josm.data.osm;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;

import org.openstreetmap.josm.data.GeoPoint;
import org.openstreetmap.josm.data.osm.visitor.Visitor;


/**
 * One node data, consisting of one world coordinate waypoint.
 *
 * @author imi
 */
public class Node extends OsmPrimitive {
	
	/**
	 * The coordinates of this node.
	 */
	public GeoPoint coor;

	/**
	 * The list of line segments, this node is part of.
	 */
	transient Collection<LineSegment> parentSegment = new LinkedList<LineSegment>();

	/**
	 * Returns a read-only list of all segments this node is in.
	 * @return A list of all segments. Readonly.
	 */
	public Collection<LineSegment> getParentSegments() {
		return Collections.unmodifiableCollection(parentSegment);
	}
	
	/**
	 * Return a list only this added.
	 */
	@Override
	public Collection<Node> getAllNodes() {
		LinkedList<Node> nodes = new LinkedList<Node>();
		nodes.add(this);
		return nodes;
	}

	@Override
	public void visit(Visitor visitor) {
		visitor.visit(this);
	}
}
