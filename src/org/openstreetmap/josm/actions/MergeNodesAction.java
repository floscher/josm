//License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Map.Entry;
import java.util.HashSet;

import javax.swing.Box;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.command.ChangeCommand;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.command.DeleteCommand;
import org.openstreetmap.josm.command.SequenceCommand;
import org.openstreetmap.josm.data.SelectionChangedListener;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.NodePair;
import org.openstreetmap.josm.tools.GBC;


/**
 * Merge two or more nodes into one node.
 * (based on Combine ways)
 * 
 * @author Matthew Newton
 *
 */
public class MergeNodesAction extends JosmAction implements SelectionChangedListener {

	public MergeNodesAction() {
		super(tr("Merge Nodes"), "mergenodes", tr("Merge nodes into one."), KeyEvent.VK_M, KeyEvent.CTRL_MASK | KeyEvent.SHIFT_MASK, true);
		DataSet.selListeners.add(this);
	}

	public void actionPerformed(ActionEvent event) {
		Collection<OsmPrimitive> selection = Main.ds.getSelected();
		LinkedList<Node> selectedNodes = new LinkedList<Node>();

		// the selection check should stop this procedure starting if
		// nothing but node are selected - otherwise we don't care
		// anyway as long as we have at least two nodes
		for (OsmPrimitive osm : selection)
			if (osm instanceof Node)
				selectedNodes.add((Node)osm);

		if (selectedNodes.size() < 2) {
			JOptionPane.showMessageDialog(Main.parent, tr("Please select at least two nodes to merge."));
			return;
		}

		// Find which node to merge into (i.e. which one will be left)
		// - this should be combined from two things:
		//   1. It will be the first node in the list that has a
		//      positive ID number, OR the first node.
		//   2. It will be at the position of the first node in the
		//      list.
		//
		// *However* - there is the problem that the selection list is
		// _not_ in the order that the nodes were clicked on, meaning
		// that the user doesn't know which node will be chosen (so
		// (2) is not implemented yet.)  :-(
		Node useNode = null;
		for (Node n: selectedNodes) {
			if (n.id > 0) {
				useNode = n;
				break;
			}
		}
		if (useNode == null)
			useNode = selectedNodes.iterator().next();

		mergeNodes(selectedNodes, useNode);
	}

	/**
	 * really do the merging - returns the node that is left
	 */
	public static Node mergeNodes(LinkedList<Node> allNodes, Node dest) {
		Node newNode = new Node(dest);

		// Check whether all ways have identical relationship membership. More 
		// specifically: If one of the selected ways is a member of relation X
		// in role Y, then all selected ways must be members of X in role Y.

		// FIXME: In a later revision, we should display some sort of conflict 
		// dialog like we do for tags, to let the user choose which relations
		// should be kept.

		// Step 1, iterate over all relations and create counters indicating
		// how many of the selected ways are part of relation X in role Y
		// (hashMap backlinks contains keys formed like X@Y)
		HashMap<String, Integer> backlinks = new HashMap<String, Integer>();
		HashSet<Relation> relationsUsingNodes = new HashSet<Relation>();
		for (Relation r : Main.ds.relations) {
			if (r.deleted || r.incomplete) continue;
			for (RelationMember rm : r.members) {
				if (rm.member instanceof Node) {
					for(Node n : allNodes) {
						if (rm.member == n) {
							String hash = Long.toString(r.id) + "@" + rm.role;
							System.out.println(hash);
							if (backlinks.containsKey(hash)) {
								backlinks.put(hash, new Integer(backlinks.get(hash)+1));
							} else {
								backlinks.put(hash, 1);
							}
							// this is just a cache for later use
							relationsUsingNodes.add(r);
						}
					}
				}
			}
		}

		// Step 2, all values of the backlinks HashMap must now equal the size
		// of the selection.
		for (Integer i : backlinks.values()) {
			if (i.intValue() != allNodes.size()) {
				JOptionPane.showMessageDialog(Main.parent, tr("The selected nodes cannot be merged as they have differing relation memberships."));
				return null;
			}
		}

		// collect properties for later conflict resolving
		Map<String, Set<String>> props = new TreeMap<String, Set<String>>();
		for (Node n : allNodes) {
			for (Entry<String,String> e : n.entrySet()) {
				if (!props.containsKey(e.getKey()))
					props.put(e.getKey(), new TreeSet<String>());
				props.get(e.getKey()).add(e.getValue());
			}
		}

		// display conflict dialog
		Map<String, JComboBox> components = new HashMap<String, JComboBox>();
		JPanel p = new JPanel(new GridBagLayout());
		for (Entry<String, Set<String>> e : props.entrySet()) {
			if (e.getValue().size() > 1) {
				JComboBox c = new JComboBox(e.getValue().toArray());
				c.setEditable(true);
				p.add(new JLabel(e.getKey()), GBC.std());
				p.add(Box.createHorizontalStrut(10), GBC.std());
				p.add(c, GBC.eol());
				components.put(e.getKey(), c);
			} else
				newNode.put(e.getKey(), e.getValue().iterator().next());
		}

		if (!components.isEmpty()) {
			int answer = JOptionPane.showConfirmDialog(Main.parent, p, tr("Enter values for all conflicts."), JOptionPane.OK_CANCEL_OPTION);
			if (answer != JOptionPane.OK_OPTION)
				return null;
			for (Entry<String, JComboBox> e : components.entrySet())
				newNode.put(e.getKey(), e.getValue().getEditor().getItem().toString());
		}

		LinkedList<Command> cmds = new LinkedList<Command>();
		cmds.add(new ChangeCommand(dest, newNode));

		// OK, now to merge the nodes - this should be fairly
		// straightforward:
		//   for each node in allNodes that is not newNode, do:
		//     search all existing ways and replace references of
		//     current node with newNode, then delete current node

		Collection<OsmPrimitive> del = new HashSet<OsmPrimitive>();

		for (Way w : Main.ds.ways) {
			if (w.deleted || w.incomplete || w.nodes.size() < 1) continue;
			boolean modify = false;
			for (Node sn : allNodes) {
				if (sn == dest) continue;
				if (w.nodes.contains(sn)) {
					modify = true;
				}
			}
			if (!modify) continue;
			// OK - this way contains one or more nodes to change
			ArrayList<Node> nn = new ArrayList<Node>();
			Node lastNode = null;
			for (int i = 0; i < w.nodes.size(); i++) {
				Node pushNode = w.nodes.get(i);
				if (allNodes.contains(pushNode)) {
					pushNode = dest;
				}
				if (pushNode != lastNode) {
					nn.add(pushNode);
				}
				lastNode = pushNode;
			}
			if (nn.size() < 2) {
				del.add(w);
			} else {
				Way newWay = new Way(w);
				newWay.nodes.clear();
				newWay.nodes.addAll(nn);
				cmds.add(new ChangeCommand(w, newWay));
			}
		}

		// delete any merged nodes
		for (Node n : allNodes) {
			if (n != dest) {
				del.add(n);
			}
		}
		if (!del.isEmpty()) cmds.add(new DeleteCommand(del));

		// modify all relations containing the now-deleted nodes
		for (Relation r : relationsUsingNodes) {
			Relation newRel = new Relation(r);
			newRel.members.clear();
			for (RelationMember rm : r.members) {
				// only copy member if it is either the first of all the selected
				// nodes (indexOf==0) or not one of the selected nodes (indexOf==-1)
				if (allNodes.indexOf(rm.member) < 1) {
					newRel.members.add(new RelationMember(rm));
				}
			}
			cmds.add(new ChangeCommand(r, newRel));
		}

		Main.main.undoRedo.add(new SequenceCommand(tr("Merge {0} nodes", allNodes.size()), cmds));
		Main.ds.setSelected(dest);

		return dest;
	}


	/**
	 * Enable the "Merge Nodes" menu option if more then one node is selected
	 */
	public void selectionChanged(Collection<? extends OsmPrimitive> newSelection) {
		boolean ok = true;
		if (newSelection.size() < 2) {
			setEnabled(false);
			return;
		}
		for (OsmPrimitive osm : newSelection) {
			if (!(osm instanceof Node)) {
				ok = false;
				break;
			}
		}
		setEnabled(ok);
	}
}
