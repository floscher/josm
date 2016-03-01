// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.preferences.advanced;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.BeforeClass;
import org.junit.Test;
import org.openstreetmap.josm.JOSMFixture;
import org.openstreetmap.josm.data.preferences.StringSetting;

import nl.jqno.equalsverifier.EqualsVerifier;

/**
 * Unit tests of {@link PrefEntry} class.
 */
public class PrefEntryTest {

    /**
     * Setup test.
     */
    @BeforeClass
    public static void setUpBeforeClass() {
        JOSMFixture.createUnitTestFixture().init();
    }

    /**
     * Unit test of {@link PrefEntry#PrefEntry}.
     */
    @Test
    public void testPrefEntry()  {
        String key = "key";
        StringSetting val = new StringSetting("value");
        StringSetting def = new StringSetting("defaultValue");
        PrefEntry pe = new PrefEntry(key, val, def, false);
        assertFalse(pe.isDefault());
        assertEquals(key, pe.getKey());
        assertEquals(val, pe.getValue());
        assertEquals(def, pe.getDefaultValue());
        StringSetting val2 = new StringSetting("value2");
        assertFalse(pe.isChanged());
        pe.setValue(val2);
        assertEquals(val2, pe.getValue());
        assertEquals(val2.toString(), pe.toString());
        assertTrue(pe.isChanged());
        pe.reset();
        pe.markAsChanged();
        assertTrue(pe.isChanged());
    }

    /**
     * Unit test of methods {@link PrefEntry#equals} and {@link PrefEntry#hashCode}.
     */
    @Test
    public void equalsContract() {
        EqualsVerifier.forClass(PrefEntry.class).usingGetClass().verify();
    }
}
