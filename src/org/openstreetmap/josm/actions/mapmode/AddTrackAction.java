package org.openstreetmap.josm.actions.mapmode;

import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.command.AddCommand;
import org.openstreetmap.josm.data.osm.LineSegment;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Track;
import org.openstreetmap.josm.gui.MapFrame;
import org.openstreetmap.josm.gui.SelectionManager;
import org.openstreetmap.josm.gui.SelectionManager.SelectionEnded;

/**
 * Add a new track from all selected line segments.
 *
 * If there is a selection when the mode is entered, all line segments in this
 * selection form a new track, except the user holds down Shift.
 *
 * The user can click on a line segment. If he holds down Shift, no track is 
 * created yet. If he holds down Alt, the whole track is considered instead of 
 * the clicked line segment. If the user holds down Ctrl, no track is created 
 * and the clicked line segment get removed from the list.
 *
 * Also, the user may select a rectangle as in selection mode. No node, area or
 * track can be selected this way.
 *
 * @author imi
 *
 */
public class AddTrackAction extends MapMode implements SelectionEnded {

	/**
	 * The selection manager for this action, keeping track of all selections.
	 */
	SelectionManager selectionManager;
	
	/**
	 * Create a new AddTrackAction.
	 * @param mapFrame The MapFrame this action belongs to.
	 */
	public AddTrackAction(MapFrame mapFrame) {
		super("Add Track", "addtrack", "Combine line segments to a new track.", KeyEvent.VK_T, mapFrame);
		this.selectionManager = new SelectionManager(this, false, mv);
	}

	@Override
	public void registerListener() {
		super.registerListener();
		selectionManager.register(mv);
	}

	@Override
	public void unregisterListener() {
		super.unregisterListener();
		selectionManager.unregister(mv);
	}

	
	@Override
	public void actionPerformed(ActionEvent e) {
		makeTrack();
		super.actionPerformed(e);
	}

	/**
	 * If Shift is pressed, only add the selected line segments to the selection.
	 * If Ctrl is pressed, only remove the selected line segments from the selection.
	 * If both, Shift and Ctrl is pressed, do nothing.
	 * 
	 * Else, form a new track out of all line segments in the selection and
	 * clear the selection afterwards.
	 * 
	 * If Alt is pressed, consider all linesegments of all tracks a selected
	 * line segment is part of. Also consider all line segments that cross the
	 * selecting rectangle, instead only those that are fully within.
	 * 
	 */
	public void selectionEnded(Rectangle r, boolean alt, boolean shift, boolean ctrl) {
		if (shift && ctrl)
			return; // not allowed together

		if (!ctrl && !shift)
			Main.main.ds.clearSelection(); // new selection will replace the old.

		Collection<OsmPrimitive> selectionList = selectionManager.getObjectsInRectangle(r,alt);
		for (OsmPrimitive osm : selectionList)
			osm.setSelected(!ctrl);

		mv.repaint(); // from now on, the map has to be repainted.

		if (ctrl || shift)
			return; // no new track yet.
		
		makeTrack();
	}

	/**
	 * Just make a track of all selected items.
	 */
	private void makeTrack() {
		Collection<OsmPrimitive> selection = Main.main.ds.getSelected();
		if (selection.isEmpty())
			return;

		// form a new track
		LinkedList<LineSegment> lineSegments = new LinkedList<LineSegment>();
		for (OsmPrimitive osm : selection) {
			if (osm instanceof Track)
				lineSegments.addAll(((Track)osm).segments);
			else if (osm instanceof LineSegment)
				lineSegments.add((LineSegment)osm);
		}
		
		// sort the line segments in best possible order. This is done by:
		// 0  if no elements in list, quit
		// 1  taking the first ls as pivot, remove it from list
		// 2  searching for a connection at start or end of pivot
		// 3  if found, attach it, remove it from list, goto 2
		// 4  if not found, save the pivot-string and goto 0
		LinkedList<LineSegment> sortedLineSegments = new LinkedList<LineSegment>();
		while (!lineSegments.isEmpty()) {
			LinkedList<LineSegment> pivotList = new LinkedList<LineSegment>();
			pivotList.add(lineSegments.getFirst());
			lineSegments.removeFirst();
			for (boolean found = true; found;) {
				found = false;
				for (Iterator<LineSegment> it = lineSegments.iterator(); it.hasNext();) {
					LineSegment ls = it.next();
					if (ls.start == pivotList.getLast().end) {
						pivotList.addLast(ls);
						it.remove();
						found = true;
					} else if (ls.end == pivotList.getFirst().start) {
						pivotList.addFirst(ls);
						it.remove();
						found = true;
					}
				}
			}
			sortedLineSegments.addAll(pivotList);
		}
		
		Track t = new Track();
		for (LineSegment ls : sortedLineSegments)
			t.segments.add(ls);
		mv.editLayer().add(new AddCommand(Main.main.ds, t));
		Main.main.ds.clearSelection();
		mv.repaint();
	}
}
