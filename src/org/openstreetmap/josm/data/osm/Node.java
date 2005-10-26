package org.openstreetmap.josm.data.osm;

import org.openstreetmap.josm.data.GeoPoint;
import org.openstreetmap.josm.data.osm.visitor.Visitor;


/**
 * One node data, consisting of one world coordinate waypoint.
 *
 * @author imi
 */
public class Node extends OsmPrimitive {
	
	/**
	 * The coordinates of this node.
	 */
	public GeoPoint coor;

	@Override
	public void visit(Visitor visitor) {
		visitor.visit(this);
	}
}
