package org.openstreetmap.josm.actions.mapmode;

import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.util.Collection;
import java.util.LinkedList;

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
			ds.clearSelection(); // new selection will replace the old.

		Collection<OsmPrimitive> selectionList = selectionManager.getObjectsInRectangle(r,alt);
		for (OsmPrimitive osm : selectionList)
			osm.setSelected(!ctrl, ds);

		mv.repaint(); // from now on, the map has to be repainted.

		if (ctrl || shift)
			return; // no new track yet.
		
		Collection<OsmPrimitive> selection = ds.getSelected();
		if (selection.isEmpty())
			return;

		// form a new track
		LinkedList<LineSegment> lineSegments = new LinkedList<LineSegment>();
		for (OsmPrimitive osm : selection) {
			if (osm instanceof Track)
				lineSegments.addAll(((Track)osm).segments());
			else if (osm instanceof LineSegment)
				lineSegments.add((LineSegment)osm);
		}
		Track t = new Track();
		for (LineSegment ls : lineSegments)
			ds.assignPendingLineSegment(ls, t, true);
		ds.addTrack(t);
		ds.clearSelection();
	}
}
