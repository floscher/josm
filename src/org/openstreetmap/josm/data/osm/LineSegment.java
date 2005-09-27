package org.openstreetmap.josm.data.osm;


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
}	
