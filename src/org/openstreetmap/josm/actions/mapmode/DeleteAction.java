package org.openstreetmap.josm.actions.mapmode;

import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;

import javax.swing.JOptionPane;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.command.CombineAndDeleteCommand;
import org.openstreetmap.josm.command.DeleteCommand;
import org.openstreetmap.josm.command.CombineAndDeleteCommand.LineSegmentCombineEntry;
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
 * If a node is part of exactly two line segments from a track, the two line 
 * segments are combined into one. The first line segment spans now to the end 
 * of the second and the second line segment gets deleted. This is checked for
 * every track.
 * 
 * If a node is the end of the ending line segment of one track and the start of
 * exactly one other tracks start segment, the tracks are combined into one track,
 * deleting the second track and keeping the first one. The ending line segment 
 * of the fist track is combined with the starting line segment of the second 
 * track.
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
 * objects get deleted. Combining applies to the selected objects.
 * 
 * @author imi
 */
public class DeleteAction extends MapMode {

	/**
	 * Construct a new DeleteAction. Mnemonic is the delete - key.
	 * @param mapFrame The frame this action belongs to.
	 */
	public DeleteAction(MapFrame mapFrame) {
		super("Delete", "delete", "Delete nodes, streets or areas.", KeyEvent.VK_DELETE, mapFrame);
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
			delete(sel);

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
		// collect all tracks, areas and pending line segments that should be deleted
		ArrayList<Track> tracksToDelete = new ArrayList<Track>();
		ArrayList<LineSegment> lineSegmentsToDelete = new ArrayList<LineSegment>();

		if (osm instanceof Node) {
			// delete any track and line segment the node is in.
			for (Track t : Main.main.ds.tracks)
				for (LineSegment ls : t.segments)
					if (ls.start == osm || ls.end == osm)
						tracksToDelete.add(t);
			for (LineSegment ls : Main.main.ds.pendingLineSegments)
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
		
		mv.editLayer().add(new DeleteCommand(deleteData));
	}

	/**
	 * Try to delete the given primitive. If the primitive is a node and
	 * used somewhere, try to combine the references to make the node unused.
	 * If this fails, inform the user and do not delete.
	 * 
	 * @param osm The object to delete.
	 */
	private void delete(OsmPrimitive osm) {
		if (osm instanceof Node && isReferenced((Node)osm)) {
			combineAndDelete((Node)osm);
			return;
		}
		Collection<OsmPrimitive> c = new LinkedList<OsmPrimitive>();
		c.add(osm);
		mv.editLayer().add(new DeleteCommand(c));
	}

	
	/**
	 * Return <code>true</code>, if the node is used by anything in the map.
	 * @param n The node to check.
	 * @return Whether the node is used by a track or area.
	 */
	private boolean isReferenced(Node n) {
		for (Track t : Main.main.ds.tracks)
			for (LineSegment ls : t.segments)
				if (ls.start == n || ls.end == n)
					return true;
		for (LineSegment ls : Main.main.ds.pendingLineSegments)
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
	 * @return <code>null</code> if combining suceded or an error string if there
	 * 		are problems combining the node.
	 */
	private void combineAndDelete(Node n) {
		// first, check for pending line segments
		for (LineSegment ls : Main.main.ds.pendingLineSegments)
			if (n == ls.start || n == ls.end) {
				JOptionPane.showMessageDialog(Main.main, "Node used by a line segment which is not part of any track. Remove this first.");
				return;
			}
		
		// These line segments must be combined within the track combining
		ArrayList<LineSegment> pendingLineSegmentsForTrack = new ArrayList<LineSegment>();

		// try to combine line segments
		
		// These line segments are combinable. The inner arraylist has always 
		// two elements. The keys maps to the track, the line segments are in.
		Collection<LineSegmentCombineEntry> lineSegments = new ArrayList<LineSegmentCombineEntry>();
		
		for (Track t : Main.main.ds.tracks) {
			ArrayList<LineSegment> current = new ArrayList<LineSegment>();
			for (LineSegment ls : t.segments)
				if (ls.start == n || ls.end == n)
					current.add(ls);
			if (!current.isEmpty()) {
				if (current.size() > 2) {
					JOptionPane.showMessageDialog(Main.main, "Node used by more than two line segments.");
					return;
				}
				if (current.size() == 1 && 
						(current.get(0) == t.getStartingSegment() || current.get(0) == t.getEndingSegment()))
					pendingLineSegmentsForTrack.add(current.get(0));
				else if (current.get(0).end != current.get(1).start &&
						current.get(1).end != current.get(0).start) {
					JOptionPane.showMessageDialog(Main.main, "Node used by line segments that points together.");
					return;
				} else if (!current.get(0).keyPropertiesMergable(current.get(1))) {
					JOptionPane.showMessageDialog(Main.main, "Node used by line segments with different properties.");
					return;
				} else {
					LineSegmentCombineEntry e = new LineSegmentCombineEntry();
					e.first = current.get(0);
					e.second = current.get(1);
					e.track = t;
					lineSegments.add(e);
				}
			}
		}
		
		// try to combine tracks
		ArrayList<Track> tracks = new ArrayList<Track>();
		for (Track t : Main.main.ds.tracks)
			if (t.getStartingNode() == n || t.getEndingNode() == n)
				tracks.add(t);
		if (!tracks.isEmpty()) {
			if (tracks.size() > 2) {
				JOptionPane.showMessageDialog(Main.main, "Node used by more than two tracks.");
				return;
			}
			if (tracks.size() == 1) {
				JOptionPane.showMessageDialog(Main.main, "Node used by a track.");
				return;
			}
			Track t1 = tracks.get(0);
			Track t2 = tracks.get(1);
			if (t1.getStartingNode() != t2.getEndingNode() &&
					t2.getStartingNode() != t1.getEndingNode()) {
				if (t1.getStartingNode() == t2.getStartingNode() ||
						t1.getEndingNode() == t2.getEndingNode()) {
					JOptionPane.showMessageDialog(Main.main, "Node used by tracks that point together.");
					return;
				}
				JOptionPane.showMessageDialog(Main.main, "Node used by tracks that cannot be combined.");
				return;
			}
			if (!t1.keyPropertiesMergable(t2)) {
				JOptionPane.showMessageDialog(Main.main, "Node used by tracks with different properties.");
				return;
			}
		}
		
		// try to match the pending line segments
		if (pendingLineSegmentsForTrack.size() == 2) {
			LineSegment l1 = pendingLineSegmentsForTrack.get(0);
			LineSegment l2 = pendingLineSegmentsForTrack.get(1);
			if (l1.start == l2.start || l1.end == l2.end) {
				JOptionPane.showMessageDialog(Main.main, "Node used by line segments that points together.");
				return;
			}
			if (l1.start == l2.end || l2.start == l1.end)
				pendingLineSegmentsForTrack.clear(); // resolved.
		}
		
		// still pending line segments?
		if (!pendingLineSegmentsForTrack.isEmpty()) {
			JOptionPane.showMessageDialog(Main.main, "Node used by tracks that cannot be combined.");
			return;
		}

		// Ok, we can combine. Do it.
		Track firstTrack = tracks.isEmpty() ? null : tracks.get(0);
		Track secondTrack = tracks.isEmpty() ? null : tracks.get(1);
		mv.editLayer().add(new CombineAndDeleteCommand(n, lineSegments, firstTrack, secondTrack));
	}
}
