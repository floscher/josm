package org.openstreetmap.josm.gui.engine;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import org.openstreetmap.josm.data.osm.LineSegment;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.Track;
import org.openstreetmap.josm.gui.Main;
import org.openstreetmap.josm.gui.MapView;

/**
 * This engine draws data from raw gps sources. It is to be in the backgound
 * and give an editor a hint of where he walked.
 * @author imi
 */
public class RawGpsEngine extends Engine implements PropertyChangeListener {

	/**
	 * Draw a line to this node if forceRawGpsLines is set.
	 */
	private Node lastNode;
	
	/**
	 * Create a raw gps engine. The engine will register itself as listener on
	 * the main preference settings to capture the drawRawGpsLines changes.
	 */
	public RawGpsEngine() {
		Main.pref.addPropertyChangeListener(this);
	}

	
	@Override
	public void init(Graphics g, MapView mv) {
		super.init(g, mv);
		lastNode = null;
	}

	/**
	 * Draw nodes as small gray dots
	 */
	@Override
	public void drawNode(Node n) {
		Point p = mv.getScreenPoint(n.coor);
		g.setColor(n.isSelected() ? Color.WHITE : Color.GRAY);
		g.drawRect(p.x, p.y, 0, 0);
		if (Main.pref.isForceRawGpsLines()) {
			if (lastNode != null)
				drawLine(lastNode, n, false, Color.GRAY);
			lastNode = n;
		}
	}

	/**
	 * Draw tracks by drawing all line segments as simple gray lines.
	 * If the main preference "drawRawGpsLines" is set to false, nothing
	 * is drawn.
	 */
	@Override
	public void drawTrack(Track t) {
		if (!Main.pref.isDrawRawGpsLines())
			return;
		for (LineSegment ls : t.segments())
			drawLine(ls.getStart(), ls.getEnd(), ls.isSelected(), t.isSelected() ? Color.WHITE : Color.GRAY);
	}

	/**
	 * Draw line segments as simple gray lines.
	 */
	@Override
	public void drawPendingLineSegment(LineSegment ls) {
		drawLine(ls.getStart(), ls.getEnd(), ls.isSelected(), Color.GRAY);
	}

	/**
	 * Draw the line segment in the given color.
	 * @param ls		The line segment to draw.
	 * @param color		The color, the line segment should be drawn in.
	 */
	private void drawLine(Node start, Node end, boolean isSelected, Color color) {
		g.setColor(isSelected ? Color.WHITE : color);
		Point p1 = mv.getScreenPoint(start.coor);
		Point p2 = mv.getScreenPoint(end.coor);
		g.drawLine(p1.x, p1.y, p2.x, p2.y);
	}


	/**
	 * Called when the some preferences are changed.
	 */
	public void propertyChange(PropertyChangeEvent e) {
		if (e.getPropertyName().equals("drawRawGpsLines") || e.getPropertyName().equals("forceRawGpsLines"))
			mv.repaint();
	}
}
