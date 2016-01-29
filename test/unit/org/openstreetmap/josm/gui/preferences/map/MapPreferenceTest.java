// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.preferences.map;

import static org.junit.Assert.assertNotNull;

import org.junit.BeforeClass;
import org.junit.Test;
import org.openstreetmap.josm.JOSMFixture;
import org.openstreetmap.josm.gui.preferences.PreferencesTestUtils;

/**
 * Unit tests of {@link MapPreference} class.
 */
public class MapPreferenceTest {

    /**
     * Setup test.
     */
    @BeforeClass
    public static void setUpBeforeClass() {
        JOSMFixture.createUnitTestFixture().init();
    }

    /**
     * Unit test of {@link MapPreference#MapPreference}.
     */
    @Test
    public void testMapPreference()  {
        assertNotNull(new MapPreference.Factory().createPreferenceSetting());
    }

    /**
     * Unit test of {@link MapPreference#addGui}.
     */
    @Test
    public void testAddGui() {
        PreferencesTestUtils.testPreferenceSettingAddGui(new MapPreference.Factory(), null);
    }
}
