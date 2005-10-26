package org.openstreetmap.josm.data.osm.visitor;

import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.osm.Key;
import org.openstreetmap.josm.data.osm.LineSegment;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.Track;

/**
 * Calculates the total bounding rectangle of a serie of OsmPrimitives.
 * @author imi
 */
public class BoundingVisitor implements Visitor {

	/**
	 * The bounding rectangle of all primitives visited so far.
	 */
	public Bounds bounds;

	/**
	 * Calculate regarding lat/lon or x/y?
	 */
	public static enum Type {LATLON, XY};
	private Type type;
	
	
	public BoundingVisitor(Type type) {
		this.type = type;
	}
	

	public void visit(Node n) {
		if (bounds == null)
			bounds = new Bounds(n.coor.clone(), n.coor.clone());
		else {
			if (type == Type.LATLON) {
				bounds.min.lat = Math.min(bounds.min.lat, n.coor.lat);
				bounds.min.lon = Math.min(bounds.min.lon, n.coor.lon);
				bounds.max.lat = Math.max(bounds.max.lat, n.coor.lat);
				bounds.max.lon = Math.max(bounds.max.lon, n.coor.lon);
			} else {
				bounds.min.x = Math.min(bounds.min.x, n.coor.x);
				bounds.min.y = Math.min(bounds.min.y, n.coor.y);
				bounds.max.x = Math.max(bounds.max.x, n.coor.x);
				bounds.max.y = Math.max(bounds.max.y, n.coor.y);
			}
		}
	}

	public void visit(LineSegment ls) {
		visit(ls.start);
		visit(ls.end);
	}

	public void visit(Track t) {
		for (LineSegment ls : t.segments)
			visit(ls);
	}

	public void visit(Key k) {
		// do nothing
	}
}

