// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.actions.mapmode;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Cursor;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;

import javax.swing.JOptionPane;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.GroupAction;
import org.openstreetmap.josm.command.AddCommand;
import org.openstreetmap.josm.command.ChangeCommand;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.command.SequenceCommand;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Segment;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.MapFrame;
import org.openstreetmap.josm.tools.ImageProvider;

/**
 * This mode adds a new node to the dataset. The user clicks on a place to add
 * and there is it. Nothing more, nothing less.
 *
 * Newly created nodes are selected. Shift modifier does not cancel the old
 * selection as usual.
 *
 * @author imi
 *
 */
public class AddNodeAction extends MapMode {

	enum Mode {node, nodesegment, autonode}
	private final Mode mode;

	public static class AddNodeGroup extends GroupAction {
		public AddNodeGroup(MapFrame mf) {
			super(KeyEvent.VK_N,0);
			putValue("help", "Action/AddNode");
			actions.add(new AddNodeAction(mf,tr("Add node"), Mode.node, tr("Add a new node to the map")));
			actions.add(new AddNodeAction(mf, tr("Add node into segment"), Mode.nodesegment,tr( "Add a node into an existing segment")));
			actions.add(new AddNodeAction(mf, tr("Add node and connect"), Mode.autonode,tr( "Add a node and connect it to the selected (previously added) node")));
			setCurrent(0);
		}
	}

	public AddNodeAction(MapFrame mapFrame, String name, Mode mode, String desc) {
		super(name, "node/"+mode, desc, mapFrame, getCursor());
		this.mode = mode;
		putValue("help", "Action/AddNode/"+Character.toUpperCase(mode.toString().charAt(0))+mode.toString().substring(1));
	}

	private static Cursor getCursor() {
		try {
	        return ImageProvider.getCursor("crosshair", null);
        } catch (Exception e) {
        }
	    return Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR);
    }

	@Override public void enterMode() {
		super.enterMode();
		Main.map.mapView.addMouseListener(this);
	}

	@Override public void exitMode() {
		super.exitMode();
		Main.map.mapView.removeMouseListener(this);
	}

	/**
	 * If user clicked with the left button, add a node at the current mouse
	 * position.
	 *
	 * If in nodesegment mode, add the node to the line segment by splitting the
	 * segment. The new created segment will be inserted in every way the segment
	 * was part of.
	 */
	@Override public void mouseClicked(MouseEvent e) {
		if (e.getButton() != MouseEvent.BUTTON1)
			return;

		Node n = new Node(Main.map.mapView.getLatLon(e.getX(), e.getY()));
		if (n.coor.isOutSideWorld()) {
			JOptionPane.showMessageDialog(Main.parent,tr("Can not add a node outside of the world."));
			return;
		}

		Command c = new AddCommand(n);
		if (mode == Mode.nodesegment) {
			Segment s = Main.map.mapView.getNearestSegment(e.getPoint());
			if (s == null)
				return;

			if ((e.getModifiersEx() & MouseEvent.ALT_DOWN_MASK) == 0) {
				// moving the new point to the perpendicular point
				EastNorth A = s.from.eastNorth;
				EastNorth B = s.to.eastNorth;
				double ab = A.distance(B);
				double nb = n.eastNorth.distance(B);
				double na = n.eastNorth.distance(A);
				double q = (nb-na+ab)/ab/2;
				n.eastNorth = new EastNorth(B.east() + q*(A.east()-B.east()), B.north() + q*(A.north()-B.north()));
				n.coor = Main.proj.eastNorth2latlon(n.eastNorth);
			}

			Collection<Command> cmds = new LinkedList<Command>();
			cmds.add(c);
			Segment s1 = new Segment(s);
			s1.to = n;
			Segment s2 = new Segment(s.from, s.to);
			s2.from = n;
			if (s.keys != null)
				s2.keys = new HashMap<String, String>(s.keys);

			cmds.add(new ChangeCommand(s, s1));
			cmds.add(new AddCommand(s2));

			// Add the segment to every way
			for (Way wold : Main.ds.ways) {
				if (wold.segments.contains(s)) {
					Way wnew = new Way(wold);
					Collection<Segment> segs = new ArrayList<Segment>(wnew.segments);
					wnew.segments.clear();
					for (Segment waySeg : segs) {
						wnew.segments.add(waySeg);
						if (waySeg == s)
							wnew.segments.add(s2);
					}
					cmds.add(new ChangeCommand(wold, wnew));
				}
			}

			c = new SequenceCommand(tr("Add node into segment"), cmds);
		}

		if (mode == Mode.autonode) {
			Collection<OsmPrimitive> selection = Main.ds.getSelected();
			if (selection.size() == 1 && selection.iterator().next() instanceof Node) {
				Node n1 = (Node)selection.iterator().next();
				Collection<Command> cmds = new LinkedList<Command>();
				Segment s = new Segment(n1, n);
				cmds.add(c);				
				cmds.add(new AddCommand(s));			

				Way way = getWayForNode(n1);
				if (way != null) {
					Way newWay = new Way(way);
					if (way.segments.get(0).from == n1) {
						Node tmp = s.from;
						s.from = s.to;
						s.to = tmp;
						newWay.segments.add(0, s);
					} else
						newWay.segments.add(s);
					cmds.add(new ChangeCommand(way, newWay));
				}

				c = new SequenceCommand(tr("Add node and connect"), cmds);
			}	
		}		
	
		Main.main.editLayer().add(c);
		Main.ds.setSelected(n);
		Main.map.mapView.repaint();
	}
	
	/**
	 * @return If the node is part of exactly one way, return this. 
	 * 	<code>null</code> otherwise.
	 */
	private Way getWayForNode(Node n) {
		Way way = null;
		for (Way w : Main.ds.ways) {
			for (Segment s : w.segments) {
				if (s.from == n || s.to == n) {
					if (way != null)
						return null;
					if (s.from == s.to)
						return null;
					way = w;
				}
			}
		}
		return way;
	}
}
