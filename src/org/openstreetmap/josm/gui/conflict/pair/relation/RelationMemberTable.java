// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.conflict.pair.relation;

import javax.swing.ListSelectionModel;

import org.openstreetmap.josm.actions.ZoomToAction;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.gui.conflict.pair.ListMergeModel;
import org.openstreetmap.josm.gui.conflict.pair.PairTable;
import org.openstreetmap.josm.gui.widgets.OsmPrimitivesTableModel;

public class RelationMemberTable extends PairTable {

    /**
     * Constructs a new {@code RelationMemberTable}.
     * @param name table name
     * @param model relation member merge model
     * @param dm table model
     * @param sm selection model
     */
    public RelationMemberTable(String name, ListMergeModel<RelationMember> model, OsmPrimitivesTableModel dm, ListSelectionModel sm) {
        super(name, model, dm, new RelationMemberListColumnModel(), sm);
    }

    @Override
    protected ZoomToAction buildZoomToAction() {
        return new ZoomToAction(this);
    }
}
