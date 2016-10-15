// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.validator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;

import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.validation.Severity;
import org.openstreetmap.josm.data.validation.TestError;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unit tests of {@link ValidatorTreePanel} class.
 */
public class ValidatorTreePanelTest {

    /**
     * Setup tests
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules().preferences();

    /**
     * Unit test of {@link ValidatorTreePanel#ValidatorTreePanel}.
     */
    @Test
    public void testValidatorTreePanel() {
        assertNotNull(new ValidatorTreePanel());

        ValidatorTreePanel vtp = new ValidatorTreePanel(new ArrayList<>(Arrays.asList(
                TestError.builder(null, Severity.ERROR, 0)
                        .message("err")
                        .primitives(new Node(1))
                        .build(),
                TestError.builder(null, Severity.WARNING, 0)
                        .message("warn", "foo")
                        .primitives(new Node(2))
                        .build(),
                TestError.builder(null, Severity.WARNING, 0)
                        .message("warn", "bar")
                        .primitives(new Node(2))
                        .build())));
        assertNotNull(vtp);
        final Enumeration nodes = vtp.getRoot().breadthFirstEnumeration();
        assertEquals("", nodes.nextElement().toString());
        assertEquals("Errors (1)", nodes.nextElement().toString());
        assertEquals("Warnings (2)", nodes.nextElement().toString());
        assertEquals("err (1)", nodes.nextElement().toString());
        assertEquals("warn (2)", nodes.nextElement().toString());
        nodes.nextElement();
        assertEquals("bar (1)", nodes.nextElement().toString());
        assertEquals("foo (1)", nodes.nextElement().toString());
        vtp.setVisible(true);
        vtp.setVisible(false);
        Node n = new Node(10);
        vtp.setErrors(Arrays.asList(TestError.builder(null, Severity.ERROR, 0)
                .message("")
                .primitives(n)
                .build()));
        assertEquals(1, vtp.getErrors().size());
        vtp.selectRelatedErrors(Collections.<OsmPrimitive>singleton(n));
        vtp.expandAll();
        assertNotNull(vtp.getRoot());
        vtp.resetErrors();
        Set<? extends OsmPrimitive> filter = new HashSet<>(Arrays.asList(n));
        vtp.setFilter(filter);
        assertEquals(filter, vtp.getFilter());
        vtp.setFilter(new HashSet<OsmPrimitive>());
        assertNull(vtp.getFilter());
        vtp.setFilter(null);
        assertNull(vtp.getFilter());
        vtp.destroy();
    }
}
