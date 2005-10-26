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
	 * Add to the lists, don't clear them. The lists can be <code>null</code>
	 * in which case they are ignored.
	 * 
	 * @param modified  The modified primitives or <code>null</code>
	 * @param deleted   The deleted primitives or <code>null</code>
	 * @param added		The added primitives or <code>null</code>
	 */
	void fillModifiedData(Collection<OsmPrimitive> modified,
			Collection<OsmPrimitive> deleted,
			Collection<OsmPrimitive> added);
}
