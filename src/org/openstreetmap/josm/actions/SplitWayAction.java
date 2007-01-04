package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;

import javax.swing.JOptionPane;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.command.AddCommand;
import org.openstreetmap.josm.command.ChangeCommand;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.command.SequenceCommand;
import org.openstreetmap.josm.data.SelectionChangedListener;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Segment;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.visitor.NameVisitor;

/**
 *
 * @author framm
 */
public class SplitWayAction extends JosmAction implements SelectionChangedListener {

	private Way way;
	private Node node;

	/**
	 * Create a new SplitWayAction.
	 */
	public SplitWayAction() {
		super(tr("Split Way"), "splitway", tr("Split a way at the selected node."), KeyEvent.VK_P, KeyEvent.CTRL_MASK | KeyEvent.SHIFT_MASK, true);
		Main.ds.addSelectionChangedListener(this);
	}

	/**
	 * Called when the action is executed
	 */
	public void actionPerformed(ActionEvent e) {
		if (!checkSelection(Main.ds.getSelected())) {
			JOptionPane.showMessageDialog(Main.parent, tr("Select exactly one way to split and one node to split the way at."));
			return;
		}

		if (way == null) {
			for (Way w : Main.ds.ways) {
				for (Segment s : w.segments) {
					if (s.from == node || s.to == node) {
						if (way != null) {
							JOptionPane.showMessageDialog(Main.parent, tr("The selected node belongs to more than one way. Please select both, the node and the way to split."));
							return;
						}
						way = w;
						break;
					}
				}
			}
		}
		if (way == null) {
			JOptionPane.showMessageDialog(Main.parent, tr("The seleced node is not part of any way."));
			return;
		}
		
		boolean node_found = false;
		for (Segment s : way.segments) {
			if (s.incomplete) {
				JOptionPane.showMessageDialog(Main.parent, tr("Warning: This way is incomplete. Try to download it before splitting."));
				return;
			}
			if (!node_found && (s.from.equals(node) || s.to.equals(node))) {
				node_found = true;
			}
		}

		if (!node_found) {
			JOptionPane.showMessageDialog(Main.parent, tr("The seleced node is not part of the selected way."));
			return;
		}

		splitWay();
	}

	/** 
	 * Checks if the selection consists of eactly one way and one node.
	 * Does not check whether the node is part of the way.
	 */
	private boolean checkSelection(Collection<? extends OsmPrimitive> selection) {
		if (selection.isEmpty() || selection.size() > 2)
			return false;
		way = null;
		node = null;
		for (OsmPrimitive p : selection) {
			if ((p instanceof Way) && (way == null)) {
				way = (Way)p;
			} else if ((p instanceof Node) && (node == null)) {
				node = (Node)p;
			} else {
				return false;
			}
		}
		return node != null;
	}

	/**
	 * Split a way into two parts.
	 */
	private void splitWay() {

		HashSet<Node> nodesInFirstHalf = new HashSet<Node>();

		for (Segment s : way.segments) {
			if (s.from.equals(node)) {
				nodesInFirstHalf.add(s.to);
				break;
			} else if (s.to.equals(node)) {
				nodesInFirstHalf.add(s.from);
				break;
			}
		}

		boolean loop = true;
		while (loop) {
			loop = false;
			for (Segment s : way.segments) {
				if (nodesInFirstHalf.contains(s.from) && (!s.to.equals(node))
				        && !nodesInFirstHalf.contains(s.to)) {
					nodesInFirstHalf.add(s.to);
					loop = true;
				} else if (nodesInFirstHalf.contains(s.to)
				        && (!s.from.equals(node))
				        && !nodesInFirstHalf.contains(s.from)) {
					nodesInFirstHalf.add(s.from);
					loop = true;
				}
			}
		}

		Way wayToAdd = new Way(way);
		wayToAdd.id = 0;
		Way changedWay = new Way(way);

		for (Segment s : way.segments) {
			if (nodesInFirstHalf.contains(s.from) || nodesInFirstHalf.contains(s.to)) {
				changedWay.segments.remove(s);
			} else {
				wayToAdd.segments.remove(s);
			}
		}
		
		if (wayToAdd.segments.isEmpty() || changedWay.segments.isEmpty()) {
			JOptionPane.showMessageDialog(Main.parent, tr("The selected node is not in the middle of the way."));
			return;
		}

		NameVisitor v = new NameVisitor();
		v.visit(way);
		Main.main.editLayer().add(new SequenceCommand(tr("Split way {0}",v.name),
			Arrays.asList(new Command[]{new ChangeCommand(way, changedWay), new AddCommand(wayToAdd)})));
		Main.ds.clearSelection();
		way = null;
		node = null;
		return;
	}

	public void selectionChanged(Collection<? extends OsmPrimitive> newSelection) {
		setEnabled(checkSelection(newSelection));
	}
}
