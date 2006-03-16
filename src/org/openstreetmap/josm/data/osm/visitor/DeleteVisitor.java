/**
 */
package org.openstreetmap.josm.data.osm.visitor;

import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.LineSegment;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.Way;

/**
 * Visitor, that adds the visited object to the dataset given at constructor.
 * 
 * Is not capable of adding keys.
 * 
 * @author imi
 */
public class DeleteVisitor implements Visitor {
	
	private final DataSet ds;
	
	public DeleteVisitor(DataSet ds) {
		this.ds = ds;
	}
	
	public void visit(Node n) {
		ds.nodes.remove(n);
	}
	public void visit(LineSegment ls) {
		ds.lineSegments.remove(ls);
	}
	public void visit(Way t) {
		ds.waies.remove(t);
	}
}