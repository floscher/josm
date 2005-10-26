package org.openstreetmap.josm.data.osm.visitor;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.util.Collection;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.osm.Key;
import org.openstreetmap.josm.data.osm.LineSegment;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.Track;
import org.openstreetmap.josm.gui.MapView;

/**
 * A visitor that paint a simple scheme of every primitive it visits to a 
 * previous set graphic environment.
 * 
 * @author imi
 */
public class SimplePaintVisitor implements Visitor {

	private final static Color darkblue = new Color(0,0,128);
	private final static Color darkgreen = new Color(0,128,0);

	/**
	 * The environment to paint to.
	 */
	private final Graphics g;
	/**
	 * MapView to get screen coordinates.
	 */
	private final MapView mv;
	/**
	 * Can be set to non-<code>null</code> and then replace every other color.
	 */
	private final Color forceColor;
	
	/**
	 * Construct the painter visitor.
	 * @param g   The graphics to draw to.
	 * @param mv  The view to get screen coordinates from.
	 * @param forceColor If non-<code>null</code>, always draw with this color.
	 */
	public SimplePaintVisitor(Graphics g, MapView mv, Color forceColor) {
		this.g = g;
		this.mv = mv;
		this.forceColor = forceColor;
	}
	
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
	public void visit(Node n) {
		if (n.isSelected()) {
			drawNode(n, Color.WHITE); // selected
			return;
		}

		Collection<LineSegment> lineSegments = Main.main.ds.nodeLsRef.get(n);
		if (lineSegments == null || lineSegments.isEmpty()) {
			drawNode(n, Color.YELLOW); // single waypoint only
			return;
		}

		Collection<Track> tracks = Main.main.ds.nodeTrackRef.get(n);
		if (tracks == null || tracks.isEmpty()) {
			drawNode(n, Color.GREEN); // pending line
			return;
		}
		if (tracks.size() > 1) {
			drawNode(n, Color.RED); // more than one track
			return;
		}
		int segmentUsed = 0;
		for (LineSegment ls : tracks.iterator().next().segments)
			if (n == ls.start || n == ls.end)
				++segmentUsed;
		drawNode(n, segmentUsed > 1 ? darkblue : Color.RED);
	}

	public void visit(LineSegment ls) {
		if (forceColor != null)
			g.setColor(forceColor);
		else if (ls.isSelected())
			g.setColor(Color.WHITE);
		else if (Main.main.ds.pendingLineSegments.contains(ls))
			g.setColor(darkgreen);
		else
			g.setColor(darkblue);
		Point p1 = mv.getScreenPoint(ls.start.coor);
		Point p2 = mv.getScreenPoint(ls.end.coor);
		g.drawLine(p1.x, p1.y, p2.x, p2.y);
	}

	/**
	 * Draw a darkblue line for all line segments.
	 * @param t The track to draw.
	 */
	public void visit(Track t) {
		for (LineSegment ls : t.segments)
			visit(ls);
	}

	public void visit(Key k) {
	}
	
	/**
	 * Draw the node as small rectangle with the given color.
	 *
	 * @param n		The node to draw.
	 * @param color The color of the node.
	 */
	private void drawNode(Node n, Color color) {
		Point p = mv.getScreenPoint(n.coor);
		g.setColor(forceColor != null ? forceColor : color);
		g.drawRect(p.x-1, p.y-1, 2, 2);
	}
}
