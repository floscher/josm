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

	public Node(GeoPoint coor) {
		this.coor = coor;
	}

	@Override
	public void visit(Visitor visitor) {
		visitor.visit(this);
	}
	
	@Override
	public String toString() {
		return "{Node id="+id+",lat="+coor.lat+",lon="+coor.lon+"}";
	}

	@Override
	public void cloneFrom(OsmPrimitive osm) {
		super.cloneFrom(osm);
		GeoPoint g = ((Node)osm).coor;
		coor = new GeoPoint(g.lat, g.lon, g.x, g.y); //TODO: Make GeoPoint immutable!
	}
}
