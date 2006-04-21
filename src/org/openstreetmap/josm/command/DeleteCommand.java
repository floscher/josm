package org.openstreetmap.josm.command;

import java.util.Collection;
import java.util.HashSet;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.visitor.CollectBackReferencesVisitor;

/**
 * A command to delete a number of primitives from the dataset.
 * @author imi
 */
public class DeleteCommand extends Command {

	/**
	 * The primitive that get deleted.
	 */
	final Collection<OsmPrimitive> data = new HashSet<OsmPrimitive>();

	public DeleteCommand(OsmPrimitive osm) {
		CollectBackReferencesVisitor v = new CollectBackReferencesVisitor(Main.ds);
		osm.visit(v);
		data.addAll(v.data);
		data.add(osm);
	}
	
	@Override public void executeCommand() {
		super.executeCommand();
		for (OsmPrimitive osm : data)
			osm.delete(true);
	}

	@Override public void fillModifiedData(Collection<OsmPrimitive> modified, Collection<OsmPrimitive> deleted, Collection<OsmPrimitive> added) {
		deleted.addAll(data);
	}
}
