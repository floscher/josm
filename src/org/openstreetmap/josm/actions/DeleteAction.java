// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.gui.help.HelpUtil.ht;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.Collection;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.tools.Shortcut;

/**
 * Action that deletes selected objects.
 * @since 770
 */
public final class DeleteAction extends JosmAction {

    /**
     * Constructs a new {@code DeleteAction}.
     */
    public DeleteAction() {
        super(tr("Delete"), "dialogs/delete", tr("Delete selected objects."),
                Shortcut.registerShortcut("system:delete", tr("Edit: {0}", tr("Delete")), KeyEvent.VK_DELETE, Shortcut.DIRECT), true);
        putValue("help", ht("/Action/Delete"));
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (!isEnabled() || !Main.map.mapView.isActiveLayerVisible())
            return;
        org.openstreetmap.josm.actions.mapmode.DeleteAction.doActionPerformed(e);
    }

    @Override
    protected void updateEnabledState() {
        DataSet ds = getLayerManager().getEditDataSet();
        if (ds == null) {
            setEnabled(false);
        } else {
            updateEnabledState(ds.getSelected());
        }
    }

    @Override
    protected void updateEnabledState(Collection<? extends OsmPrimitive> selection) {
        setEnabled(selection != null && !selection.isEmpty());
    }
}
