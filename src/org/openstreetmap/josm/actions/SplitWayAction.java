// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trn;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.Map.Entry;

import javax.swing.JOptionPane;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.command.AddCommand;
import org.openstreetmap.josm.command.ChangeCommand;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.command.SequenceCommand;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.visitor.AbstractVisitor;
import org.openstreetmap.josm.data.osm.visitor.Visitor;
import org.openstreetmap.josm.gui.OptionPaneUtil;
import org.openstreetmap.josm.gui.PrimitiveNameFormatter;
import org.openstreetmap.josm.tools.Shortcut;

/**
 * Splits a way into multiple ways (all identical except for their node list).
 *
 * Ways are just split at the selected nodes.  The nodes remain in their
 * original order.  Selected nodes at the end of a way are ignored.
 */

public class SplitWayAction extends JosmAction {

    private Way selectedWay;
    private List<Node> selectedNodes;

    /**
     * Create a new SplitWayAction.
     */
    public SplitWayAction() {
        super(tr("Split Way"), "splitway", tr("Split a way at the selected node."),
                Shortcut.registerShortcut("tools:splitway", tr("Tool: {0}", tr("Split Way")), KeyEvent.VK_P, Shortcut.GROUP_EDIT), true);
    }

    /**
     * Called when the action is executed.
     *
     * This method performs an expensive check whether the selection clearly defines one
     * of the split actions outlined above, and if yes, calls the splitWay method.
     */
    public void actionPerformed(ActionEvent e) {

        Collection<OsmPrimitive> selection = getCurrentDataSet().getSelected();

        if (!checkSelection(selection)) {
            OptionPaneUtil.showMessageDialog(
                    Main.parent,
                    tr("The current selection cannot be used for splitting."),
                    tr("Warning"),
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        selectedWay = null;
        selectedNodes = null;

        Visitor splitVisitor = new AbstractVisitor() {
            public void visit(Node n) {
                if (selectedNodes == null) {
                    selectedNodes = new LinkedList<Node>();
                }
                selectedNodes.add(n);
            }
            public void visit(Way w) {
                selectedWay = w;
            }
            public void visit(Relation e) {
                // enties are not considered
            }
        };

        for (OsmPrimitive p : selection) {
            p.visit(splitVisitor);
        }

        // If only nodes are selected, try to guess which way to split. This works if there
        // is exactly one way that all nodes are part of.
        if (selectedWay == null && selectedNodes != null) {
            HashMap<Way, Integer> wayOccurenceCounter = new HashMap<Way, Integer>();
            for (Node n : selectedNodes) {
                for (Way w : getCurrentDataSet().ways) {
                    if (w.deleted || w.incomplete) {
                        continue;
                    }
                    int last = w.nodes.size()-1;
                    if(last <= 0) {
                        continue; // zero or one node ways
                    }
                    Boolean circular = w.nodes.get(0).equals(w.nodes.get(last));
                    int i = 0;
                    for (Node wn : w.nodes) {
                        if ((circular || (i > 0 && i < last)) && n.equals(wn)) {
                            Integer old = wayOccurenceCounter.get(w);
                            wayOccurenceCounter.put(w, (old == null) ? 1 : old+1);
                            break;
                        }
                        i++;
                    }
                }
            }
            if (wayOccurenceCounter.isEmpty()) {
                OptionPaneUtil.showMessageDialog(Main.parent,
                        trn("The selected node is not in the middle of any way.",
                                "The selected nodes are not in the middle of any way.",
                                selectedNodes.size()),
                                tr("Warning"),
                                JOptionPane.WARNING_MESSAGE);
                return;
            }

            for (Entry<Way, Integer> entry : wayOccurenceCounter.entrySet()) {
                if (entry.getValue().equals(selectedNodes.size())) {
                    if (selectedWay != null) {
                        OptionPaneUtil.showMessageDialog(Main.parent,
                                trn("There is more than one way using the node you selected. Please select the way also.",
                                        "There is more than one way using the nodes you selected. Please select the way also.",
                                        selectedNodes.size()),
                                        tr("Warning"),
                                        JOptionPane.WARNING_MESSAGE);
                        return;
                    }
                    selectedWay = entry.getKey();
                }
            }

            if (selectedWay == null) {
                OptionPaneUtil.showMessageDialog(Main.parent,
                        tr("The selected nodes do not share the same way."),
                        tr("Warning"),
                        JOptionPane.WARNING_MESSAGE);
                return;
            }

            // If a way and nodes are selected, verify that the nodes are part of the way.
        } else if (selectedWay != null && selectedNodes != null) {

            HashSet<Node> nds = new HashSet<Node>(selectedNodes);
            for (Node n : selectedWay.nodes) {
                nds.remove(n);
            }
            if (!nds.isEmpty()) {
                OptionPaneUtil.showMessageDialog(Main.parent,
                        trn("The selected way does not contain the selected node.",
                                "The selected way does not contain all the selected nodes.",
                                selectedNodes.size()),
                                tr("Warning"),
                                JOptionPane.WARNING_MESSAGE);
                return;
            }
        }

        // and then do the work.
        splitWay();
    }

    /**
     * Checks if the selection consists of something we can work with.
     * Checks only if the number and type of items selected looks good;
     * does not check whether the selected items are really a valid
     * input for splitting (this would be too expensive to be carried
     * out from the selectionChanged listener).
     */
    private boolean checkSelection(Collection<? extends OsmPrimitive> selection) {
        boolean way = false;
        boolean node = false;
        for (OsmPrimitive p : selection) {
            if (p instanceof Way && !way) {
                way = true;
            } else if (p instanceof Node) {
                node = true;
            } else
                return false;
        }
        return node;
    }

    /**
     * Split a way into two or more parts, starting at a selected node.
     */
    private void splitWay() {
        // We take our way's list of nodes and copy them to a way chunk (a
        // list of nodes).  Whenever we stumble upon a selected node, we start
        // a new way chunk.

        Set<Node> nodeSet = new HashSet<Node>(selectedNodes);
        List<List<Node>> wayChunks = new LinkedList<List<Node>>();
        List<Node> currentWayChunk = new ArrayList<Node>();
        wayChunks.add(currentWayChunk);

        Iterator<Node> it = selectedWay.nodes.iterator();
        while (it.hasNext()) {
            Node currentNode = it.next();
            boolean atEndOfWay = currentWayChunk.isEmpty() || !it.hasNext();
            currentWayChunk.add(currentNode);
            if (nodeSet.contains(currentNode) && !atEndOfWay) {
                currentWayChunk = new ArrayList<Node>();
                currentWayChunk.add(currentNode);
                wayChunks.add(currentWayChunk);
            }
        }

        // Handle circular ways specially.
        // If you split at a circular way at two nodes, you just want to split
        // it at these points, not also at the former endpoint.
        // So if the last node is the same first node, join the last and the
        // first way chunk.
        List<Node> lastWayChunk = wayChunks.get(wayChunks.size() - 1);
        if (wayChunks.size() >= 2
                && wayChunks.get(0).get(0) == lastWayChunk.get(lastWayChunk.size() - 1)
                && !nodeSet.contains(wayChunks.get(0).get(0))) {
            if (wayChunks.size() == 2) {
                OptionPaneUtil.showMessageDialog(
                        Main.parent,
                        tr("You must select two or more nodes to split a circular way."),
                        tr("Warning"),
                        JOptionPane.WARNING_MESSAGE);
                return;
            }
            lastWayChunk.remove(lastWayChunk.size() - 1);
            lastWayChunk.addAll(wayChunks.get(0));
            wayChunks.remove(wayChunks.size() - 1);
            wayChunks.set(0, lastWayChunk);
        }

        if (wayChunks.size() < 2) {
            if(wayChunks.get(0).get(0) == wayChunks.get(0).get(wayChunks.get(0).size()-1)) {
                OptionPaneUtil.showMessageDialog(
                        Main.parent,
                        tr("You must select two or more nodes to split a circular way."),
                        tr("Warning"),
                        JOptionPane.WARNING_MESSAGE);
            } else {
                OptionPaneUtil.showMessageDialog(
                        Main.parent,
                        tr("The way cannot be split at the selected nodes. (Hint: Select nodes in the middle of the way.)"),
                        tr("Warning"),
                        JOptionPane.WARNING_MESSAGE);
            }
            return;
        }
        //Main.debug("wayChunks.size(): " + wayChunks.size());
        //Main.debug("way id: " + selectedWay.id);

        // build a list of commands, and also a new selection list
        Collection<Command> commandList = new ArrayList<Command>(wayChunks.size());
        Collection<Way> newSelection = new ArrayList<Way>(wayChunks.size());

        Iterator<List<Node>> chunkIt = wayChunks.iterator();

        // First, change the original way
        Way changedWay = new Way(selectedWay);
        changedWay.nodes.clear();
        changedWay.nodes.addAll(chunkIt.next());
        commandList.add(new ChangeCommand(selectedWay, changedWay));
        newSelection.add(selectedWay);

        Collection<Way> newWays = new ArrayList<Way>();
        // Second, create new ways
        while (chunkIt.hasNext()) {
            Way wayToAdd = new Way();
            if (selectedWay.keys != null) {
                wayToAdd.keys = new HashMap<String, String>(selectedWay.keys);
            }
            newWays.add(wayToAdd);
            wayToAdd.nodes.addAll(chunkIt.next());
            commandList.add(new AddCommand(wayToAdd));
            //Main.debug("wayToAdd: " + wayToAdd);
            newSelection.add(wayToAdd);

        }
        Boolean warnmerole=false;
        Boolean warnme=false;
        // now copy all relations to new way also

        for (Relation r : getCurrentDataSet().relations) {
            if (r.deleted || r.incomplete) {
                continue;
            }
            Relation c = null;
            String type = r.get("type");
            if (type == null) {
                type = "";
            }
            int i = 0;

            for (RelationMember rm : r.members) {
                if (rm.member instanceof Way) {
                    if (rm.member == selectedWay)
                    {
                        if(!("route".equals(type)) && !("multipolygon".equals(type))) {
                            warnme = true;
                        }
                        if (c == null) {
                            c = new Relation(r);
                        }

                        int j = i;
                        boolean backwards = "backward".equals(rm.role);
                        for(Way wayToAdd : newWays)
                        {
                            RelationMember em = new RelationMember();
                            em.member = wayToAdd;
                            em.role = rm.role;
                            if(em.role != null && em.role.length() > 0 && !("multipolygon".equals(type))) {
                                warnmerole = true;
                            }

                            j++;
                            if (backwards) {
                                c.members.add(i, em);
                            } else {
                                c.members.add(j, em);
                            }
                        }
                        i = j;
                    }
                }
                i++;
            }

            if (c != null) {
                commandList.add(new ChangeCommand(r, c));
            }
        }
        if(warnmerole) {
            OptionPaneUtil.showMessageDialog(
                    Main.parent,
                    tr("<html>A role based relation membership was copied to all new ways.<br>You should verify this and correct it when necessary.</html>"),
                    tr("Warning"),
                    JOptionPane.WARNING_MESSAGE);
        } else if(warnme) {
            OptionPaneUtil.showMessageDialog(
                    Main.parent,
                    tr("<html>A relation membership was copied to all new ways.<br>You should verify this and correct it when necessary.</html>"),
                    tr("Warning"),
                    JOptionPane.WARNING_MESSAGE);
        }

        Main.main.undoRedo.add(
                new SequenceCommand(tr("Split way {0} into {1} parts",
                        new PrimitiveNameFormatter().getName(selectedWay), wayChunks.size()),
                        commandList));
        getCurrentDataSet().setSelected(newSelection);
    }

    @Override
    protected void updateEnabledState() {
        if (getCurrentDataSet() == null) {
            setEnabled(false);
            return;
        }
        setEnabled(checkSelection(getCurrentDataSet().getSelected()));
    }
}
