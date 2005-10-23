package org.openstreetmap.josm.gui.layer;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.util.Collection;
import java.util.HashSet;

import javax.swing.Icon;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.osm.Key;
import org.openstreetmap.josm.data.osm.LineSegment;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Track;
import org.openstreetmap.josm.data.osm.visitor.Visitor;
import org.openstreetmap.josm.gui.ImageProvider;
import org.openstreetmap.josm.gui.MapView;

/**
 * A layer holding data imported from the osm server.
 * 
 * The data can be fully edited.
 * 
 * @author imi
 */
public class OsmDataLayer extends Layer implements Visitor {

	private static Icon icon;
	private final static Color darkblue = new Color(0,0,128);
	private final static Color darkgreen = new Color(0,128,0);

	/**
	 * The data behind this layer. A list of primitives which are also in Main.main.ds.
	 */
	private final Collection<OsmPrimitive> data;

	/**
	 * The mapview we are currently drawing on.
	 */
	private MapView mv;
	/**
	 * The graphic environment we are drawing with.
	 */
	private Graphics g;
	
	/**
	 * Construct a OsmDataLayer.
	 */
	protected OsmDataLayer(Collection<OsmPrimitive> data, String name) {
		super(name);
		this.data = data;
	}

	/**
	 * TODO: @return Return a dynamic drawn icon of the map data. The icon is
	 * 		updated by a background thread to not disturb the running programm.
	 */
	@Override
	public Icon getIcon() {
		if (icon == null)
			icon = ImageProvider.get("layer", "osmdata");
		return icon;
	}

	@Override
	public boolean isEditable() {
		return true;
	}

	@Override
	public void paint(Graphics g, MapView mv) {
		this.mv = mv;
		this.g = g;
		for (OsmPrimitive osm : data)
			osm.visit(this);
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

	public void visit(LineSegment ls) {
		g.setColor(ls.isSelected() ? Color.WHITE : darkblue);
		if (Main.main.ds.pendingLineSegments().contains(ls))
			g.setColor(darkgreen);
		Point p1 = mv.getScreenPoint(ls.getStart().coor);
		Point p2 = mv.getScreenPoint(ls.getEnd().coor);
		g.drawLine(p1.x, p1.y, p2.x, p2.y);
	}

	/**
	 * Draw a darkblue line for all line segments.
	 * @param t The track to draw.
	 */
	public void visit(Track t) {
		for (LineSegment ls : t.segments())
			ls.visit(this);
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
		g.setColor(color);
		g.drawRect(p.x-1, p.y-1, 2, 2);
	}
}
