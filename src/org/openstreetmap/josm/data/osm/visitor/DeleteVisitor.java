/**
 */
package org.openstreetmap.josm.data.osm.visitor;

import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Key;
import org.openstreetmap.josm.data.osm.LineSegment;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.Track;

/**
 * Visitor, that removes the visited object from the dataset given at constructor.
 * 
 * Is not capable of removing keys.
 * 
 * @author imi
 */
public class DeleteVisitor implements Visitor {
	
	private final DataSet ds;
	
	public DeleteVisitor(DataSet ds) {
		this.ds = ds;
	}
	
	public void visit(Node n) {
		if (ds.nodes.remove(n))
			ds.deleted.add(n);
	}
	public void visit(LineSegment ls) {
		if (ds.lineSegments.remove(ls))
			ds.deleted.add(ls);
	}
	public void visit(Track t) {
		if (ds.tracks.remove(t))
			ds.deleted.add(t);
	}
	public void visit(Key k) {}
}