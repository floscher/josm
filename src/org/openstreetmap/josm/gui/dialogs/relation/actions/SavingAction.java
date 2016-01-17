// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.relation.actions;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.ArrayList;
import java.util.List;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.command.AddCommand;
import org.openstreetmap.josm.command.ChangeCommand;
import org.openstreetmap.josm.command.conflict.ConflictAddCommand;
import org.openstreetmap.josm.data.conflict.Conflict;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.gui.DefaultNameFormatter;
import org.openstreetmap.josm.gui.HelpAwareOptionPane;
import org.openstreetmap.josm.gui.HelpAwareOptionPane.ButtonSpec;
import org.openstreetmap.josm.gui.dialogs.relation.MemberTable;
import org.openstreetmap.josm.gui.dialogs.relation.MemberTableModel;
import org.openstreetmap.josm.gui.dialogs.relation.RelationAware;
import org.openstreetmap.josm.gui.dialogs.relation.RelationDialogManager;
import org.openstreetmap.josm.gui.dialogs.relation.RelationEditor;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.gui.tagging.TagEditorModel;
import org.openstreetmap.josm.gui.tagging.ac.AutoCompletingTextField;
import org.openstreetmap.josm.tools.ImageProvider;

/**
 * Abstract superclass of relation saving actions (OK, Apply, Cancel).
 * @since 9496
 */
abstract class SavingAction extends AbstractRelationEditorAction {

    protected final TagEditorModel tagModel;
    protected final AutoCompletingTextField tfRole;

    protected SavingAction(MemberTable memberTable, MemberTableModel memberTableModel, TagEditorModel tagModel, OsmDataLayer layer,
            RelationAware editor, AutoCompletingTextField tfRole) {
        super(memberTable, memberTableModel, null, layer, editor);
        this.tagModel = tagModel;
        this.tfRole = tfRole;
    }

    /**
     * apply updates to a new relation
     * @param tagEditorModel tag editor model
     */
    protected void applyNewRelation(TagEditorModel tagEditorModel) {
        final Relation newRelation = new Relation();
        tagEditorModel.applyToPrimitive(newRelation);
        memberTableModel.applyToRelation(newRelation);
        List<RelationMember> newMembers = new ArrayList<>();
        for (RelationMember rm: newRelation.getMembers()) {
            if (!rm.getMember().isDeleted()) {
                newMembers.add(rm);
            }
        }
        if (newRelation.getMembersCount() != newMembers.size()) {
            newRelation.setMembers(newMembers);
            String msg = tr("One or more members of this new relation have been deleted while the relation editor\n" +
            "was open. They have been removed from the relation members list.");
            JOptionPane.showMessageDialog(Main.parent, msg, tr("Warning"), JOptionPane.WARNING_MESSAGE);
        }
        // If the user wanted to create a new relation, but hasn't added any members or
        // tags, don't add an empty relation
        if (newRelation.getMembersCount() == 0 && !newRelation.hasKeys())
            return;
        Main.main.undoRedo.add(new AddCommand(layer, newRelation));

        // make sure everybody is notified about the changes
        //
        layer.data.fireSelectionChanged();
        editor.setRelation(newRelation);
        if (editor instanceof RelationEditor) {
            RelationDialogManager.getRelationDialogManager().updateContext(
                    layer, editor.getRelation(), (RelationEditor) editor);
        }
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                // Relation list gets update in EDT so selecting my be postponed to following EDT run
                Main.map.relationListDialog.selectRelation(newRelation);
            }
        });
    }

    /**
     * Apply the updates for an existing relation which has been changed outside of the relation editor.
     * @param tagEditorModel tag editor model
     */
    protected void applyExistingConflictingRelation(TagEditorModel tagEditorModel) {
        Relation editedRelation = new Relation(editor.getRelation());
        tagEditorModel.applyToPrimitive(editedRelation);
        memberTableModel.applyToRelation(editedRelation);
        Conflict<Relation> conflict = new Conflict<>(editor.getRelation(), editedRelation);
        Main.main.undoRedo.add(new ConflictAddCommand(layer, conflict));
    }

    /**
     * Apply the updates for an existing relation which has not been changed outside of the relation editor.
     * @param tagEditorModel tag editor model
     */
    protected void applyExistingNonConflictingRelation(TagEditorModel tagEditorModel) {
        Relation editedRelation = new Relation(editor.getRelation());
        tagEditorModel.applyToPrimitive(editedRelation);
        memberTableModel.applyToRelation(editedRelation);
        Main.main.undoRedo.add(new ChangeCommand(editor.getRelation(), editedRelation));
        layer.data.fireSelectionChanged();
        // this will refresh the snapshot and update the dialog title
        //
        editor.setRelation(editor.getRelation());
    }

    protected boolean confirmClosingBecauseOfDirtyState() {
        ButtonSpec[] options = new ButtonSpec[] {
                new ButtonSpec(
                        tr("Yes, create a conflict and close"),
                        ImageProvider.get("ok"),
                        tr("Click to create a conflict and close this relation editor"),
                        null /* no specific help topic */
                ),
                new ButtonSpec(
                        tr("No, continue editing"),
                        ImageProvider.get("cancel"),
                        tr("Click to return to the relation editor and to resume relation editing"),
                        null /* no specific help topic */
                )
        };

        int ret = HelpAwareOptionPane.showOptionDialog(
                Main.parent,
                tr("<html>This relation has been changed outside of the editor.<br>"
                        + "You cannot apply your changes and continue editing.<br>"
                        + "<br>"
                        + "Do you want to create a conflict and close the editor?</html>"),
                        tr("Conflict in data"),
                        JOptionPane.WARNING_MESSAGE,
                        null,
                        options,
                        options[0], // OK is default
                        "/Dialog/RelationEditor#RelationChangedOutsideOfEditor"
        );
        return ret == 0;
    }

    protected void warnDoubleConflict() {
        JOptionPane.showMessageDialog(
                Main.parent,
                tr("<html>Layer ''{0}'' already has a conflict for object<br>"
                        + "''{1}''.<br>"
                        + "Please resolve this conflict first, then try again.</html>",
                        layer.getName(),
                        editor.getRelation().getDisplayName(DefaultNameFormatter.getInstance())
                ),
                tr("Double conflict"),
                JOptionPane.WARNING_MESSAGE
        );
    }

    @Override
    protected void updateEnabledState() {
        // Do nothing
    }
}
