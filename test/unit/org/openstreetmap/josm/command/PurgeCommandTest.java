// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.command;

import org.junit.BeforeClass;
import org.junit.Test;
import org.openstreetmap.josm.JOSMFixture;
import org.openstreetmap.josm.data.conflict.Conflict;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Hash;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Storage;
import org.openstreetmap.josm.data.osm.User;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;

import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;

/**
 * Unit tests of {@link PurgeCommand} class.
 */
public class PurgeCommandTest {

    /**
     * Setup test.
     */
    @BeforeClass
    public static void setUpBeforeClass() {
        JOSMFixture.createUnitTestFixture().init(false);
    }

    /**
     * Unit test of methods {@link PurgeCommand#equals} and {@link PurgeCommand#hashCode}.
     */
    @Test
    public void equalsContract() {
        EqualsVerifier.forClass(PurgeCommand.class).usingGetClass()
            .withPrefabValues(DataSet.class,
                new DataSet(), new DataSet())
            .withPrefabValues(User.class,
                    User.createOsmUser(1, "foo"), User.createOsmUser(2, "bar"))
            .withPrefabValues(Conflict.class,
                    new Conflict<>(new Node(1, 1), new Node(2, 1)),
                    new Conflict<>(new Node(1, 1), new Node(3, 1)))
            .withPrefabValues(OsmDataLayer.class,
                new OsmDataLayer(new DataSet(), "1", null), new OsmDataLayer(new DataSet(), "2", null))
            .withPrefabValues(Hash.class,
                Storage.<OsmPrimitive>defaultHash(), Storage.<OsmPrimitive>defaultHash())
            .suppress(Warning.NONFINAL_FIELDS)
            .verify();
    }
}
