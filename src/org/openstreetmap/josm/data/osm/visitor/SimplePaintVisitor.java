package org.openstreetmap.josm.data.osm.visitor;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.osm.LineSegment;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.NavigatableComponent;
import org.openstreetmap.josm.tools.ColorHelper;

/**
 * A visitor that paint a simple scheme of every primitive it visits to a 
 * previous set graphic environment.
 * 
 * @author imi
 */
public class SimplePaintVisitor implements Visitor {

	public final static Color darkerblue = new Color(0,0,96);
	public final static Color darkblue = new Color(0,0,128);
	public final static Color darkgreen = new Color(0,128,0);

	/**
	 * The environment to paint to.
	 */
	private final Graphics g;
	/**
	 * MapView to get screen coordinates.
	 */
	private final NavigatableComponent nc;
	
	/**
	 * Construct the painter visitor.
	 * @param g   The graphics to draw to.
	 * @param mv  The view to get screen coordinates from.
	 */
	public SimplePaintVisitor(Graphics g, NavigatableComponent mv) {
		this.g = g;
		this.nc = mv;
	}
	
	/**
	 * Draw a small rectangle. 
	 * White if selected (as always) or red otherwise.
	 * 
	 * @param n The node to draw.
	 */
	public void visit(Node n) {
		drawNode(n, n.isSelected() ? getPreferencesColor("selected", Color.WHITE)
				: getPreferencesColor("node", Color.RED));
	}

	/**
	 * Draw just a line between the points.
	 * White if selected (as always) or green otherwise.
	 */
	public void visit(LineSegment ls) {
		drawLineSegment(ls, getPreferencesColor("segment", darkgreen));
	}

	/**
	 * Draw a darkblue line for all line segments.
	 * @param t The way to draw.
	 */
	public void visit(Way t) {
		// only to overwrite with blue
		Color wayColor = getPreferencesColor("way", darkblue);
		for (LineSegment ls : t.segments) {
			if (ls.incomplete) {
				wayColor = getPreferencesColor("incomplete way", darkerblue);
				break;
			}
		}
		for (LineSegment ls : t.segments)
			if (!ls.isSelected()) // selected already in good color
				drawLineSegment(ls, t.isSelected() ? getPreferencesColor("selected", Color.WHITE) : wayColor);
	}

	/**
	 * Draw the node as small rectangle with the given color.
	 *
	 * @param n		The node to draw.
	 * @param color The color of the node.
	 */
	private void drawNode(Node n, Color color) {
		Point p = nc.getPoint(n.eastNorth);
		g.setColor(color);
		g.drawRect(p.x-1, p.y-1, 2, 2);
	}

	/**
	 * Draw a line with the given color.
	 */
	private void drawLineSegment(LineSegment ls, Color col) {
		if (ls.incomplete)
			return;
		if (ls.isSelected())
			col = getPreferencesColor("selected", Color.WHITE);
		g.setColor(col);
		Point p1 = nc.getPoint(ls.from.eastNorth);
		Point p2 = nc.getPoint(ls.to.eastNorth);
		g.drawLine(p1.x, p1.y, p2.x, p2.y);
	}
	
	private Color getPreferencesColor(String colName, Color def) {
		String colStr = Main.pref.get("color."+colName);
		if (colStr.equals(""))
			return def;
		return ColorHelper.html2color(colStr);
	}
}
