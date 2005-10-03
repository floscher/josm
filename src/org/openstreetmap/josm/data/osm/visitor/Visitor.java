package org.openstreetmap.josm.data.osm.visitor;

import org.openstreetmap.josm.data.osm.Key;
import org.openstreetmap.josm.data.osm.LineSegment;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.Track;

/**
 * Implementation of the visitor scheme. Every OsmPrimitive can be visited by
 * several different visitors.
 * 
 * @author imi
 */
abstract public class Visitor {
	public void visit(Node n) {}
	public void visit(LineSegment ls) {}
	public void visit(Track t) {}
	public void visit(Key k) {}
}
