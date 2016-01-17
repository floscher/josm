// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.relation.actions;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.gui.dialogs.relation.GenericRelationEditor.AddAbortException;
import org.openstreetmap.josm.gui.dialogs.relation.MemberTableModel;
import org.openstreetmap.josm.gui.dialogs.relation.RelationAware;
import org.openstreetmap.josm.gui.dialogs.relation.SelectionTableModel;
import org.openstreetmap.josm.tools.ImageProvider;

/**
 * Add all objects selected in the current dataset after the last selected member.
 * @since 9496
 */
public class AddSelectedAfterSelection extends AddFromSelectionAction {

    /**
     * Constructs a new {@code AddSelectedAfterSelection}.
     * @param memberTableModel member table model
     * @param selectionTableModel selection table model
     * @param editor relation editor
     */
    public AddSelectedAfterSelection(MemberTableModel memberTableModel, SelectionTableModel selectionTableModel, RelationAware editor) {
        super(null, memberTableModel, null, selectionTableModel, null, null, editor);
        putValue(SHORT_DESCRIPTION, tr("Add all objects selected in the current dataset after the last selected member"));
        putValue(SMALL_ICON, ImageProvider.get("dialogs/conflict", "copyaftercurrentright"));
        updateEnabledState();
    }

    @Override
    protected void updateEnabledState() {
        setEnabled(selectionTableModel.getRowCount() > 0 && memberTableModel.getSelectionModel().getMinSelectionIndex() >= 0);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        try {
            memberTableModel.addMembersAfterIdx(filterConfirmedPrimitives(selectionTableModel.getSelection()),
                    memberTableModel.getSelectionModel().getMaxSelectionIndex());
        } catch (AddAbortException ex) {
            if (Main.isTraceEnabled()) {
                Main.trace(ex.getMessage());
            }
        }
    }
}
