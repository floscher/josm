package org.openstreetmap.josm.data.osm;

import org.openstreetmap.josm.data.GeoPoint;
import org.openstreetmap.josm.data.osm.visitor.Visitor;


/**
 * One way line segment consisting of a pair of nodes (start/end) 
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
	
	/**
	 * @return <code>true</code>, if the <code>ls</code> occupy
	 * exactly the same place as <code>this</code>.
	 */
	public boolean equalPlace(LineSegment ls) {
		if (equals(ls))
			return true;
		GeoPoint s1 = start.coor;
		GeoPoint s2 = ls.start.coor;
		GeoPoint e1 = end.coor;
		GeoPoint e2 = ls.end.coor;
		return ((s1.equalsLatLon(s2) && e1.equalsLatLon(e2)) ||
				(s1.equalsLatLon(e2) && e1.equalsLatLon(s2)));
	}
}	
