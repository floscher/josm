//License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.command.ChangeCommand;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.command.SequenceCommand;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.WaySegment;
import org.openstreetmap.josm.tools.Shortcut;

public class JoinNodeWayAction extends JosmAction {
    public JoinNodeWayAction() {
        super(tr("Join Node to Way"), "joinnodeway", tr("Join a node into the nearest way segments"),
            Shortcut.registerShortcut("tools:joinnodeway", tr("Tool: {0}", tr("Join Node to Way")), KeyEvent.VK_J, Shortcut.GROUP_EDIT), true);
    }

    public void actionPerformed(ActionEvent e) {
        Collection<OsmPrimitive> sel = Main.ds.getSelected();
        if (sel.size() != 1 || !(sel.iterator().next() instanceof Node)) return;
        Node node = (Node) sel.iterator().next();

        List<WaySegment> wss = Main.map.mapView.getNearestWaySegments(
            Main.map.mapView.getPoint(node.eastNorth));
        HashMap<Way, List<Integer>> insertPoints = new HashMap<Way, List<Integer>>();
        for (WaySegment ws : wss) {
            List<Integer> is;
            if (insertPoints.containsKey(ws.way)) {
                is = insertPoints.get(ws.way);
            } else {
                is = new ArrayList<Integer>();
                insertPoints.put(ws.way, is);
            }

            if (ws.way.nodes.get(ws.lowerIndex) != node
                    && ws.way.nodes.get(ws.lowerIndex+1) != node) {
                is.add(ws.lowerIndex);
            }
        }

        Collection<Command> cmds = new LinkedList<Command>();
        for (Map.Entry<Way, List<Integer>> insertPoint : insertPoints.entrySet()) {
            Way w = insertPoint.getKey();
            Way wnew = new Way(w);
            List<Integer> is = insertPoint.getValue();
            pruneSuccsAndReverse(is);
            for (int i : is) wnew.nodes.add(i+1, node);
            cmds.add(new ChangeCommand(w, wnew));
        }

        Main.main.undoRedo.add(new SequenceCommand(tr("Join Node and Line"), cmds));
        Main.map.repaint();
    }

    private static void pruneSuccsAndReverse(List<Integer> is) {
        //if (is.size() < 2) return;

        HashSet<Integer> is2 = new HashSet<Integer>();
        for (int i : is) {
            if (!is2.contains(i - 1) && !is2.contains(i + 1)) {
                is2.add(i);
            }
        }
        is.clear();
        is.addAll(is2);
        Collections.sort(is);
        Collections.reverse(is);
    }
}
