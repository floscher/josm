// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.validation.util;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;

import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unit tests for class {@link MultipleNameVisitor}.
 */
public class MultipleNameVisitorTest {

    /**
     * Setup test.
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules().preferences();

    /**
     * Non-regression test for bug #11967.
     */
    @Test
    public void testTicket11967() {
        MultipleNameVisitor visitor = new MultipleNameVisitor();
        visitor.visit(Arrays.asList(new Way(), new Way()));
        assertEquals("2 ways: \u20680\u2069 \u2068(0 nodes)\u2069, \u20680\u2069 \u2068(0 nodes)\u2069", visitor.toString());
    }

    /**
     * Non-regression test for bug #16652.
     */
    @Test
    public void testTicket16652() {
        MultipleNameVisitor visitor = new MultipleNameVisitor();
        visitor.visit(Arrays.asList(
                TestUtils.newNode("name=foo"),
                TestUtils.newWay("addr:housename=Stark"),
                TestUtils.newRelation("type=route")));
        assertEquals("3 objects: \u2068foo\u2069, \u2068House Stark\u2069 \u2068(0 nodes)\u2069, route (0, 0 members)", visitor.toString());
    }
}
