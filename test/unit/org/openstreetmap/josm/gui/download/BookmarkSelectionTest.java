// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.download;

import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unit tests of {@link BookmarkSelection} class.
 */
public class BookmarkSelectionTest {

    /**
     * Setup tests
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules().preferences();

    /**
     * Test for {@link BookmarkSelection#BookmarkSelection}.
     */
    @Test
    public void testBookmarkSelection() {
        BookmarkSelection sel = new BookmarkSelection();
        sel.addGui(null);
        sel.setDownloadArea(null);
        sel.setDownloadArea(new Bounds(0, 0, 1, 1));
    }
}
