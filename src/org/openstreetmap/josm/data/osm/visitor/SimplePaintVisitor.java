package org.openstreetmap.josm.data.osm.visitor;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;

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
	 * White if selected (as always) or red otherwise.
	 * 
	 * @param n The node to draw.
	 */
	public void visit(Node n) {
		drawNode(n, n.isSelected() ? Color.WHITE : Color.RED);
	}

	/**
	 * Draw just a line between the points.
	 * White if selected (as always) or green otherwise.
	 */
	public void visit(LineSegment ls) {
		drawLineSegment(ls, darkgreen);
	}

	/**
	 * Draw a darkblue line for all line segments.
	 * @param t The track to draw.
	 */
	public void visit(Track t) {
		for (LineSegment ls : t.segments)
			drawLineSegment(ls, darkblue);
	}

	/**
	 * Do not draw a key.
	 */
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

	/**
	 * Draw a line with the given color.
	 */
	private void drawLineSegment(LineSegment ls, Color col) {
		if (forceColor != null)
			col = forceColor;
		else if (ls.isSelected())
			col = Color.WHITE;
		g.setColor(col);
		Point p1 = mv.getScreenPoint(ls.start.coor);
		Point p2 = mv.getScreenPoint(ls.end.coor);
		g.drawLine(p1.x, p1.y, p2.x, p2.y);
	}
}
