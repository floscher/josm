package org.openstreetmap.josm.data.conflict;

import org.openstreetmap.josm.data.osm.OsmPrimitive;

public class DeleteConflict extends ConflictItem {

	@Override public boolean hasConflict(OsmPrimitive key, OsmPrimitive value) {
		return key.deleted != value.deleted;
	}

	@Override public String key() {
		return "deleted|deleted";
	}

	@Override protected String str(OsmPrimitive osm) {
		return osm.deleted ? "true" : "false";
	}

	@Override public void apply(OsmPrimitive target, OsmPrimitive other) {
		target.deleted = other.deleted;
	}
}
