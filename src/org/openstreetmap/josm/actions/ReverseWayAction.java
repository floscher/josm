// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;

import javax.swing.JOptionPane;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.command.ChangeCommand;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.command.SequenceCommand;
import org.openstreetmap.josm.corrector.ReverseWayTagCorrector;
import org.openstreetmap.josm.corrector.UserCancelException;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.tools.Shortcut;

public final class ReverseWayAction extends JosmAction {

    public ReverseWayAction() {
        super(tr("Reverse Ways"), "wayflip", tr("Reverse the direction of all selected ways."),
                Shortcut.registerShortcut("tools:reverse", tr("Tool: {0}", tr("Reverse Ways")), KeyEvent.VK_R, Shortcut.GROUP_EDIT), true);
    }

    public void actionPerformed(ActionEvent e) {
        if (! isEnabled())
            return;
        if (getCurrentDataSet() == null)
            return;

        final Collection<Way> sel = new LinkedList<Way>();
        for (OsmPrimitive primitive : getCurrentDataSet().getSelected()) {
            if (primitive instanceof Way) {
                sel.add((Way)primitive);
            }
        }
        if (sel.isEmpty()) {
            JOptionPane.showMessageDialog(Main.parent,
                    tr("Please select at least one way."));
            return;
        }

        boolean propertiesUpdated = false;
        ReverseWayTagCorrector reverseWayTagCorrector = new ReverseWayTagCorrector();
        Collection<Command> c = new LinkedList<Command>();
        for (Way w : sel) {
            Way wnew = new Way(w);
            Collections.reverse(wnew.nodes);
            if (Main.pref.getBoolean("tag-correction.reverse-way", true)) {
                try
                {
                    final Collection<Command> changePropertyCommands = reverseWayTagCorrector.execute(w, wnew);
                    propertiesUpdated = propertiesUpdated
                    || (changePropertyCommands != null && !changePropertyCommands.isEmpty());
                    c.addAll(changePropertyCommands);
                }
                catch(UserCancelException ex)
                {
                    return;
                }
            }
            c.add(new ChangeCommand(w, wnew));
        }
        Main.main.undoRedo.add(new SequenceCommand(tr("Reverse ways"), c));
        if (propertiesUpdated) {
            DataSet.fireSelectionChanged(getCurrentDataSet().getSelected());
        }
        Main.map.repaint();
    }

    protected int getNumWaysInSelection() {
        if (getCurrentDataSet() == null) return 0;
        int ret = 0;
        for (OsmPrimitive primitive : getCurrentDataSet().getSelected()) {
            if (primitive instanceof Way) {
                ret++;
            }
        }
        return ret;
    }

    @Override
    protected void updateEnabledState() {
        setEnabled(getNumWaysInSelection() > 0);
    }
}
