package org.openstreetmap.josm.gui.engine;

import java.awt.Color;
import java.awt.Point;
import java.util.Collection;
import java.util.HashSet;

import org.openstreetmap.josm.data.osm.LineSegment;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.Track;

/**
 * A simple graphic engine that draws rather symbolic images to easy identify
 * the different types on the screen. This is optimized for expert editors.
 * 
 * @author imi
 */
public class SimpleEngine extends Engine {

	private final static Color darkblue = new Color(0,0,128);
	private final static Color darkgreen = new Color(0,128,0);

	/**
	 * Draw a small rectangle. 
	 * 
	 * - White if selected (as always)
	 * - Yellow, if not used by any tracks or areas.
	 * - Green, if only used by pending line segments.
	 * - Darkblue, if used in tracks but are only as inbound node. Inbound are
	 *   all nodes, that have only line segments of the same track and
	 *   at least two different line segments attached.
	 * - Red otherwise (means, this is a dead end or is part of more than
	 *   one track).
	 * 
	 * @param n The node to draw.
	 */
	@Override
	public void drawNode(Node n) {
		if (n.isSelected()) {
			drawNode(n, Color.WHITE); // selected
			return;
		}

		Collection<LineSegment> lineSegments = n.getParentSegments();
		if (lineSegments.isEmpty()) {
			drawNode(n, Color.YELLOW); // single waypoint only
			return;
		}
		
		HashSet<Track> tracks = new HashSet<Track>();
		for (LineSegment ls : lineSegments)
			tracks.addAll(ls.getParents());
		if (tracks.isEmpty()) {
			drawNode(n, Color.GREEN); // pending line
			return;
		}
		if (tracks.size() > 1) {
			drawNode(n, Color.RED); // more than one track
			return;
		}
		int segmentUsed = 0;
		for (LineSegment ls : tracks.iterator().next().segments())
			if (n == ls.getStart() || n == ls.getEnd())
				++segmentUsed;
		drawNode(n, segmentUsed > 1 ? darkblue : Color.RED);
	}

	/**
	 * Draw a darkblue line for all line segments.
	 * @param t The track to draw.
	 */
	@Override
	public void drawTrack(Track t) {
		for (LineSegment ls : t.segments())
			drawLineSegment(ls, t.isSelected() ? Color.WHITE : darkblue);
	}


	/**
	 * Draw the pending line as darkgreen line.
	 * @param ls The line segment to draw.
	 */
	@Override
	public void drawPendingLineSegment(LineSegment ls) {
		drawLineSegment(ls, darkgreen);
	}


	/**
	 * Draw the line segment in the given color.
	 * @param ls		The line segment to draw.
	 * @param color		The color, the line segment should be drawn in.
	 */
	private void drawLineSegment(LineSegment ls, Color color) {
		g.setColor(ls.isSelected() ? Color.WHITE : color);
		Point p1 = mv.getScreenPoint(ls.getStart().coor);
		Point p2 = mv.getScreenPoint(ls.getEnd().coor);
		g.drawLine(p1.x, p1.y, p2.x, p2.y);
	}

	/**
	 * Drat the node as small rectangle with the given color.
	 *
	 * @param n		The node to draw.
	 * @param color The color of the node.
	 */
	private void drawNode(Node n, Color color) {
		Point p = mv.getScreenPoint(n.coor);
		g.setColor(color);
		g.drawRect(p.x-1, p.y-1, 2, 2);
	}
}

