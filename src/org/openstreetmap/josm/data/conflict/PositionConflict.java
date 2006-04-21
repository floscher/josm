package org.openstreetmap.josm.data.conflict;

import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;

public class PositionConflict extends ConflictItem {
	
	@Override public boolean hasConflict(OsmPrimitive key, OsmPrimitive value) {
		return key instanceof Node && !((Node)key).coor.equals(((Node)value).coor);
	}
	
	@Override protected String str(OsmPrimitive osm) {
		return osm instanceof Node ? ((Node)osm).coor.lat()+", "+((Node)osm).coor.lon() : null;
	}
	
	@Override public String key() {
		return "node|position";
	}
	
	@Override public void apply(OsmPrimitive target, OsmPrimitive other) {
		if (target instanceof Node) {
			((Node)target).coor = ((Node)other).coor;
			((Node)target).eastNorth = ((Node)other).eastNorth;
		}
    }
}