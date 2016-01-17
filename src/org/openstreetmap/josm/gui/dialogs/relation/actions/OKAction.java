// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.relation.actions;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Component;
import java.awt.event.ActionEvent;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.gui.dialogs.relation.MemberTable;
import org.openstreetmap.josm.gui.dialogs.relation.MemberTableModel;
import org.openstreetmap.josm.gui.dialogs.relation.RelationAware;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.gui.tagging.TagEditorModel;
import org.openstreetmap.josm.gui.tagging.ac.AutoCompletingTextField;
import org.openstreetmap.josm.tools.ImageProvider;

/**
 * Apply the updates and close the dialog.
 */
public class OKAction extends SavingAction {

    /**
     * Constructs a new {@code OKAction}.
     * @param memberTable member table
     * @param memberTableModel member table model
     * @param tagModel tag editor model
     * @param layer OSM data layer
     * @param editor relation editor
     * @param tfRole role text field
     */
    public OKAction(MemberTable memberTable, MemberTableModel memberTableModel, TagEditorModel tagModel, OsmDataLayer layer,
            RelationAware editor, AutoCompletingTextField tfRole) {
        super(memberTable, memberTableModel, tagModel, layer, editor, tfRole);
        putValue(SHORT_DESCRIPTION, tr("Apply the updates and close the dialog"));
        putValue(SMALL_ICON, ImageProvider.get("ok"));
        putValue(NAME, tr("OK"));
        setEnabled(true);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Main.pref.put("relation.editor.generic.lastrole", tfRole.getText());
        memberTable.stopHighlighting();
        if (editor.getRelation() == null) {
            applyNewRelation(tagModel);
        } else if (!memberTableModel.hasSameMembersAs(editor.getRelationSnapshot()) || tagModel.isDirty()) {
            if (editor.isDirtyRelation()) {
                if (confirmClosingBecauseOfDirtyState()) {
                    if (layer.getConflicts().hasConflictForMy(editor.getRelation())) {
                        warnDoubleConflict();
                        return;
                    }
                    applyExistingConflictingRelation(tagModel);
                } else
                    return;
            } else {
                applyExistingNonConflictingRelation(tagModel);
            }
        }
        if (editor instanceof Component) {
            ((Component) editor).setVisible(false);
        }
    }
}
