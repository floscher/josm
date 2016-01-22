// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.tagging.presets.items;

import static org.junit.Assert.assertEquals;

import org.junit.BeforeClass;
import org.junit.Test;
import org.openstreetmap.josm.JOSMFixture;
import org.openstreetmap.josm.gui.tagging.presets.items.ComboMultiSelect.PresetListEntry;

/**
 * Unit tests of {@link ComboMultiSelect} class.
 */
public class ComboMultiSelectTest {

    /**
     * Setup test.
     */
    @BeforeClass
    public static void setUp() {
        JOSMFixture.createUnitTestFixture().init();
    }

    /**
     * Non-regression test for ticket <a href="https://josm.openstreetmap.de/ticket/12416">#12416</a>.
     */
    @Test
    public void testTicket12416() {
        assertEquals("&nbsp;", new PresetListEntry("").getListDisplay());
    }
}
