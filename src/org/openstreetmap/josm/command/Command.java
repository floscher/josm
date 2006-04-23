package org.openstreetmap.josm.command;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map.Entry;

import javax.swing.tree.MutableTreeNode;

import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.visitor.CloneVisitor;


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
abstract public class Command {

	private CloneVisitor orig; 
	
	/**
	 * Executes the command on the dataset. This implementation will remember all
	 * primitives returned by fillModifiedData for restoring them on undo.
	 */
	public void executeCommand() {
		orig = new CloneVisitor();
		Collection<OsmPrimitive> all = new HashSet<OsmPrimitive>();
		fillModifiedData(all, all, all);
		for (OsmPrimitive osm : all)
			osm.visit(orig);
	}

	/**
	 * Undoes the command. 
	 * It can be assumed, that all objects are in the same state they were before.
	 * It can also be assumed that executeCommand was called exactly once before.
	 * 
	 * This implementation undoes all objects stored by a former call to executeCommand.
	 */
	public void undoCommand() {
		for (Entry<OsmPrimitive, OsmPrimitive> e : orig.orig.entrySet())
			e.getKey().cloneFrom(e.getValue());
	}

	/**
	 * Fill in the changed data this command operates on.
	 * Add to the lists, don't clear them.
	 * 
	 * @param modified  The modified primitives
	 * @param deleted   The deleted primitives
	 * @param added		The added primitives
	 */
	abstract public void fillModifiedData(Collection<OsmPrimitive> modified,
			Collection<OsmPrimitive> deleted,
			Collection<OsmPrimitive> added);

	abstract public MutableTreeNode description();
}
