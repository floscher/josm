// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.io;

import java.awt.BorderLayout;
import java.util.Map;

import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.osm.Changeset;
import org.openstreetmap.josm.gui.tagging.TagEditorPanel;
import org.openstreetmap.josm.gui.tagging.TagModel;
import org.openstreetmap.josm.tools.CheckParameterUtil;

/**
 * Tag settings panel of upload dialog.
 * @since 2599
 */
public class TagSettingsPanel extends JPanel implements TableModelListener {

    /** checkbox for selecting whether an atomic upload is to be used  */
    private final TagEditorPanel pnlTagEditor = new TagEditorPanel(null, null, Changeset.MAX_CHANGESET_TAG_LENGTH);
    /** the model for the changeset comment */
    private final transient ChangesetCommentModel changesetCommentModel;
    private final transient ChangesetCommentModel changesetSourceModel;

    /**
     * Creates a new panel
     *
     * @param changesetCommentModel the changeset comment model. Must not be null.
     * @param changesetSourceModel the changeset source model. Must not be null.
     * @throws IllegalArgumentException if {@code changesetCommentModel} is null
     */
    public TagSettingsPanel(ChangesetCommentModel changesetCommentModel, ChangesetCommentModel changesetSourceModel) {
        CheckParameterUtil.ensureParameterNotNull(changesetCommentModel, "changesetCommentModel");
        CheckParameterUtil.ensureParameterNotNull(changesetSourceModel, "changesetSourceModel");
        this.changesetCommentModel = changesetCommentModel;
        this.changesetSourceModel = changesetSourceModel;
        this.changesetCommentModel.addChangeListener(new ChangesetCommentChangeListener("comment"));
        this.changesetSourceModel.addChangeListener(new ChangesetCommentChangeListener("source"));
        build();
        pnlTagEditor.getModel().addTableModelListener(this);
    }

    protected void build() {
        setLayout(new BorderLayout());
        add(pnlTagEditor, BorderLayout.CENTER);
    }

    protected void setProperty(String key, String value) {
        String val = (value == null ? "" : value).trim();
        String commentInTag = getTagEditorValue(key);
        if (val.equals(commentInTag))
            return;

        if (val.isEmpty()) {
            pnlTagEditor.getModel().delete(key);
            return;
        }
        TagModel tag = pnlTagEditor.getModel().get(key);
        if (tag == null) {
            tag = new TagModel(key, val);
            pnlTagEditor.getModel().add(tag);
        } else {
            pnlTagEditor.getModel().updateTagValue(tag, val);
        }
    }

    protected String getTagEditorValue(String key) {
        TagModel tag = pnlTagEditor.getModel().get(key);
        return tag == null ? null : tag.getValue();
    }

    /**
     * Initialize panel from the given tags.
     * @param tags the tags used to initialize the panel
     */
    public void initFromTags(Map<String, String> tags) {
        pnlTagEditor.getModel().initFromTags(tags);
    }

    /**
     * Replies the map with the current tags in the tag editor model.
     * @param keepEmpty {@code true} to keep empty tags
     * @return the map with the current tags in the tag editor model.
     */
    public Map<String, String> getTags(boolean keepEmpty) {
        return pnlTagEditor.getModel().getTags(keepEmpty);
    }

    /**
     * Initializes the panel for user input
     */
    public void startUserInput() {
        pnlTagEditor.initAutoCompletion(Main.getLayerManager().getEditLayer());
    }

    /* -------------------------------------------------------------------------- */
    /* Interface TableChangeListener                                              */
    /* -------------------------------------------------------------------------- */
    @Override
    public void tableChanged(TableModelEvent e) {
        changesetCommentModel.setComment(getTagEditorValue("comment"));
        changesetSourceModel.setComment(getTagEditorValue("source"));
    }

    /**
     * Observes the changeset comment model and keeps the tag editor in sync
     * with the current changeset comment
     */
    class ChangesetCommentChangeListener implements ChangeListener {

        private final String key;

        ChangesetCommentChangeListener(String key) {
            this.key = key;
        }

        @Override
        public void stateChanged(ChangeEvent e) {
            if (e.getSource() instanceof ChangesetCommentModel) {
                String newValue = ((ChangesetCommentModel) e.getSource()).getComment();
                String oldValue = getTagEditorValue(key);
                if (oldValue == null) {
                    oldValue = "";
                }
                if (!oldValue.equals(newValue)) {
                    setProperty(key, newValue);
                }
            }
        }
    }
}
