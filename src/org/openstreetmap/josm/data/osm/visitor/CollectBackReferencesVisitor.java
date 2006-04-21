package org.openstreetmap.josm.data.osm.visitor;

import java.util.Collection;
import java.util.HashSet;

import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Segment;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Way;

/**
 * Helper that collect all segments a node is part of, all ways
 * a node or segment is part of and all areas a node is part of. 
 * 
 * Deleted objects are not collected.
 * 
 * @author imi
 */
public class CollectBackReferencesVisitor implements Visitor {

	private final DataSet ds;
	
	/**
	 * The result list of primitives stored here.
	 */
	public final Collection<OsmPrimitive> data = new HashSet<OsmPrimitive>();


	/**
	 * Construct a back reference counter.
	 * @param ds The dataset to operate on.
	 */
	public CollectBackReferencesVisitor(DataSet ds) {
		this.ds = ds;
	}
	
	public void visit(Node n) {
		for (Way t : ds.ways) {
			if (t.deleted)
				continue;
			for (Segment ls : t.segments) {
				if (ls.incomplete)
					continue;
				if (ls.from == n || ls.to == n) {
					data.add(t);
					break;
				}
			}
		}
		for (Segment ls : ds.segments) {
			if (ls.deleted || ls.incomplete)
				continue;
			if (ls.from == n || ls.to == n)
				data.add(ls);
		}
	}
	public void visit(Segment ls) {
		for (Way t : ds.ways) {
			if (t.deleted)
				continue;
			if (t.segments.contains(ls))
				data.add(t);
		}
	}
	public void visit(Way t) {}
}