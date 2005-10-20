package org.openstreetmap.josm.command;

import java.awt.Component;


/**
 * Classes implementing Command modify a dataset in a specific way. A command is
 * one atomic action on a dataset, such as move or delete.
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
}
