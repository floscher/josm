package org.openstreetmap.josm.actions;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;

import org.openstreetmap.josm.actions.SelectionManager.SelectionEnded;
import org.openstreetmap.josm.data.osm.LineSegment;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.Track;
import org.openstreetmap.josm.gui.MapFrame;
import org.openstreetmap.josm.gui.MapView;

/**
 * This MapMode enables the user to easy make a selection of different objects.
 * 
 * The selected objects are drawn in a different style.
 * 
 * Holding and dragging the left mouse button draws an selection rectangle. 
 * When releasing the left mouse button, all objects within the rectangle get 
 * selected. 
 * 
 * When releasing the left mouse button while the right mouse button pressed,
 * nothing happens (the selection rectangle will be cleared, however).
 *
 * When releasing the mouse button and one of the following keys was hold:
 *
 * If Alt key was hold, select all objects that are touched by the 
 * selection rectangle. If the Alt key was not hold, select only those objects 
 * completly within (e.g. for tracks mean: only if all nodes of the track are 
 * within).  
 *
 * If Shift key was hold, the objects are added to the current selection. If
 * Shift key wasn't hold, the current selection get replaced.
 * 
 * If Ctrl key was hold, remove all objects under the current rectangle from
 * the active selection (if there were any). Nothing is added to the current
 * selection.
 *
 * Alt can be combined with Ctrl or Shift. Ctrl and Shift cannot be combined.
 * If both are pressed, nothing happens when releasing the mouse button.
 *
 * @author imi
 */
public class SelectionAction extends MapMode implements SelectionEnded {

	/**
	 * Shortcut for the MapView.
	 */
	private MapView mv;
	/**
	 * The SelectionManager that manages the selection rectangle.
	 */
	private SelectionManager selectionManager;

	/**
	 * Create a new SelectionAction in the given frame.
	 * @param mapFrame
	 */
	public SelectionAction(MapFrame mapFrame) {
		super("Selection", "selection", KeyEvent.VK_S, mapFrame);
		this.mv = mapFrame.mapView;
		this.selectionManager = new SelectionManager(this, false, mv);
	}

	@Override
	public void registerListener(MapView mapView) {
		selectionManager.register(mapView);
	}

	@Override
	public void unregisterListener(MapView mapView) {
		selectionManager.unregister(mapView);
	}


	/**
	 * Check the state of the keys and buttons and set the selection accordingly.
	 */
	public void selectionEnded(Rectangle r, int modifiers) {
		boolean shift = (modifiers & MouseEvent.SHIFT_DOWN_MASK) != 0;
		boolean alt = (modifiers & MouseEvent.ALT_DOWN_MASK) != 0;
		boolean ctrl = (modifiers & MouseEvent.CTRL_DOWN_MASK) != 0;
		if (shift && ctrl)
			return; // not allowed together

		if (!ctrl && !shift) {
			// remove the old selection. The new selection will replace the old.
			mv.dataSet.clearSelection();
		}

		// now set the selection to this value
		boolean selection = !ctrl;

		// nodes
		for (Node n : mv.dataSet.allNodes) {
			Point sp = mv.getScreenPoint(n.coor);
			if (r.contains(sp))
				n.selected = selection;
		}
		
		// tracks
		for (Track t : mv.dataSet.tracks) {
			boolean wholeTrackSelected = t.segments.size() > 0;
			for (LineSegment ls : t.segments) {
				if (alt) {
					Point p1 = mv.getScreenPoint(ls.start.coor);
					Point p2 = mv.getScreenPoint(ls.end.coor);
					if (r.intersectsLine(p1.x, p1.y, p2.x, p2.y))
						ls.selected = selection;
					else
						wholeTrackSelected = false;
				} else {
					if (r.contains(mv.getScreenPoint(ls.start.coor)) && 
							r.contains(mv.getScreenPoint(ls.end.coor)))
						ls.selected = selection;
					else
						wholeTrackSelected = false;
				}
			}
			if (wholeTrackSelected)
				t.selected = true;
		}
		mv.repaint();
	}
}
