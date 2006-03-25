package org.openstreetmap.josm.actions.mapmode;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;

import javax.swing.JOptionPane;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.command.DeleteCommand;
import org.openstreetmap.josm.command.SequenceCommand;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.visitor.CollectBackReferencesVisitor;
import org.openstreetmap.josm.gui.MapFrame;

/**
 * An action that enables the user to delete nodes and other objects.
 *
 * The user can click on an object, which get deleted if possible. When Ctrl is 
 * pressed when releasing the button, the objects and all its references are 
 * deleted. The exact definition of "all its references" are in 
 * @see #deleteWithReferences(OsmPrimitive)
 *
 * Pressing Alt will select the way instead of a line segment, as usual.
 * 
 * If the user did not press Ctrl and the object has any references, the user
 * is informed and nothing is deleted.
 *
 * If the user enters the mapmode and any object is selected, all selected
 * objects that can be deleted will.
 * 
 * @author imi
 */
public class DeleteAction extends MapMode {

	/**
	 * Construct a new DeleteAction. Mnemonic is the delete - key.
	 * @param mapFrame The frame this action belongs to.
	 */
	public DeleteAction(MapFrame mapFrame) {
		super("Delete", "delete", "Delete nodes, streets or segments.", "Delete", KeyEvent.VK_DELETE, mapFrame);
	}

	@Override
	public void registerListener() {
		super.registerListener();
		mv.addMouseListener(this);
	}

	@Override
	public void unregisterListener() {
		super.unregisterListener();
		mv.removeMouseListener(this);
	}

	
	@Override
	public void actionPerformed(ActionEvent e) {
		super.actionPerformed(e);
		boolean ctrl = (e.getModifiers() & ActionEvent.CTRL_MASK) != 0;
		if (ctrl)
			deleteWithReferences(Main.main.ds.getSelected());
		else
			delete(Main.main.ds.getSelected(), false);
		mv.repaint();
	}

	/**
	 * If user clicked with the left button, delete the nearest object.
	 * position.
	 */
	@Override
	public void mouseClicked(MouseEvent e) {
		if (e.getButton() != MouseEvent.BUTTON1)
			return;
		
		OsmPrimitive sel = mv.getNearest(e.getPoint(), (e.getModifiersEx() & MouseEvent.ALT_DOWN_MASK) != 0);
		if (sel == null)
			return;

		if ((e.getModifiersEx() & MouseEvent.CTRL_DOWN_MASK) != 0)
			deleteWithReferences(Collections.singleton(sel));
		else
			delete(Collections.singleton(sel), true);

		mv.repaint();
	}

	/**
	 * Delete the primitives and everything they references.
	 * 
	 * If a node is deleted, the node and all line segments, ways and areas
	 * the node is part of are deleted as well.
	 * 
	 * If a line segment is deleted, all ways the line segment is part of 
	 * are deleted as well. No nodes are deleted.
	 * 
	 * If a way is deleted, only the way and no line segments or nodes are 
	 * deleted.
	 * 
	 * If an area is deleted, only the area gets deleted.
	 * 
	 * @param selection The list of all object to be deleted.
	 */
	private void deleteWithReferences(Collection<OsmPrimitive> selection) {
		Collection<Command> deleteCommands = new LinkedList<Command>();
		for (OsmPrimitive osm : selection)
			deleteCommands.add(new DeleteCommand(Main.main.ds, osm));
		if (!deleteCommands.isEmpty())
			mv.editLayer().add(new SequenceCommand(deleteCommands));
	}

	/**
	 * Try to delete all given primitives. If a primitive is
	 * used somewhere and that "somewhere" is not going to be deleted,
	 * inform the user and do not delete.
	 * 
	 * @param selection The objects to delete.
	 * @param msgBox Whether a message box for errors should be shown
	 */
	private void delete(Collection<OsmPrimitive> selection, boolean msgBox) {
		Collection<Command> deleteCommands = new LinkedList<Command>();
		for (OsmPrimitive osm : selection) {
			CollectBackReferencesVisitor v = new CollectBackReferencesVisitor(Main.main.ds);
			osm.visit(v);
			if (!selection.containsAll(v.data)) {
				if (msgBox)
					JOptionPane.showMessageDialog(Main.main, "This object is in use.");
			} else
				deleteCommands.add(new DeleteCommand(Main.main.ds, osm));
		}
		if (!deleteCommands.isEmpty())
			mv.editLayer().add(new SequenceCommand(deleteCommands));
	}
}
