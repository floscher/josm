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
	private final boolean includeReferences;
	
	public AddVisitor(DataSet ds, boolean includeReferences) {
		this.ds = ds;
		this.includeReferences = includeReferences;
	}
	
	public AddVisitor(DataSet ds) {
		this(ds, false);
	}
	
	public void visit(Node n) {
		if (!includeReferences || !ds.nodes.contains(n))
			ds.nodes.add(n);
	}
	public void visit(Segment s) {
		ds.segments.add(s);
		if (includeReferences && !s.incomplete) {
			if (!ds.nodes.contains(s.from))
				s.from.visit(this);
			if (!ds.nodes.contains(s.to))
				s.to.visit(this);
		}
	}
	public void visit(Way w) {
		ds.ways.add(w);
		if (includeReferences)
			for (Segment s : w.segments)
				if (!ds.segments.contains(s))
					s.visit(this);
	}
}