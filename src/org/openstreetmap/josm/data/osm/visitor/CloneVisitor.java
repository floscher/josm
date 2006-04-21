package org.openstreetmap.josm.data.osm.visitor;

import java.util.HashMap;
import java.util.Map;

import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Segment;
import org.openstreetmap.josm.data.osm.Way;

public class CloneVisitor implements Visitor {
	
	public Map<OsmPrimitive, OsmPrimitive> orig = new HashMap<OsmPrimitive, OsmPrimitive>();
	
	public void visit(Node n) {
		orig.put(n, new Node(n));
    }
	public void visit(Segment s) {
		orig.put(s, new Segment(s));
    }
	public void visit(Way w) {
		orig.put(w, new Way(w));
    }
}