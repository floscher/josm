// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

import org.junit.BeforeClass;
import org.junit.Test;
import org.openstreetmap.josm.JOSMFixture;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;

/**
 * Unit tests for class {@link SplitWayAction}.
 */
public final class SplitWayActionTest {

    /** Class under test. */
    private static SplitWayAction action;

    /**
     * Setup test.
     */
    @BeforeClass
    public static void setUp() {
        JOSMFixture.createUnitTestFixture().init(true);

        // Enable "Align in line" feature.
        action = Main.main.menu.splitWay;
        action.setEnabled(true);
    }

    /**
     * Test case: When node is share by multiple ways, split selected way.
     * see #11184
     */
    @Test
    public void testTicket11184() {
        DataSet dataSet = new DataSet();
        OsmDataLayer layer = new OsmDataLayer(dataSet, OsmDataLayer.createNewName(), null);

        Node n1 = new Node(new EastNorth(0, 0));
        Node n2 = new Node(new EastNorth(-1, 1));
        Node n3 = new Node(new EastNorth(1, 1));
        Node n4 = new Node(new EastNorth(-1, -1));
        Node n5 = new Node(new EastNorth(1, -1));
        Node n6 = new Node(new EastNorth(-1, 0));
        Node n7 = new Node(new EastNorth(1, 0));
        dataSet.addPrimitive(n1);
        dataSet.addPrimitive(n2);
        dataSet.addPrimitive(n3);
        dataSet.addPrimitive(n4);
        dataSet.addPrimitive(n5);
        dataSet.addPrimitive(n6);
        dataSet.addPrimitive(n7);

        Way w1 = new Way();
        Node[] w1NodesArray = new Node[] {n6, n1, n7};
        w1.setNodes(Arrays.asList(w1NodesArray));
        Way w2 = new Way();
        w2.setNodes(Arrays.asList(new Node[] {n1, n2, n3, n1, n4, n5, n1}));
        dataSet.addPrimitive(w1);
        dataSet.addPrimitive(w2);

        dataSet.addSelected(n1);
        dataSet.addSelected(w2);

        try {
            Main.main.addLayer(layer);
            action.actionPerformed(null);
        } finally {
            // Ensure we clean the place before leaving, even if test fails.
            Main.map.mapView.removeLayer(layer);
        }

        // Ensures 3 ways.
        assertSame(String.format("Found %d ways after split action instead of 3.", dataSet.getWays().size()),
                   dataSet.getWays().size(), 3);

        // Ensures way w1 is unchanged.
        assertTrue("Unselected ways disappear during split action.",
                   dataSet.getWays().contains(w1));
        assertSame("Unselected way seems to have change during split action.",
                   w1.getNodesCount(), 3);
        for (int i = 0; i < 3; i++) {
            assertSame("Node change in unselected way during split action.",
                       w1.getNode(i), w1NodesArray[i]);
        }
    }

    @Test
    public void testRouteRelation() {
        doTestRouteRelation(false, 0);
        doTestRouteRelation(false, 1);
        doTestRouteRelation(false, 2);
        doTestRouteRelation(false, 3);
        doTestRouteRelation(true, 0);
        doTestRouteRelation(true, 1);
        doTestRouteRelation(true, 2);
        doTestRouteRelation(true, 3);
    }

    void doTestRouteRelation(final boolean wayIsReversed, final int indexOfWayToKeep) {
        final DataSet dataSet = new DataSet();
        final OsmDataLayer layer = new OsmDataLayer(dataSet, OsmDataLayer.createNewName(), null);
        final Node n1 = new Node(new LatLon(1, 0));
        final Node n2 = new Node(new LatLon(2, 0));
        final Node n3 = new Node(new LatLon(3, 0));
        final Node n4 = new Node(new LatLon(4, 0));
        final Node n5 = new Node(new LatLon(5, 0));
        final Node n6 = new Node(new LatLon(6, 0));
        final Node n7 = new Node(new LatLon(7, 0));
        final Way w1 = new Way();
        final Way w2 = new Way();
        final Way w3 = new Way();
        final Relation route = new Relation();
        for (OsmPrimitive p : Arrays.asList(n1, n2, n3, n4, n5, n6, n7, w1, w2, w3, route)) {
            dataSet.addPrimitive(p);
        }
        w1.setNodes(Arrays.asList(n1, n2));
        w2.setNodes(wayIsReversed
                ? Arrays.asList(n6, n5, n4, n3, n2)
                : Arrays.asList(n2, n3, n4, n5, n6)
        );
        w3.setNodes(Arrays.asList(n6, n7));
        route.put("type", "route");
        route.addMember(new RelationMember("", w1));
        route.addMember(new RelationMember("", w2));
        route.addMember(new RelationMember("", w3));
        dataSet.setSelected(Arrays.asList(w2, n3, n4, n5));


        final SplitWayAction.Strategy strategy = new SplitWayAction.Strategy() {

            @Override
            public Way determineWayToKeep(Iterable<Way> wayChunks) {
                final Iterator<Way> it = wayChunks.iterator();
                for (int i = 0; i < indexOfWayToKeep; i++) {
                    it.next();
                }
                return it.next();
            }
        };
        final SplitWayAction.SplitWayResult result = SplitWayAction.splitWay(
                layer, w2, SplitWayAction.buildSplitChunks(w2, Arrays.asList(n3, n4, n5)), new ArrayList<OsmPrimitive>(), strategy);
        Main.main.undoRedo.add(result.getCommand());

        assertEquals(6, route.getMembersCount());
        assertEquals(w1, route.getMemberPrimitivesList().get(0));
        assertEquals(w3, route.getMemberPrimitivesList().get(5));
        assertFirstLastNodeIs(((Way) route.getMemberPrimitivesList().get(0)), n1);
        assertFirstLastNodeIs(((Way) route.getMemberPrimitivesList().get(0)), n2);
        assertFirstLastNodeIs(((Way) route.getMemberPrimitivesList().get(1)), n2);
        assertFirstLastNodeIs(((Way) route.getMemberPrimitivesList().get(1)), n3);
        assertFirstLastNodeIs(((Way) route.getMemberPrimitivesList().get(2)), n3);
        assertFirstLastNodeIs(((Way) route.getMemberPrimitivesList().get(2)), n4);
        assertFirstLastNodeIs(((Way) route.getMemberPrimitivesList().get(3)), n4);
        assertFirstLastNodeIs(((Way) route.getMemberPrimitivesList().get(3)), n5);
        assertFirstLastNodeIs(((Way) route.getMemberPrimitivesList().get(4)), n5);
        assertFirstLastNodeIs(((Way) route.getMemberPrimitivesList().get(4)), n6);
        assertFirstLastNodeIs(((Way) route.getMemberPrimitivesList().get(5)), n6);
        assertFirstLastNodeIs(((Way) route.getMemberPrimitivesList().get(5)), n7);

    }

    static void assertFirstLastNodeIs(Way way, Node node) {
        assertTrue("First/last node of " + way + " should be " + node, node.equals(way.firstNode()) || node.equals(way.lastNode()));
    }
}
