package org.openstreetmap.josm.data.conflict;

import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Segment;
import org.openstreetmap.josm.data.osm.Way;

public class SegmentConflict extends ConflictItem {
	
	@Override public boolean hasConflict(OsmPrimitive key, OsmPrimitive value) {
		return key instanceof Way && !((Way)key).segments.equals(((Way)value).segments);
	}
	
	@Override protected String str(OsmPrimitive osm) {
		if (!(osm instanceof Way))
			return null;
		String s = "";
		for (Segment ls : ((Way)osm).segments)
			s += ls.id + ",";
		return s.equals("") ? "<html><i>&lt;none&gt;</i></html>" : s.substring(0, s.length()-1);
	}
	
	@Override public String key() {
		return "way|segments";
	}
	
	@Override public void apply(OsmPrimitive target, OsmPrimitive other) {
		if (!(target instanceof Way))
			return;
		((Way)target).segments.clear();
		((Way)target).segments.addAll(((Way)other).segments);
    }
}