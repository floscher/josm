// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.io;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.BeforeClass;
import org.junit.Test;
import org.openstreetmap.josm.JOSMFixture;
import org.openstreetmap.josm.gui.io.CredentialDialog.CredentialPanel;

/**
 * Unit tests of {@link CredentialDialog} class.
 */
public class CredentialDialogTest {

    /**
     * Setup tests
     */
    @BeforeClass
    public static void setUpBeforeClass() {
        JOSMFixture.createUnitTestFixture().init();
    }

    /**
     * Test of {@link CredentialDialog.CredentialPanel} class.
     */
    @Test
    public void testCredentialPanel() {
        CredentialPanel cp = new CredentialPanel(null);
        cp.build();

        cp.init(null, null);
        assertEquals("", cp.getUserName());
        assertArrayEquals("".toCharArray(), cp.getPassword());
        assertFalse(cp.isSaveCredentials());

        cp.init("user", "password");
        assertEquals("user", cp.getUserName());
        assertArrayEquals("password".toCharArray(), cp.getPassword());
        assertTrue(cp.isSaveCredentials());

        cp.updateWarningLabel(null);
        cp.updateWarningLabel("http://something_insecure");
        cp.updateWarningLabel("https://something_secure");

        cp.startUserInput();
    }
}
