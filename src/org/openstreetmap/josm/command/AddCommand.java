package org.openstreetmap.josm.command;

import java.util.Collection;

import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.visitor.AddVisitor;
import org.openstreetmap.josm.data.osm.visitor.DeleteVisitor;

/**
 * A command that adds an osm primitive to a dataset. Keys cannot be added this
 * way. Use ChangeKeyValueCommand instead.
 * 
 * @author imi
 */
public class AddCommand extends Command {

	/**
	 * The dataset this command operates on.
	 */
	private DataSet ds;

	/**
	 * The primitive to add to the dataset.
	 */
	private final OsmPrimitive osm;

	/**
	 * Create the command and specify the element to add.
	 */
	public AddCommand(DataSet ds, OsmPrimitive osm) {
		this.ds = ds;
		this.osm = osm;
	}

	@Override public void executeCommand() {
		osm.visit(new AddVisitor(ds));
	}

	@Override public void undoCommand() {
		osm.visit(new DeleteVisitor(ds));
	}

	@Override public void fillModifiedData(Collection<OsmPrimitive> modified, Collection<OsmPrimitive> deleted, Collection<OsmPrimitive> added) {
		added.add(osm);
	}
}
