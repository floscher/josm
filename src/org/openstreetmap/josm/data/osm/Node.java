package org.openstreetmap.josm.data.osm;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.osm.visitor.Visitor;


/**
 * One node data, consisting of one world coordinate waypoint.
 *
 * @author imi
 */
public class Node extends OsmPrimitive {
	
	public LatLon coor;
	public EastNorth eastNorth;

	public Node(LatLon latlon) {
		this.coor = latlon;
		eastNorth = Main.pref.getProjection().latlon2eastNorth(latlon);
	}

	@Override
	public void visit(Visitor visitor) {
		visitor.visit(this);
	}
	
	@Override
	public String toString() {
		return "{Node id="+id+",lat="+coor.lat()+",lon="+coor.lon()+"}";
	}

	@Override
	public void cloneFrom(OsmPrimitive osm) {
		super.cloneFrom(osm);
		coor = ((Node)osm).coor;
		eastNorth = ((Node)osm).eastNorth;
	}
}
