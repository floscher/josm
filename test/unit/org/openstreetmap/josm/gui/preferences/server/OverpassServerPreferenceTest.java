// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.preferences.server;

import static org.junit.Assert.assertNotNull;

import org.junit.BeforeClass;
import org.junit.Test;
import org.openstreetmap.josm.JOSMFixture;
import org.openstreetmap.josm.gui.preferences.PreferencesTestUtils;

/**
 * Unit tests of {@link OverpassServerPreference} class.
 */
public class OverpassServerPreferenceTest {

    /**
     * Setup test.
     */
    @BeforeClass
    public static void setUpBeforeClass() {
        JOSMFixture.createUnitTestFixture().init();
    }

    /**
     * Unit test of {@link OverpassServerPreference#OverpassServerPreference}.
     */
    @Test
    public void testOverpassServerPreference()  {
        assertNotNull(new OverpassServerPreference.Factory().createPreferenceSetting());
    }

    /**
     * Unit test of {@link OverpassServerPreference#addGui}.
     */
    @Test
    public void testAddGui() {
        PreferencesTestUtils.testPreferenceSettingAddGui(new OverpassServerPreference.Factory(), ServerAccessPreference.class);
    }
}
