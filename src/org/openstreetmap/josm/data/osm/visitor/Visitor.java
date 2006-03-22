package org.openstreetmap.josm.data.osm.visitor;

import org.openstreetmap.josm.data.osm.LineSegment;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.Way;

/**
 * Implementation of the visitor scheme. Every OsmPrimitive can be visited by
 * several different visitors.
 * 
 * @author imi
 */
public interface Visitor {
	void visit(Node n);
	void visit(LineSegment ls);
	void visit(Way w);
}
