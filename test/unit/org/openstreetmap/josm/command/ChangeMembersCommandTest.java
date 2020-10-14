// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.command;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.command.CommandTest.CommandTestDataWithRelation;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.User;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;

/**
 * Unit tests of {@link ChangeMembersCommand} class.
 */
public class ChangeMembersCommandTest {

    /**
     * We need prefs for nodes.
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules().preferences().i18n();
    private CommandTestDataWithRelation testData;

    /**
     * Set up the test data.
     */
    @Before
    public void createTestData() {
        testData = new CommandTestDataWithRelation();
    }

    /**
     * Test {@link ChangeMembersCommand#executeCommand()}
     */
    @Test
    public void testChange() {
        assertTrue(testData.existingNode.getReferrers().contains(testData.existingRelation));
        assertEquals(2, testData.existingRelation.getMembersCount());
        List<RelationMember> members = testData.existingRelation.getMembers();
        members.add(new RelationMember("n2", testData.existingNode2));
        new ChangeMembersCommand(testData.existingRelation, members).executeCommand();
        assertEquals(3, testData.existingRelation.getMembersCount());
        members = testData.existingRelation.getMembers();
        members.remove(0);
        new ChangeMembersCommand(testData.existingRelation, members).executeCommand();
        assertEquals(2, testData.existingRelation.getMembersCount());
        assertTrue(testData.existingRelation.getMembersFor(Collections.singleton(testData.existingNode)).isEmpty());
        assertEquals(testData.existingWay, testData.existingRelation.getMember(0).getMember());
        assertEquals(testData.existingNode2, testData.existingRelation.getMember(1).getMember());
    }

    /**
     * Test {@link ChangeMembersCommand#undoCommand()}
     */
    @Test
    public void testUndo() {
        List<RelationMember> members = testData.existingRelation.getMembers();
        members.add(new RelationMember("n2", testData.existingNode2));
        Command command = new ChangeMembersCommand(testData.existingRelation, members);
        command.executeCommand();

        assertEquals(3, testData.existingRelation.getMembersCount());

        command.undoCommand();
        assertEquals(2, testData.existingRelation.getMembersCount());
    }

    /**
     * Test {@link ChangeMembersCommand#getDescriptionText()}
     */
    @Test
    public void testDescription() {
        testData.existingRelation.put("name", "xy");
        List<RelationMember> members = testData.existingRelation.getMembers();
        members.remove(1);
        assertTrue(new ChangeMembersCommand(testData.existingRelation, members).getDescriptionText().matches("Change members of .*xy.*"));
    }

    /**
     * Unit test of methods {@link ChangeMembersCommand#equals} and {@link ChangeMembersCommand#hashCode}.
     */
    @Test
    public void testEqualsContract() {
        TestUtils.assumeWorkingEqualsVerifier();
        EqualsVerifier.forClass(ChangeMembersCommand.class).usingGetClass()
            .withPrefabValues(DataSet.class,
                new DataSet(), new DataSet())
            .withPrefabValues(User.class,
                    User.createOsmUser(1, "foo"), User.createOsmUser(2, "bar"))
            .withPrefabValues(OsmPrimitive.class,
                new Node(1), new Node(2))
            .withPrefabValues(OsmDataLayer.class,
                new OsmDataLayer(new DataSet(), "1", null), new OsmDataLayer(new DataSet(), "2", null))
            .suppress(Warning.NONFINAL_FIELDS)
            .verify();
    }

}
