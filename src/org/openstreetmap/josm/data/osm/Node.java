package org.openstreetmap.josm.data.osm;

import java.util.Collection;
import java.util.LinkedList;

import org.openstreetmap.josm.data.GeoPoint;


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
	 * Nodes are equal when their coordinates are equal.
	 */
	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof Node))
			return false;
		Node n = (Node)obj;
		if (coor == null)
			return n.coor == null;
		return coor.equals(n.coor) && super.equals(obj);
	}

	/**
	 * Compute the hashcode from the OsmPrimitive's hash and the coor's hash.
	 */
	@Override
	public int hashCode() {
		return (coor == null ? 0 : coor.hashCode()) + super.hashCode();
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
	
	
}
