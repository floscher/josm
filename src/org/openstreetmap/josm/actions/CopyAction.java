// License: GPL. Copyright 2007 by Immanuel Scholz and others
// Author: David Earl
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.Collection;
import java.util.LinkedList;

import javax.swing.JOptionPane;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.tools.Shortcut;

public final class CopyAction extends JosmAction {

    private LinkedList<JosmAction> listeners;

    public CopyAction() {
        super(tr("Copy"), "copy",
                tr("Copy selected objects to paste buffer."),
                Shortcut.registerShortcut("system:copy", tr("Edit: {0}", tr("Copy")), KeyEvent.VK_C, Shortcut.GROUP_MENU), true);
        listeners = new LinkedList<JosmAction>();
    }

    @Override public void addListener(JosmAction a) {
        listeners.add(a);
    }

    public void actionPerformed(ActionEvent e) {
        if(isEmptySelection()) return;

        Main.pasteBuffer.makeCopy(getCurrentDataSet().getSelected());
        Main.pasteSource = getEditLayer();
        Main.main.menu.paste.setEnabled(true); /* now we have a paste buffer we can make paste available */

        for(JosmAction a : listeners) {
            a.pasteBufferChanged(Main.pasteBuffer);
        }
    }

    @Override
    protected void updateEnabledState() {
        if (getCurrentDataSet() == null) {
            setEnabled(false);
        } else {
            updateEnabledState(getCurrentDataSet().getSelected());
        }
    }

    @Override
    protected void updateEnabledState(Collection<? extends OsmPrimitive> selection) {
        setEnabled(selection != null && !selection.isEmpty());
    }

    private boolean isEmptySelection() {
        Collection<OsmPrimitive> sel = getCurrentDataSet().getSelected();
        if (sel.isEmpty()) {
            JOptionPane.showMessageDialog(
                    Main.parent,
                    tr("Please select something to copy."),
                    tr("Information"),
                    JOptionPane.INFORMATION_MESSAGE
            );
            return true;
        }
        return false;
    }
}
