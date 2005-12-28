/**
 */
package org.openstreetmap.josm.data.osm.visitor;

import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Key;
import org.openstreetmap.josm.data.osm.LineSegment;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.Track;

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
		n.setDeleted(false);
	}
	public void visit(LineSegment ls) {
		ds.lineSegments.add(ls);
		ls.setDeleted(false);
	}
	public void visit(Track t) {
		ds.tracks.add(t);
		t.setDeleted(false);
	}
	public void visit(Key k) {}
}