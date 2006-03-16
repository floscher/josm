package org.openstreetmap.josm.data.osm.visitor;

import java.util.Collection;
import java.util.HashSet;

import org.openstreetmap.josm.data.osm.LineSegment;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Way;

/**
 * Collect all nodes a specific osm primitive has.
 * 
 * @author imi
 */
public class AllNodesVisitor implements Visitor {

	/**
	 * The resulting nodes collected so far.
	 */
	public Collection<Node> nodes = new HashSet<Node>();

	/**
	 * Nodes have only itself as nodes.
	 */
	public void visit(Node n) {
		nodes.add(n);
	}

	/**
	 * Line segments have exactly two nodes: start and end.
	 */
	public void visit(LineSegment ls) {
		nodes.add(ls.start);
		nodes.add(ls.end);
	}

	/**
	 * Ways have all nodes from their line segments.
	 */
	public void visit(Way t) {
		for (LineSegment ls : t.segments) {
			nodes.add(ls.start);
			nodes.add(ls.end);
		}
	}

	/**
	 * @return All nodes the given primitive has.
	 */
	public static Collection<Node> getAllNodes(OsmPrimitive osm) {
		AllNodesVisitor v = new AllNodesVisitor();
		osm.visit(v);
		return v.nodes;
	}
}
