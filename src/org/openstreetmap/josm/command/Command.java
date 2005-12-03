package org.openstreetmap.josm.command;

import java.awt.Component;
import java.util.Collection;

import org.openstreetmap.josm.data.osm.OsmPrimitive;


/**
 * Classes implementing Command modify a dataset in a specific way. A command is
 * one atomic action on a specific dataset, such as move or delete.
 *
 * Remember, that the command must be executable and undoable, even if the 
 * Main.main.ds has changed, so the command must save the dataset it operates on
 * if necessary.
 *
 * @author imi
 */
public interface Command {

	/**
	 * Executes the command on the dataset.
	 */
	void executeCommand();

	/**
	 * Undoes the command. 
	 * It can be assumed, that all objects are in the same state they were before.
	 * It can also be assumed that executeCommand was called exactly once before.
	 */
	void undoCommand();
	
	/**
	 * Give a description of the command as component to draw
	 */
	Component commandDescription();
	
	/**
	 * Fill in the changed data this command operates on.
	 * Add to the lists, don't clear them.
	 * 
	 * @param modified  The modified primitives
	 * @param deleted   The deleted primitives
	 * @param added		The added primitives
	 */
	void fillModifiedData(Collection<OsmPrimitive> modified,
			Collection<OsmPrimitive> deleted,
			Collection<OsmPrimitive> added);
}
