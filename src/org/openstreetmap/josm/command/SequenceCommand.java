package org.openstreetmap.josm.command;

import java.util.Collection;

import org.openstreetmap.josm.data.osm.OsmPrimitive;

/**
 * A command consisting of a sequenz of other commands. Executes the other commands
 * and undo them in reverse order.
 * @author imi
 */
public class SequenceCommand implements Command {

	/**
	 * The command sequenz to be executed.
	 */
	private Command[] sequenz;

	/**
	 * Create the command by specifying the list of commands to execute.
	 * @param sequenz The sequenz that should be executed.
	 */
	public SequenceCommand(Collection<Command> sequenz) {
		this.sequenz = new Command[sequenz.size()];
		this.sequenz = sequenz.toArray(this.sequenz);
	}
	
	public void executeCommand() {
		for (Command c : sequenz)
			c.executeCommand();
	}

	public void undoCommand() {
		for (int i = sequenz.length-1; i >= 0; --i)
			sequenz[i].undoCommand();
	}

	public void fillModifiedData(Collection<OsmPrimitive> modified,
			Collection<OsmPrimitive> deleted, Collection<OsmPrimitive> added) {
		for (Command c : sequenz)
			c.fillModifiedData(modified, deleted, added);
	}

}
