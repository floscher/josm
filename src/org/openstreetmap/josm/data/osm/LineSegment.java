package org.openstreetmap.josm.data.osm;

import java.util.Collection;
import java.util.LinkedList;


/**
 * One track line segment consisting of a pair of nodes (start/end) 
 *
 * @author imi
 */
public class LineSegment extends OsmPrimitive {

	/**
	 * Create an line segment from the given starting and ending node
	 * @param start	Starting node of the line segment.
	 * @param end	Ending node of the line segment.
	 */
	public LineSegment(Node start, Node end) {
		this.start = start;
		this.end = end;
	}

	/**
	 * The starting node of the line segment
	 */
	public Node start;
	
	/**
	 * The ending node of the line segment
	 */
	public Node end;

	/**
	 * Return start and end in a list.
	 */
	@Override
	public Collection<Node> getAllNodes() {
		LinkedList<Node> nodes = new LinkedList<Node>();
		nodes.add(start);
		nodes.add(end);
		return nodes;
	}

	/**
	 * Line segments are equal, if their starting and ending node and their
	 * keys are equal.
	 */
	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof LineSegment))
			return false;
		return super.equals(obj) && 
			start.equals(((LineSegment)obj).start) &&
			end.equals(((LineSegment)obj).end);
	}

	@Override
	public int hashCode() {
		return super.hashCode() + start.hashCode() + end.hashCode();
	}
}	
