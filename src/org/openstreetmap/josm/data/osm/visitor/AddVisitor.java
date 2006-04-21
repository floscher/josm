/**
 */
package org.openstreetmap.josm.data.osm.visitor;

import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Segment;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.Way;

/**
 * Visitor, that adds the visited object to the dataset given at constructor.
 * 
 * Is not capable of adding keys.
 * 
 * @author imi
 */
public class AddVisitor implements Visitor {
	
	private final DataSet ds;
	
	public AddVisitor(DataSet ds) {
		this.ds = ds;
	}
	
	public void visit(Node n) {
		ds.nodes.add(n);
	}
	public void visit(Segment ls) {
		ds.segments.add(ls);
	}
	public void visit(Way t) {
		ds.ways.add(t);
	}
}