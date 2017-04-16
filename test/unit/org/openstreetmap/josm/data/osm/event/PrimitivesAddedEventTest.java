// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm.event;

import static org.junit.Assert.assertEquals;

import java.util.Collections;

import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unit tests of {@link PrimitivesAddedEvent} class.
 */
public class PrimitivesAddedEventTest {

    /**
     * Setup test.
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules();

    /**
     * Unit test of {@link PrimitivesAddedEvent#toString}.
     */
    @Test
    public void testToString() {
        assertEquals("PRIMITIVES_ADDED", new PrimitivesAddedEvent(null, Collections.emptyList(), false).toString());
    }
}
