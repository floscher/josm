// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.io;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.BeforeClass;
import org.junit.Test;
import org.openstreetmap.josm.JOSMFixture;

/**
 * Unit tests of {@link UploadDialog} class.
 */
public class UploadDialogTest {

    /**
     * Setup tests
     */
    @BeforeClass
    public static void setUpBeforeClass() {
        JOSMFixture.createUnitTestFixture().init();
    }

    private static IUploadDialog newUploadDialog(final String comment, final String source) {
        return new IUploadDialog() {

            @Override
            public void rememberUserInput() {
                // Do nothing
            }

            @Override
            public boolean isCanceled() {
                return false;
            }

            @Override
            public void handleMissingSource() {
                // Do nothing
            }

            @Override
            public void handleMissingComment() {
                // Do nothing
            }

            @Override
            public void handleIllegalChunkSize() {
                // Do nothing
            }

            @Override
            public UploadStrategySpecification getUploadStrategySpecification() {
                return new UploadStrategySpecification();
            }

            @Override
            public String getUploadSource() {
                return source;
            }

            @Override
            public String getUploadComment() {
                return comment;
            }

            @Override
            public Map<String, String> getTags(boolean keepEmpty) {
                return new ConcurrentHashMap<>();
            }
        };
    }

    /**
     * Test of {@link UploadDialog.CancelAction} class.
     */
    @Test
    public void testCancelAction() {
        new UploadDialog.CancelAction(newUploadDialog(null, null)).actionPerformed(null);
    }

    /**
     * Test of {@link UploadDialog.UploadAction} class.
     */
    @Test
    public void testUploadAction() {
        new UploadDialog.UploadAction(newUploadDialog("comment", "source")).actionPerformed(null);
        new UploadDialog.UploadAction(newUploadDialog("", "source")).actionPerformed(null);
        new UploadDialog.UploadAction(newUploadDialog("comment", "")).actionPerformed(null);
        new UploadDialog.UploadAction(newUploadDialog("a comment long enough", "a source long enough")).actionPerformed(null);
    }
}
