package org.openstreetmap.josm.data.conflict;

import java.util.Collection;
import java.util.Map;

import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.gui.ConflictResolver.Resolution;

public abstract class ConflictItem {

	public String my, their;
	public Resolution resolution = null;

	public final void initialize(Map<OsmPrimitive,OsmPrimitive> conflicts) {
		my = collectStr(conflicts.keySet());
		their = collectStr(conflicts.values());
	}

	abstract public boolean hasConflict(OsmPrimitive key, OsmPrimitive value);
	abstract protected String str(OsmPrimitive osm);
	abstract public String key();
	abstract public void apply(OsmPrimitive target, OsmPrimitive other);

	protected final String collectStr(Collection<OsmPrimitive> c) {
		String value = null;
		for (OsmPrimitive osm : c) {
			String v = str(osm);
			if (value == null)
				value = v;
			else if (!value.equals(v)) {
				value = "<html><i>&lt;different&gt;</i></html>";
				break;
			}
		}
		return value == null ? "" : value;
	}
}