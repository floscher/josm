package org.openstreetmap.josm.actions.mapmode;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;

import javax.swing.JOptionPane;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.command.DeleteCommand;
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
 * Pressing Alt will select the way instead of a segment, as usual.
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

	@Override public void enterMode() {
		super.enterMode();
		Main.map.mapView.addMouseListener(this);
	}

	@Override public void exitMode() {
		super.exitMode();
		Main.map.mapView.removeMouseListener(this);
	}

	
	@Override public void actionPerformed(ActionEvent e) {
		super.actionPerformed(e);
		boolean ctrl = (e.getModifiers() & ActionEvent.CTRL_MASK) != 0;
		if (ctrl)
			deleteWithReferences(Main.ds.getSelected());
		else
			delete(Main.ds.getSelected(), false);
		Main.map.repaint();
	}

	/**
	 * If user clicked with the left button, delete the nearest object.
	 * position.
	 */
	@Override public void mouseClicked(MouseEvent e) {
		if (e.getButton() != MouseEvent.BUTTON1)
			return;
		
		OsmPrimitive sel = Main.map.mapView.getNearest(e.getPoint(), (e.getModifiersEx() & MouseEvent.ALT_DOWN_MASK) != 0);
		if (sel == null)
			return;

		if ((e.getModifiersEx() & MouseEvent.CTRL_DOWN_MASK) != 0)
			deleteWithReferences(Collections.singleton(sel));
		else
			delete(Collections.singleton(sel), true);

		Main.map.mapView.repaint();
	}

	/**
	 * Delete the primitives and everything they references.
	 * 
	 * If a node is deleted, the node and all segments, ways and areas
	 * the node is part of are deleted as well.
	 * 
	 * If a segment is deleted, all ways the segment is part of 
	 * are deleted as well. No nodes are deleted.
	 * 
	 * If a way is deleted, only the way and no segments or nodes are 
	 * deleted.
	 * 
	 * If an area is deleted, only the area gets deleted.
	 * 
	 * @param selection The list of all object to be deleted.
	 */
	private void deleteWithReferences(Collection<OsmPrimitive> selection) {
		CollectBackReferencesVisitor v = new CollectBackReferencesVisitor(Main.ds);
		for (OsmPrimitive osm : selection)
			osm.visit(v);
		v.data.addAll(selection);
		if (!v.data.isEmpty())
			Main.main.editLayer().add(new DeleteCommand(v.data));
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
		Collection<OsmPrimitive> del = new HashSet<OsmPrimitive>();
		for (OsmPrimitive osm : selection) {
			CollectBackReferencesVisitor v = new CollectBackReferencesVisitor(Main.ds);
			osm.visit(v);
			if (!selection.containsAll(v.data)) {
				if (msgBox) {
					JOptionPane.showMessageDialog(Main.parent, "This object is in use.");
					return;
				}
			} else {
				del.addAll(v.data);
				del.add(osm);
			}
		}
		if (!del.isEmpty())
			Main.main.editLayer().add(new DeleteCommand(del));
	}
}
