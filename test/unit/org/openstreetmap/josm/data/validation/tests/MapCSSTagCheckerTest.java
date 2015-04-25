package org.openstreetmap.josm.data.validation.tests;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.io.StringReader;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.junit.BeforeClass;
import org.junit.Test;
import org.openstreetmap.josm.JOSMFixture;
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
import org.openstreetmap.josm.data.validation.tests.MapCSSTagChecker.TagCheck;

/**
 * JUnit Test of MapCSS TagChecker.
 */
public class MapCSSTagCheckerTest {

    /**
     * Setup test.
     */
    @BeforeClass
    public static void setUp() {
        JOSMFixture.createUnitTestFixture().init();
    }

    @Test
    public void testNaturalMarsh() throws Exception {

        final List<MapCSSTagChecker.TagCheck> checks = MapCSSTagChecker.TagCheck.readMapCSS(new StringReader("" +
                "*[natural=marsh] {\n" +
                "   throwWarning: tr(\"{0}={1} is deprecated\", \"{0.key}\", tag(\"natural\"));\n" +
                "   fixRemove: \"{0.key}\";\n" +
                "   fixAdd: \"natural=wetland\";\n" +
                "   fixAdd: \"wetland=marsh\";\n" +
                "}"));
        assertThat(checks.size(), is(1));
        final MapCSSTagChecker.TagCheck check = checks.get(0);
        assertThat(check, notNullValue());
        assertThat(check.getDescription(null), is("{0.key}=null is deprecated"));
        assertThat(check.fixCommands.get(0).toString(), is("fixRemove: {0.key}"));
        assertThat(check.fixCommands.get(1).toString(), is("fixAdd: natural=wetland"));
        assertThat(check.fixCommands.get(2).toString(), is("fixAdd: wetland=marsh"));
        final Node n1 = new Node();
        n1.put("natural", "marsh");
        assertTrue(check.evaluate(n1));
        assertThat(check.getErrorForPrimitive(n1).getMessage(), is("natural=marsh is deprecated"));
        assertThat(check.getErrorForPrimitive(n1).getSeverity(), is(Severity.WARNING));
        assertThat(check.fixPrimitive(n1).getDescriptionText(), is("Sequence: Fix of natural=marsh is deprecated"));
        assertThat(((ChangePropertyCommand) check.fixPrimitive(n1).getChildren().iterator().next()).getTags().toString(),
                is("{natural=}"));
        final Node n2 = new Node();
        n2.put("natural", "wood");
        assertFalse(check.evaluate(n2));
        assertThat(MapCSSTagChecker.TagCheck.insertArguments(check.rule.selectors.get(0), "The key is {0.key} and the value is {0.value}"),
                is("The key is natural and the value is marsh"));
    }

    @Test
    public void test10913() throws Exception {
        final OsmPrimitive p = OsmUtils.createPrimitive("way highway=tertiary construction=yes");
        final TagCheck check = TagCheck.readMapCSS(new StringReader("way {" +
                "throwError: \"error\";" +
                "fixChangeKey: \"highway => construction\";\n" +
                "fixAdd: \"highway=construction\";\n" +
                "}")).get(0);
        final Command command = check.fixPrimitive(p);
        assertThat(command instanceof SequenceCommand, is(true));
        final Iterator<PseudoCommand> it = command.getChildren().iterator();
        assertThat(it.next() instanceof ChangePropertyKeyCommand, is(true));
        assertThat(it.next() instanceof ChangePropertyCommand, is(true));
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
