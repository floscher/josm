// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.io;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;

import javax.swing.JLabel;
import javax.swing.JTextField;

import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

public class UploadTextComponentValidatorTest {

    /**
     * Setup test.
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules().preferences();

    /**
     * Unit test of {@link UploadTextComponentValidator.UploadCommentValidator}
     */
    @Test
    public void testUploadCommentValidator() {
        JTextField textField = new JTextField();
        JLabel feedback = new JLabel();
        new UploadTextComponentValidator.UploadCommentValidator(textField, feedback);
        assertThat(feedback.getText(), containsString("Your upload comment is <i>empty</i>, or <i>very short</i>"));
        textField.setText("a comment long enough");
        assertThat(feedback.getText(), containsString("Thank you for providing a changeset comment"));
        textField.setText("a");
        assertThat(feedback.getText(), containsString("Your upload comment is <i>empty</i>, or <i>very short</i>"));
    }

    /**
     * Unit test of {@link UploadTextComponentValidator.UploadSourceValidator}
     */
    @Test
    public void testUploadSourceValidator() {
        JTextField textField = new JTextField();
        JLabel feedback = new JLabel();
        new UploadTextComponentValidator.UploadSourceValidator(textField, feedback);
        assertThat(feedback.getText(), containsString("You did not specify a source for your changes"));
        textField.setText("a comment long enough");
        assertThat(feedback.getText(), containsString("Thank you for providing the data source"));
    }
}
