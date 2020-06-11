// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.Collections;

import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.hot.sds.SeparateDataStorePlugin;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Integration tests of {@link Territories} class.
 */
public class TerritoriesTestIT {

    /**
     * Test rules.
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules rules = new JOSMTestRules().projection().main();


    /**
     * Test of {@link Territories#initialize} method.
     */
    @Test
    public void testUtilityClass() {
        new SeparateDataStorePlugin(null);
        Logging.clearLastErrorAndWarnings();
        Territories.initialize();
        assertEquals("no errors or warnings", Collections.emptyList(), Logging.getLastErrorAndWarnings());
        assertFalse("customTagsCache is non empty", Territories.customTagsCache.isEmpty());
        assertFalse("iso3166Cache is non empty", Territories.iso3166Cache.isEmpty());
        assertFalse("taginfoCache is non empty", Territories.taginfoCache.isEmpty());
        assertFalse("taginfoGeofabrikCache is non empty", Territories.taginfoGeofabrikCache.isEmpty());
    }
}
