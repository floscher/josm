// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.preferences.projection;

import static org.junit.Assert.assertNotNull;

import org.junit.BeforeClass;
import org.junit.Test;
import org.openstreetmap.josm.JOSMFixture;
import org.openstreetmap.josm.gui.preferences.PreferencesTestUtils;
import org.openstreetmap.josm.gui.preferences.map.MapPreference;

/**
 * Unit tests of {@link ProjectionPreference} class.
 */
public class ProjectionPreferenceTest {

    /**
     * Setup test.
     */
    @BeforeClass
    public static void setUpBeforeClass() {
        JOSMFixture.createUnitTestFixture().init();
    }

    /**
     * Unit test of {@link ProjectionPreference#ProjectionPreference}.
     */
    @Test
    public void testProjectionPreference()  {
        assertNotNull(new ProjectionPreference.Factory().createPreferenceSetting());
    }

    /**
     * Unit test of {@link ProjectionPreference#addGui}.
     */
    @Test
    public void testAddGui() {
        PreferencesTestUtils.testPreferenceSettingAddGui(new ProjectionPreference.Factory(), MapPreference.class);
    }
}
