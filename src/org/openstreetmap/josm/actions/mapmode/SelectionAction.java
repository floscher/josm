package org.openstreetmap.josm.actions.mapmode;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;

import org.openstreetmap.josm.data.osm.LineSegment;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Track;
import org.openstreetmap.josm.gui.MapFrame;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.gui.SelectionManager;
import org.openstreetmap.josm.gui.SelectionManager.SelectionEnded;

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
 * The user can also only click on the map. All total movements of 2 or less 
 * pixel are considered "only click". If that happens, the nearest Node will
 * be selected if there is any within 10 pixel range. If there is no Node within
 * 10 pixel, the nearest LineSegment (or Street, if user hold down the Alt-Key)
 * within 10 pixel range is selected. If there is no LineSegment within 10 pixel
 * and the user clicked in or 10 pixel away from an area, this area is selected. 
 * If there is even no area, nothing is selected. Shift and Ctrl key applies to 
 * this as usual.
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
		super("Selection", "selection", "Select objects by dragging or clicking", KeyEvent.VK_S, mapFrame);
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

		// whether user only clicked, not dragged.
		boolean clicked = r.width <= 2 && r.height <= 2;
		Point2D.Double center = new Point2D.Double(r.getCenterX(), r.getCenterY());

		try {
			// nodes
			double minDistanceSq = Double.MAX_VALUE;
			OsmPrimitive minPrimitive = null;
			for (Node n : mv.dataSet.allNodes) {
				Point sp = mv.getScreenPoint(n.coor);
				double dist = center.distanceSq(sp);
				if (clicked && minDistanceSq > dist && dist < 100) {
					minDistanceSq = center.distanceSq(sp);
					minPrimitive = n;
				} else if (r.contains(sp))
					n.selected = selection;
			}
			if (minPrimitive != null) {
				minPrimitive.selected = selection;
				return;
			}

			// tracks
			minDistanceSq = Double.MAX_VALUE;
			for (Track t : mv.dataSet.tracks) {
				boolean wholeTrackSelected = t.segments.size() > 0;
				for (LineSegment ls : t.segments) {
					if (clicked) {
						Point A = mv.getScreenPoint(ls.start.coor);
						Point B = mv.getScreenPoint(ls.end.coor);
						double c = A.distanceSq(B);
						double a = center.distanceSq(B);
						double b = center.distanceSq(A);
						double perDist = perpendicularDistSq(a,b,c);
						if (perDist < 100 && minDistanceSq > perDist && a < c+100 && b < c+100) {
							minDistanceSq = perDist;
							if (alt)
								minPrimitive = t;
							else
								minPrimitive = ls;
						}
					} else {
						if (alt) {
							Point p1 = mv.getScreenPoint(ls.start.coor);
							Point p2 = mv.getScreenPoint(ls.end.coor);
							if (r.intersectsLine(p1.x, p1.y, p2.x, p2.y))
								ls.selected = selection;
							else
								wholeTrackSelected = false;
						} else {
							if (r.contains(mv.getScreenPoint(ls.start.coor))
									&& r.contains(mv.getScreenPoint(ls.end.coor)))
								ls.selected = selection;
							else
								wholeTrackSelected = false;
						}
					}
				}
				if (wholeTrackSelected && !clicked)
					t.selected = true;
			}
			if (minPrimitive != null) {
				minPrimitive.selected = selection;
				return;
			}
			
			// TODO arrays
		} finally {
			mv.repaint();
		}
	}

	/**
	 * Calculates the squared perpendicular distance named "h" from a point C to the
	 * straight line going to the points A and B, where the distance to B is 
	 * sqrt(a) and the distance to A is sqrt(b).
	 * 
	 * Think of a, b and c as the squared line lengths of any ordinary triangle 
	 * A,B,C. a = BC, b = AC and c = AB. The straight line goes through A and B 
	 * and the desired return value is the perpendicular distance from C to c.
	 *
	 * @param a Squared distance from B to C.
	 * @param b Squared distance from A to C.
	 * @param c Squared distance from A to B.
	 * @return The perpendicular distance from C to c.
	 */
	private double perpendicularDistSq(double a, double b, double c) {
		// I did this on paper by myself, so I am surprised too, that it is that 
		// performant ;-) 
		return a-(a-b+c)*(a-b+c)/4/c;
	}
}
