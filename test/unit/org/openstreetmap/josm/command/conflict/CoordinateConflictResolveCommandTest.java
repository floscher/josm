// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.command.conflict;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openstreetmap.josm.JOSMFixture;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.conflict.Conflict;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.User;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.conflict.pair.MergeDecisionType;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;

import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;

/**
 * Unit tests of {@link CoordinateConflictResolveCommand} class.
 */
public class CoordinateConflictResolveCommandTest {

    private static OsmDataLayer layer;

    /**
     * Setup test.
     */
    @BeforeClass
    public static void setUpBeforeClass() {
        JOSMFixture.createUnitTestFixture().init(true);
        layer = new OsmDataLayer(new DataSet(), null, null);
        Main.main.addLayer(layer);
    }

    /**
     * Cleanup test resources.
     */
    @AfterClass
    public static void tearDownAfterClass() {
        Main.main.removeLayer(layer);
    }

    private static Conflict<Node> createConflict() {
        return new Conflict<>(new Node(LatLon.ZERO), new Node(new LatLon(50, 50)));
    }

    /**
     * Unit test of {@code CoordinateConflictResolveCommand#executeCommand} and {@code CoordinateConflictResolveCommand#undoCommand} methods.
     */
    @Test
    public void testExecuteKeepMineUndoCommand() {
        Conflict<Node> conflict = createConflict();
        CoordinateConflictResolveCommand cmd = new CoordinateConflictResolveCommand(conflict, MergeDecisionType.KEEP_MINE);
        assertTrue(cmd.executeCommand());
        assertEquals(LatLon.ZERO, conflict.getMy().getCoor());
        cmd.undoCommand();
        assertEquals(LatLon.ZERO, conflict.getMy().getCoor());
    }

    /**
     * Unit test of {@code CoordinateConflictResolveCommand#executeCommand} and {@code CoordinateConflictResolveCommand#undoCommand} methods.
     */
    @Test
    public void testExecuteKeepTheirUndoCommand() {
        Conflict<Node> conflict = createConflict();
        CoordinateConflictResolveCommand cmd = new CoordinateConflictResolveCommand(conflict, MergeDecisionType.KEEP_THEIR);
        assertTrue(cmd.executeCommand());
        assertEquals(conflict.getTheir().getCoor(), conflict.getMy().getCoor());
        cmd.undoCommand();
        //assertEquals(LatLon.ZERO, conflict.getMy().getCoor()); // FIXME it does not work
    }

    /**
     * Unit test of {@code CoordinateConflictResolveCommand#getDescriptionIcon} method.
     */
    @Test
    public void testGetDescriptionIcon() {
        Conflict<Node> conflict = createConflict();
        assertNotNull(new CoordinateConflictResolveCommand(conflict, null).getDescriptionIcon());
    }

    /**
     * Unit test of methods {@link CoordinateConflictResolveCommand#equals} and {@link CoordinateConflictResolveCommand#hashCode}.
     */
    @Test
    public void equalsContract() {
        EqualsVerifier.forClass(CoordinateConflictResolveCommand.class).usingGetClass()
            .withPrefabValues(Conflict.class,
                    new Conflict<>(new Node(), new Node()), new Conflict<>(new Way(), new Way()))
            .withPrefabValues(DataSet.class,
                    new DataSet(), new DataSet())
            .withPrefabValues(User.class,
                    User.createOsmUser(1, "foo"), User.createOsmUser(2, "bar"))
            .withPrefabValues(OsmDataLayer.class,
                    new OsmDataLayer(new DataSet(), "1", null), new OsmDataLayer(new DataSet(), "2", null))
            .suppress(Warning.NONFINAL_FIELDS)
            .verify();
    }
}
