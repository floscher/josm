// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui;

import static org.junit.Assert.assertFalse;

import java.io.IOException;

import org.junit.BeforeClass;
import org.junit.Test;
import org.openstreetmap.josm.JOSMFixture;

/**
 * Tests the {@link GettingStarted} class.
 */
public class GettingStartedTest {

    /**
     * Setup test.
     */
    @BeforeClass
    public static void init() {
        JOSMFixture.createFunctionalTestFixture().init();
    }

    /**
     * Tests that image links are replaced.
     *
     * @throws IOException if any I/O error occurs
     */
    @Test
    public void testImageReplacement() throws IOException {
        final String motd = new GettingStarted.MotdContent().updateIfRequiredString();
        // assuming that the MOTD contains one image included, fixImageLinks changes the HTML string
        assertFalse(GettingStarted.fixImageLinks(motd).equals(motd));
    }

    /**
     * Tests that image links are replaced.
     */
    @Test
    public void testImageReplacementStatic() {
        final String html = "the download button <img src=\"/browser/trunk/images/download.png?format=raw\" " +
                "alt=\"source:trunk/images/download.png\" title=\"source:trunk/images/download.png\" />.";
        assertFalse(GettingStarted.fixImageLinks(html).equals(html));

    }
}
