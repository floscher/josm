package org.openstreetmap.josm.data.osm;

import org.openstreetmap.josm.data.osm.visitor.Visitor;


/**
 * One track line segment consisting of a pair of nodes (start/end) 
 *
 * @author imi
 */
public class LineSegment extends OsmPrimitive {

	/**
	 * The starting node of the line segment
	 */
	public Node start;
	
	/**
	 * The ending node of the line segment
	 */
	public Node end;

	/**
	 * Create an line segment from the given starting and ending node
	 * @param start	Starting node of the line segment.
	 * @param end	Ending node of the line segment.
	 */
	public LineSegment(Node start, Node end) {
		this.start = start;
		this.end = end;
	}

	@Override
	public void visit(Visitor visitor) {
		visitor.visit(this);
	}
}	
