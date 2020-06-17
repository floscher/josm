// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.io;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.Collections;

import javax.swing.JLabel;
import javax.swing.text.JTextComponent;

import org.openstreetmap.josm.gui.widgets.AbstractTextComponentValidator;
import org.openstreetmap.josm.tools.Utils;

/**
 * Input validators for {@link UploadDialog}
 */
abstract class UploadTextComponentValidator extends AbstractTextComponentValidator {
    private final JLabel feedback;

    UploadTextComponentValidator(JTextComponent tc, JLabel feedback) {
        super(tc);
        this.feedback = feedback;
        this.feedback.setOpaque(true);
        validate();
    }

    @Override
    protected void feedbackValid(String msg) {
        msg = msg != null ? "<html>\u2714 " + msg : null;
        super.feedbackValid(msg);
        feedback.setText(msg);
        feedback.setForeground(VALID_COLOR);
        feedback.setBackground(null);
        feedback.setBorder(null);
    }

    @Override
    protected void feedbackWarning(String msg) {
        msg = msg != null ? "<html>" + msg : null;
        super.feedbackWarning(msg);
        feedback.setText(msg);
        feedback.setForeground(null);
        feedback.setBackground(WARNING_BACKGROUND);
        feedback.setBorder(WARNING_BORDER);
    }

    @Override
    public boolean isValid() {
        throw new UnsupportedOperationException();
    }

    /**
     * Validator for the changeset {@code comment} tag
     */
    static class UploadCommentValidator extends UploadTextComponentValidator {

        UploadCommentValidator(JTextComponent tc, JLabel feedback) {
            super(tc, feedback);
        }

        @Override
        public void validate() {
            String uploadComment = getComponent().getText();
            if (UploadDialog.UploadAction.isUploadCommentTooShort(uploadComment)) {
                feedbackWarning(tr("Your upload comment is <i>empty</i>, or <i>very short</i>.<br /><br />" +
                        "This is technically allowed, but please consider that many users who are<br />" +
                        "watching changes in their area depend on meaningful changeset comments<br />" +
                        "to understand what is going on!<br /><br />" +
                        "If you spend a minute now to explain your change, you will make life<br />" +
                        "easier for many other mappers.").replace("<br />", " "));
            } else {
                String rejection = UploadDialog.UploadAction.validateUploadTag(uploadComment, "upload.comment",
                        Collections.emptyList(), Collections.emptyList(), Collections.emptyList());
                if (rejection != null) {
                    feedbackWarning(tr("Your upload comment is <i>rejected</i>.") + "<br />" + rejection);
                } else {
                    feedbackValid(tr("Thank you for providing a changeset comment! " +
                            "This gives other mappers a better understanding of your intent."));
                }
            }
        }
    }

    /**
     * Validator for the changeset {@code source} tag
     */
    static class UploadSourceValidator extends UploadTextComponentValidator {

        UploadSourceValidator(JTextComponent tc, JLabel feedback) {
            super(tc, feedback);
        }

        @Override
        public void validate() {
            String uploadSource = getComponent().getText();
            if (Utils.isStripEmpty(uploadSource)) {
                feedbackWarning(tr("You did not specify a source for your changes.<br />" +
                        "It is technically allowed, but this information helps<br />" +
                        "other users to understand the origins of the data.<br /><br />" +
                        "If you spend a minute now to explain your change, you will make life<br />" +
                        "easier for many other mappers.").replace("<br />", " "));
            } else {
                final String rejection = UploadDialog.UploadAction.validateUploadTag(
                        uploadSource, "upload.source", Collections.emptyList(), Collections.emptyList(), Collections.emptyList());
                if (rejection != null) {
                    feedbackWarning(tr("Your changeset source is <i>rejected</i>.") + "<br />" + rejection);
                } else {
                    feedbackValid(tr("Thank you for providing the data source!"));
                }
            }
        }
    }
}
