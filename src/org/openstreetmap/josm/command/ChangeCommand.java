package org.openstreetmap.josm.command;

import java.util.Collection;

import org.openstreetmap.josm.data.osm.OsmPrimitive;

public class ChangeCommand extends Command {

	private final OsmPrimitive osm;
	private final OsmPrimitive newOsm;

	public ChangeCommand(OsmPrimitive osm, OsmPrimitive newOsm) {
		this.osm = osm;
		this.newOsm = newOsm;
    }

	@Override public void executeCommand() {
	    super.executeCommand();
	    osm.cloneFrom(newOsm);
    }

	@Override public void fillModifiedData(Collection<OsmPrimitive> modified, Collection<OsmPrimitive> deleted, Collection<OsmPrimitive> added) {
		modified.add(osm);
    }
}
