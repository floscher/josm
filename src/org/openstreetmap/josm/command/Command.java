package org.openstreetmap.josm.command;

import java.awt.Component;
import java.util.Collection;

import org.openstreetmap.josm.data.osm.OsmPrimitive;


/**
 * Classes implementing Command modify a dataset in a specific way. A command is
 * one atomic action on a specific dataset, such as move or delete.
 *
 * @author imi
 */
public interface Command {

	/**
	 * Executes the command on the dataset.
	 */
	void executeCommand();

	/**
	 * Give a description of the command as component to draw
	 */
	Component commandDescription();
	
	/**
	 * Fill in the changed data this command operates on (for sending to the server).
	 * Add to the lists, don't clear them.
	 * @param modified  The modified primitives
	 * @param deleted   The deleted primitives
	 * @param added		The added primitives
	 */
	void fillModifiedData(Collection<OsmPrimitive> modified,
			Collection<OsmPrimitive> deleted,
			Collection<OsmPrimitive> added);
}
