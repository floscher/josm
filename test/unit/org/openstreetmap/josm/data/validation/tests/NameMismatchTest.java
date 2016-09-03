// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.validation.tests;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.data.osm.OsmUtils;
import org.openstreetmap.josm.data.validation.TestError;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * JUnit Test of "Name mismatch" validation test.
 */
public class NameMismatchTest {

    /**
     * Setup test.
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules();

    List<TestError> test(String primitive) {
        final NameMismatch test = new NameMismatch();
        test.check(OsmUtils.createPrimitive(primitive));
        return test.getErrors();
    }

    /**
     * Test "A name is missing, even though name:* exists."
     */
    @Test
    public void testCase0() {
        final List<TestError> errors = test("node name:de=Europa");
        assertEquals(1, errors.size());
        assertEquals("A name is missing, even though name:* exists.", errors.get(0).getMessage());
    }

    /**
     * Test "Missing name:*={0}. Add tag with correct language key."
     */
    @Test
    public void testCase1() {
        final List<TestError> errors = test("node name=Europe name:de=Europa");
        assertEquals(1, errors.size());
        assertEquals("Missing name:*=Europe. Add tag with correct language key.", errors.get(0).getDescription());
    }

    /**
     * Test no error
     */
    @Test
    public void testCase2() {
        final List<TestError> errors = test("node name=Europe name:de=Europa name:en=Europe");
        assertEquals(0, errors.size());
    }

    /**
     * Various other tests
     */
    @Test
    public void testCase3() {
        List<TestError> errors;
        errors = test("node \"name\"=\"Italia - Italien - Italy\"");
        assertEquals(0, errors.size());
        errors = test("node name=\"Italia - Italien - Italy\" name:it=Italia");
        assertEquals(2, errors.size());
        errors = test("node name=\"Italia - Italien - Italy\" name:it=Italia name:de=Italien");
        assertEquals(1, errors.size());
        assertEquals("Missing name:*=Italy. Add tag with correct language key.", errors.get(0).getDescription());
        errors = test("node name=\"Italia - Italien - Italy\" name:it=Italia name:de=Italien name:en=Italy");
        assertEquals(0, errors.size());
    }
}
