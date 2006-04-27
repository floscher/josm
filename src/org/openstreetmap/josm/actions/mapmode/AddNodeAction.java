package org.openstreetmap.josm.actions.mapmode;

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
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.Segment;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.MapFrame;

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

	enum Mode {node, nodesegment}
	private final Mode mode;

	public static class AddNodeGroup extends GroupAction {
		public AddNodeGroup(MapFrame mf) {
			super(KeyEvent.VK_N,0);
			actions.add(new AddNodeAction(mf, "Add node", Mode.node, "Add a new node to the map"));
			actions.add(new AddNodeAction(mf, "Add node into segment", Mode.nodesegment, "Add a node into an existing segment"));
			setCurrent(0);
		}
	}

	public AddNodeAction(MapFrame mapFrame, String name, Mode mode, String desc) {
		super(name, "node/"+mode, desc, mapFrame);
		this.mode = mode;
	}

	@Override public void enterMode() {
		super.enterMode();
		Main.map.mapView.addMouseListener(this);
		Main.map.mapView.setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
	}

	@Override public void exitMode() {
		super.exitMode();
		Main.map.mapView.removeMouseListener(this);
		Main.map.mapView.setCursor(Cursor.getDefaultCursor());
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
			JOptionPane.showMessageDialog(Main.parent, "Can not add a node outside of the world.");
			return;
		}

		Command c = new AddCommand(n);
		if (mode == Mode.nodesegment) {
			Segment s = Main.map.mapView.getNearestSegment(e.getPoint());
			if (s == null)
				return;

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

			c = new SequenceCommand("Add Node into Segment", cmds);
		}
		Main.main.editLayer().add(c);
		Main.map.mapView.repaint();
	}
}
