// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.gui.help.HelpUtil.ht;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.tools.Shortcut;

/**
 * Undoes the last command.
 *
 * @author imi
 */
public class UndoAction extends JosmAction implements OsmDataLayer.CommandQueueListener {

    /**
     * Construct the action with "Undo" as label.
     */
    public UndoAction() {
        super(tr("Undo"), "undo", tr("Undo the last action."),
                Shortcut.registerShortcut("system:undo", tr("Edit: {0}", tr("Undo")), KeyEvent.VK_Z, Shortcut.CTRL), true);
        setEnabled(false);
        putValue("help", ht("/Action/Undo"));
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (Main.map == null)
            return;
        Main.map.repaint();
        Main.main.undoRedo.undo();
    }

    @Override
    protected void updateEnabledState() {
        setEnabled(Main.main != null && !Main.main.undoRedo.commands.isEmpty());
    }

    @Override
    public void commandChanged(int queueSize, int redoSize) {
        if (Main.main.undoRedo.commands.isEmpty()) {
            putValue(NAME, tr("Undo"));
            setTooltip(tr("Undo the last action."));
        } else {
            putValue(NAME, tr("Undo ..."));
            setTooltip(tr("Undo {0}",
                    Main.main.undoRedo.commands.getLast().getDescriptionText()));
        }
    }
}
