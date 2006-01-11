package org.openstreetmap.josm.command;

import java.util.Collection;
import java.util.HashSet;

import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.visitor.CollectBackReferencesVisitor;

/**
 * A command to delete a number of primitives from the dataset.
 * @author imi
 */
public class DeleteCommand implements Command {

	/**
	 * The dataset this command operates on.
	 */
	final DataSet ds;
	/**
	 * The primitive that get deleted.
	 */
	final Collection<OsmPrimitive> data = new HashSet<OsmPrimitive>();

	public DeleteCommand(DataSet ds, OsmPrimitive osm) {
		this.ds = ds;
		CollectBackReferencesVisitor v = new CollectBackReferencesVisitor(ds);
		osm.visit(v);
		data.addAll(v.data);
		data.add(osm);
	}
	
	public void executeCommand() {
		for (OsmPrimitive osm : data)
			osm.setDeleted(true);
	}

	public void undoCommand() {
		for (OsmPrimitive osm : data)
			osm.setDeleted(false);
	}

	public void fillModifiedData(Collection<OsmPrimitive> modified, Collection<OsmPrimitive> deleted, Collection<OsmPrimitive> added) {
		deleted.addAll(data);
	}
}
