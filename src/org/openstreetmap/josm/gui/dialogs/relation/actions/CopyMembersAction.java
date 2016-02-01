// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.relation.actions;

import java.awt.event.ActionEvent;
import java.util.Collection;

import org.openstreetmap.josm.actions.CopyAction;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.gui.dialogs.relation.IRelationEditor;
import org.openstreetmap.josm.gui.dialogs.relation.MemberTableModel;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;

/**
 * Copy members.
 * @since 9496
 */
public class CopyMembersAction extends AddFromSelectionAction {

    /**
     * Constructs a new {@code CopyMembersAction}.
     * @param memberTableModel member table model
     * @param layer OSM data layer
     * @param editor relation editor
     */
    public CopyMembersAction(MemberTableModel memberTableModel, OsmDataLayer layer, IRelationEditor editor) {
        super(null, memberTableModel, null, null, null, layer, editor);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        final Collection<OsmPrimitive> primitives = memberTableModel.getSelectedChildPrimitives();
        if (!primitives.isEmpty()) {
            CopyAction.copy(layer, primitives);
        }
    }

    @Override
    protected void updateEnabledState() {
        // Do nothing
    }
}
