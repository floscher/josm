// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.relation;

import org.openstreetmap.josm.data.osm.Relation;

/**
 * Super interface of relation-aware editors.
 * @since 9496
 */
public interface RelationAware {

    /**
     * Replies the currently edited relation
     *
     * @return the currently edited relation
     */
    Relation getRelation();

    /**
     * Sets the currently edited relation. Creates a snapshot of the current
     * state of the relation. See {@link #getRelationSnapshot()}
     *
     * @param relation the relation
     */
    void setRelation(Relation relation);

    /**
     * Replies the state of the edited relation when the editor has been launched.
     * @return the state of the edited relation when the editor has been launched
     */
    Relation getRelationSnapshot();

    /**
     * Replies true if the currently edited relation has been changed elsewhere.
     *
     * In this case a relation editor can't apply updates to the relation directly. Rather,
     * it has to create a conflict.
     *
     * @return true if the currently edited relation has been changed elsewhere.
     */
    boolean isDirtyRelation();
}
