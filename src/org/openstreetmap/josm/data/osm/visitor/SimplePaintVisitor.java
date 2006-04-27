package org.openstreetmap.josm.data.osm.visitor;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.Segment;
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
	private static final double PHI = Math.toRadians(20);

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
		drawNode(n, n.selected ? getPreferencesColor("selected", Color.WHITE)
				: getPreferencesColor("node", Color.RED));
	}

	/**
	 * Draw just a line between the points.
	 * White if selected (as always) or green otherwise.
	 */
	public void visit(Segment ls) {
		drawSegment(ls, getPreferencesColor("segment", darkgreen));
	}

	/**
	 * Draw a darkblue line for all segments.
	 * @param w The way to draw.
	 */
	public void visit(Way w) {
		// only to overwrite with blue
		Color wayColor = getPreferencesColor("way", darkblue);
		for (Segment ls : w.segments) {
			if (ls.incomplete) {
				wayColor = getPreferencesColor("incomplete way", darkerblue);
				break;
			}
		}

		for (Segment ls : w.segments)
			if (!ls.selected) // selected already in good color
				drawSegment(ls, w.selected ? getPreferencesColor("selected", Color.WHITE) : wayColor);
	}

	/**
	 * Draw the node as small rectangle with the given color.
	 *
	 * @param n		The node to draw.
	 * @param color The color of the node.
	 */
	public void drawNode(Node n, Color color) {
		Point p = nc.getPoint(n.eastNorth);
		g.setColor(color);
		g.drawRect(p.x-1, p.y-1, 2, 2);
	}

	/**
	 * Draw a line with the given color.
	 */
	private void drawSegment(Segment ls, Color col) {
		if (ls.incomplete)
			return;
		if (ls.selected)
			col = getPreferencesColor("selected", Color.WHITE);
		g.setColor(col);
		Point p1 = nc.getPoint(ls.from.eastNorth);
		Point p2 = nc.getPoint(ls.to.eastNorth);
		g.drawLine(p1.x, p1.y, p2.x, p2.y);

		if (Main.pref.getBoolean("draw.segment.direction")) {
			double t = Math.atan2(p2.y-p1.y, p2.x-p1.x) + Math.PI;
	        g.drawLine(p2.x,p2.y, (int)(p2.x + 10*Math.cos(t-PHI)), (int)(p2.y + 10*Math.sin(t-PHI)));
	        g.drawLine(p2.x,p2.y, (int)(p2.x + 10*Math.cos(t+PHI)), (int)(p2.y + 10*Math.sin(t+PHI)));
		}
	}

	public static Color getPreferencesColor(String colName, Color def) {
		String colStr = Main.pref.get("color."+colName);
		if (colStr.equals("")) {
			Main.pref.put("color."+colName, ColorHelper.color2html(def));
			return def;
		}
		return ColorHelper.html2color(colStr);
	}
}
