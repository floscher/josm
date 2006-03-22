package org.openstreetmap.josm.data.osm;

import org.openstreetmap.josm.data.GeoPoint;
import org.openstreetmap.josm.data.osm.visitor.Visitor;


/**
 * One way line segment consisting of a pair of nodes (from/to) 
 *
 * @author imi
 */
public class LineSegment extends OsmPrimitive {

	/**
	 * The starting node of the line segment
	 */
	public Node from;
	
	/**
	 * The ending node of the line segment
	 */
	public Node to;

	/**
	 * If set to true, this object is incomplete, which means only the id
	 * and type is known (type is the objects instance class)
	 */
	public boolean incomplete;

	/**
	 * Create an line segment from the given starting and ending node
	 * @param from	Starting node of the line segment.
	 * @param to	Ending node of the line segment.
	 */
	public LineSegment(Node from, Node to) {
		this.from = from;
		this.to = to;
		incomplete = false;
	}

	public LineSegment(long id) {
		this.id = id;
		incomplete = true;
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
		if (incomplete || ls.incomplete)
			return false;
		GeoPoint s1 = from.coor;
		GeoPoint s2 = ls.from.coor;
		GeoPoint e1 = to.coor;
		GeoPoint e2 = ls.to.coor;
		return ((s1.equalsLatLon(s2) && e1.equalsLatLon(e2)) ||
				(s1.equalsLatLon(e2) && e1.equalsLatLon(s2)));
	}

	@Override
	public String toString() {
		String s = "{LineSegment id="+id;
		if (incomplete)
			return s+",incomplete}";
		return s+",from="+from+",to="+to+"}";
	}

	@Override
	public void cloneFrom(OsmPrimitive osm) {
		super.cloneFrom(osm);
		LineSegment ls = ((LineSegment)osm);
		from = ls.from;
		to = ls.to;
		incomplete = ls.incomplete;
	}
}	
