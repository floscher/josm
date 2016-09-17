// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.validation.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.StringReader;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.command.ChangePropertyCommand;
import org.openstreetmap.josm.command.ChangePropertyKeyCommand;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.command.PseudoCommand;
import org.openstreetmap.josm.command.SequenceCommand;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.OsmUtils;
import org.openstreetmap.josm.data.validation.Severity;
import org.openstreetmap.josm.data.validation.TestError;
import org.openstreetmap.josm.data.validation.tests.MapCSSTagChecker.ParseResult;
import org.openstreetmap.josm.data.validation.tests.MapCSSTagChecker.TagCheck;
import org.openstreetmap.josm.gui.mappaint.mapcss.parsergen.ParseException;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * JUnit Test of {@link MapCSSTagChecker}.
 */
public class MapCSSTagCheckerTest {

    /**
     * Setup test.
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules();

    static MapCSSTagChecker buildTagChecker(String css) throws ParseException {
        final MapCSSTagChecker test = new MapCSSTagChecker();
        test.checks.putAll("test", TagCheck.readMapCSS(new StringReader(css)).parseChecks);
        return test;
    }

    /**
     * Test {@code natural=marsh}.
     * @throws ParseException if a parsing error occurs
     */
    @Test
    public void testNaturalMarsh() throws ParseException {
        ParseResult result = MapCSSTagChecker.TagCheck.readMapCSS(new StringReader(
                "*[natural=marsh] {\n" +
                "   group: tr(\"deprecated\");\n" +
                "   throwWarning: tr(\"{0}={1} is deprecated\", \"{0.key}\", tag(\"natural\"));\n" +
                "   fixRemove: \"{0.key}\";\n" +
                "   fixAdd: \"natural=wetland\";\n" +
                "   fixAdd: \"wetland=marsh\";\n" +
                "}"));
        final List<MapCSSTagChecker.TagCheck> checks = result.parseChecks;
        assertEquals(1, checks.size());
        assertTrue(result.parseErrors.isEmpty());
        final MapCSSTagChecker.TagCheck check = checks.get(0);
        assertNotNull(check);
        assertEquals("{0.key}=null is deprecated", check.getDescription(null));
        assertEquals("fixRemove: {0.key}", check.fixCommands.get(0).toString());
        assertEquals("fixAdd: natural=wetland", check.fixCommands.get(1).toString());
        assertEquals("fixAdd: wetland=marsh", check.fixCommands.get(2).toString());
        final Node n1 = new Node();
        n1.put("natural", "marsh");
        assertTrue(check.test(n1));
        assertEquals("deprecated", check.getErrorForPrimitive(n1).getMessage());
        assertEquals("natural=marsh is deprecated", check.getErrorForPrimitive(n1).getDescription());
        assertEquals(Severity.WARNING, check.getErrorForPrimitive(n1).getSeverity());
        assertEquals("Sequence: Fix of natural=marsh is deprecated", check.fixPrimitive(n1).getDescriptionText());
        assertEquals("{natural=}", ((ChangePropertyCommand) check.fixPrimitive(n1).getChildren().iterator().next()).getTags().toString());
        final Node n2 = new Node();
        n2.put("natural", "wood");
        assertFalse(check.test(n2));
        assertEquals("The key is natural and the value is marsh",
                MapCSSTagChecker.TagCheck.insertArguments(check.rule.selectors.get(0), "The key is {0.key} and the value is {0.value}", null));
    }

    /**
     * Non-regression test for <a href="https://josm.openstreetmap.de/ticket/10913">Bug #10913</a>.
     * @throws ParseException if a parsing error occurs
     */
    @Test
    public void testTicket10913() throws ParseException {
        final OsmPrimitive p = OsmUtils.createPrimitive("way highway=tertiary construction=yes");
        final TagCheck check = TagCheck.readMapCSS(new StringReader("way {" +
                "throwError: \"error\";" +
                "fixChangeKey: \"highway => construction\";\n" +
                "fixAdd: \"highway=construction\";\n" +
                "}")).parseChecks.get(0);
        final Command command = check.fixPrimitive(p);
        assertTrue(command instanceof SequenceCommand);
        final Iterator<PseudoCommand> it = command.getChildren().iterator();
        assertTrue(it.next() instanceof ChangePropertyKeyCommand);
        assertTrue(it.next() instanceof ChangePropertyCommand);
    }

    /**
     * Non-regression test for <a href="https://josm.openstreetmap.de/ticket/9782">Bug #9782</a>.
     * @throws ParseException if a parsing error occurs
     */
    @Test
    public void testTicket9782() throws ParseException {
        final MapCSSTagChecker test = buildTagChecker("*[/.+_name/][!name] {" +
                "throwWarning: tr(\"has {0} but not {1}\", \"{0.key}\", \"{1.key}\");}");
        final OsmPrimitive p = OsmUtils.createPrimitive("way alt_name=Foo");
        final Collection<TestError> errors = test.getErrorsForPrimitive(p, false);
        assertEquals(1, errors.size());
        assertEquals("has alt_name but not name", errors.iterator().next().getMessage());
        assertEquals("3000_*[.+_name][!name]", errors.iterator().next().getIgnoreSubGroup());
    }

    /**
     * Non-regression test for <a href="https://josm.openstreetmap.de/ticket/10859">Bug #10859</a>.
     * @throws ParseException if a parsing error occurs
     */
    @Test
    public void testTicket10859() throws ParseException {
        final MapCSSTagChecker test = buildTagChecker("way[highway=footway][foot?!] {\n" +
                "  throwWarning: tr(\"{0} used with {1}\", \"{0.value}\", \"{1.tag}\");}");
        final OsmPrimitive p = OsmUtils.createPrimitive("way highway=footway foot=no");
        final Collection<TestError> errors = test.getErrorsForPrimitive(p, false);
        assertEquals(1, errors.size());
        assertEquals("footway used with foot=no", errors.iterator().next().getMessage());
        assertEquals("3000_way[highway=footway][foot]", errors.iterator().next().getIgnoreSubGroup());
    }

    /**
     * Non-regression test for <a href="https://josm.openstreetmap.de/ticket/13630">Bug #13630</a>.
     * @throws ParseException if a parsing error occurs
     */
    @Test
    public void testTicket13630() throws ParseException {
        ParseResult result = MapCSSTagChecker.TagCheck.readMapCSS(new StringReader(
                "node[crossing=zebra] {fixRemove: \"crossing=zebra\";}"));
        assertTrue(result.parseChecks.isEmpty());
        assertEquals(1, result.parseErrors.size());
    }

    @Test
    public void testPreprocessing() throws ParseException {
        final MapCSSTagChecker test = buildTagChecker("" +
                "@supports (min-josm-version: 1) { *[foo] { throwWarning: \"!\"; } }\n" +
                "@supports (min-josm-version: 2147483647) { *[bar] { throwWarning: \"!\"; } }\n");
        assertEquals(1, test.getErrorsForPrimitive(OsmUtils.createPrimitive("way foo=1"), false).size());
        assertEquals(0, test.getErrorsForPrimitive(OsmUtils.createPrimitive("way bar=1"), false).size());
    }

    @Test
    public void testInit() throws Exception {
        MapCSSTagChecker c = new MapCSSTagChecker();
        c.initialize();

        Set<String> assertionErrors = new LinkedHashSet<>();
        for (Set<TagCheck> schecks : c.checks.values()) {
            assertionErrors.addAll(c.checkAsserts(schecks));
        }
        for (String msg : assertionErrors) {
            Main.error(msg);
        }
        assertTrue("not all assertions included in the tests are met", assertionErrors.isEmpty());
    }
}
