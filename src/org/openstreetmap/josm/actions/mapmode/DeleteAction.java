package org.openstreetmap.josm.actions.mapmode;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;

import javax.swing.JOptionPane;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.command.CombineAndDeleteCommand;
import org.openstreetmap.josm.command.DeleteCommand;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.LineSegment;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Track;
import org.openstreetmap.josm.gui.MapFrame;

/**
 * An action that enables the user to delete nodes and other objects.
 * 
 * The user can click on an object, which get deleted if possible. When Ctrl is 
 * pressed when releasing the button, the objects and all its references are 
 * deleted as well. The exact definition of "all its references" are in 
 * @see #deleteWithReferences(OsmPrimitive)
 *
 * Pressing Alt will select the track instead of a line segment, as usual.
 *
 * If the user presses Ctrl, no combining is possible. Otherwise, DeleteAction 
 * tries to combine the referencing objects as follows:
 *
 * If a node is part of exactly two line segments, the two line segments are 
 * combined into one. The first line segment spans now to the end of the 
 * second and the second line segment gets deleted.
 * 
 * Combining is only possible, if both objects that should be combined have no
 * key with a different property value. The remaining keys are merged together.
 * 
 * If a node is part of an area with more than 3 nodes, the node is removed from 
 * the area and the area has now one fewer node.
 * 
 * If combining fails, the node has still references and the user did not hold
 * Ctrl down, the deleting fails, the action informs the user and nothing is
 * deleted.
 * 
 * 
 * If the user enters the mapmode and any object is selected, all selected
 * objects that can be deleted will. Combining applies to the selected objects.
 * 
 * @author imi
 */
public class DeleteAction extends MapMode {

	/**
	 * Construct a new DeleteAction. Mnemonic is the delete - key.
	 * @param mapFrame The frame this action belongs to.
	 */
	public DeleteAction(MapFrame mapFrame) {
		super("Delete", "delete", "Delete nodes, streets or areas.", KeyEvent.VK_D, mapFrame);
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
		Collection<OsmPrimitive> selection = Main.main.ds.getSelected();
		
		int selSize = 0;
		// loop as long as the selection size changes
		while(selSize != selection.size()) {
			selSize = selection.size();

			for (Iterator<OsmPrimitive> it = selection.iterator(); it.hasNext();) {
				OsmPrimitive osm = it.next();
				if (ctrl) {
					deleteWithReferences(osm);
					it.remove();
				} else {
					if (delete(osm, false))
						it.remove();
				}
			}
		}
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
			deleteWithReferences(sel);
		else
			delete(sel, true);

		mv.repaint();
	}

	/**
	 * Delete the primitive and everything it references or beeing directly 
	 * referenced by, except of nodes which are deleted only if passed 
	 * directly or become unreferenced while deleting other objects.
	 *
	 * Nothing is combined as in @see #delete(OsmPrimitive).
	 * 
	 * Example (A is a track of line segment a and b. z is a node):
	 *
	 *              A
	 *   B   x        z
	 *  -----*--------+-----
	 *       |     a      b
	 *       |C
	 *       |
	 *       *y
	 *       
	 * If you delete C, C and y (since now unreferenced) gets deleted.
	 * If you delete A, then A, a, b and z (since now unreferenced) gets deleted.
	 * If you delete y, then y and C gets deleted.
	 * TODO If you delete x, then a,B,C and x gets deleted. A now consist of b only.
	 * If you delete a or b, then A, a, b and z gets deleted.
	 * 
	 * @param osm The object to delete.
	 */
	private void deleteWithReferences(OsmPrimitive osm) {
		// collect all tracks, areas and line segments that should be deleted
		ArrayList<Track> tracksToDelete = new ArrayList<Track>();
		ArrayList<LineSegment> lineSegmentsToDelete = new ArrayList<LineSegment>();

		if (osm instanceof Node) {
			// delete any track and line segment the node is in.
			for (Track t : Main.main.ds.tracks)
				for (LineSegment ls : t.segments)
					if (ls.start == osm || ls.end == osm)
						tracksToDelete.add(t);
			for (LineSegment ls : Main.main.ds.lineSegments)
				if (ls.start == osm || ls.end == osm)
					lineSegmentsToDelete.add(ls);
				
		} else if (osm instanceof LineSegment) {
			LineSegment lineSegment = (LineSegment)osm;
			lineSegmentsToDelete.add(lineSegment);
			for (Track t : Main.main.ds.tracks)
				for (LineSegment ls : t.segments)
					if (lineSegment == ls)
						tracksToDelete.add(t);
		} else if (osm instanceof Track) {
			tracksToDelete.add((Track)osm);
		}
		// collect all nodes, that could be unreferenced after deletion
		ArrayList<Node> checkUnreferencing = new ArrayList<Node>();
		for (Track t : tracksToDelete) {
			for (LineSegment ls : t.segments) {
				checkUnreferencing.add(ls.start);
				checkUnreferencing.add(ls.end);
			}
		}
		for (LineSegment ls : lineSegmentsToDelete) {
			checkUnreferencing.add(ls.start);
			checkUnreferencing.add(ls.end);
		}

		Collection<OsmPrimitive> deleteData = new LinkedList<OsmPrimitive>();
		deleteData.addAll(tracksToDelete);
		deleteData.addAll(lineSegmentsToDelete);
		// removing all unreferenced nodes
		for (Node n : checkUnreferencing)
			if (!isReferenced(n))
				deleteData.add(n);
		// now, all references are killed. Delete the node (if it was a node)
		if (osm instanceof Node)
			deleteData.add(osm);

		mv.editLayer().add(new DeleteCommand(Main.main.ds, deleteData));
	}

	/**
	 * Try to delete the given primitive. If the primitive is a node and
	 * used somewhere, try to combine the references to make the node unused.
	 * If this fails, inform the user and do not delete.
	 * 
	 * @param osm The object to delete.
	 * @param msgBox Whether a message box for errors should be shown
	 * @return <code>true</code> if the object could be deleted
	 */
	private boolean delete(OsmPrimitive osm, boolean msgBox) {
		if (osm instanceof Node && isReferenced((Node)osm))
			return combineAndDelete((Node)osm, msgBox);
		Collection<OsmPrimitive> c = new LinkedList<OsmPrimitive>();
		c.add(osm);
		mv.editLayer().add(new DeleteCommand(Main.main.ds, c));
		return true;
	}

	
	/**
	 * Return <code>true</code>, if the node is used by anything in the map.
	 * @param n The node to check.
	 * @return Whether the node is used by a track or area.
	 */
	private boolean isReferenced(Node n) {
		for (LineSegment ls : Main.main.ds.lineSegments)
			if (ls.start == n || ls.end == n)
				return true;
		// TODO areas
		return false;
	}

	/**
	 * Try to combine all objects when deleting the node n. If combining is not
	 * possible, return an error string why. Otherwise, combine it and return 
	 * <code>null</code>.
	 *
	 * @param n The node that is going to be deleted.
	 * @param msgBox Whether a message box should be displayed in case of problems
	 * @return <code>true</code> if combining suceded.
	 */
	private boolean combineAndDelete(Node n, boolean msgBox) {
		DataSet ds = Main.main.ds;
		Collection<LineSegment> lineSegmentsUsed = new HashSet<LineSegment>();
		for (LineSegment ls : ds.lineSegments)
			if (ls.start == n || ls.end == n)
				lineSegmentsUsed.add(ls);

		if (lineSegmentsUsed.isEmpty())
			// should not be called
			throw new IllegalStateException();
		
		if (lineSegmentsUsed.size() == 1) {
			if (msgBox)
				JOptionPane.showMessageDialog(Main.main, "Node used by a line segment. Delete this first.");
			return false;
		}
			
		if (lineSegmentsUsed.size() > 2) {
			if (msgBox)
				JOptionPane.showMessageDialog(Main.main, "Node used by more than two line segments. Delete them first.");
			return false;
		}
		
		Iterator<LineSegment> it = lineSegmentsUsed.iterator();
		LineSegment first = it.next();
		LineSegment second = it.next();
		
		// wrong direction?
		if (first.start == second.end) {
			LineSegment t = first;
			first = second;
			second = t;
		}
		
		// combinable?
		if (first.end != second.start || !first.end.keyPropertiesMergable(second.start)) {
			if (msgBox)
				JOptionPane.showMessageDialog(Main.main, "Node used by line segments that cannot be combined.");
			return false;
		}

		// Ok, we can combine. Do it.
		mv.editLayer().add(new CombineAndDeleteCommand(ds, first, second));
		return true;
	}
}
