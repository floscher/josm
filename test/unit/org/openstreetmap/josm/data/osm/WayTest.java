// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.junit.BeforeClass;
import org.junit.Test;
import org.openstreetmap.josm.JOSMFixture;
import org.openstreetmap.josm.data.coor.LatLon;

/**
 * Unit tests of the {@code Way} class.
 * @since 11270
 */
public class WayTest {

    /**
     * Setup test.
     */
    @BeforeClass
    public static void setUpBeforeClass() {
        JOSMFixture.createUnitTestFixture().init();
    }

    /**
     * Test BBox calculation with Way
     */
    @Test
    public void testBBox() {
        DataSet ds = new DataSet();
        Node n1 = new Node(1);
        Node n2 = new Node(2);
        Node n3 = new Node(3);
        Node n4 = new Node(4);
        n1.setIncomplete(true);
        n2.setCoor(new LatLon(10, 10));
        n3.setCoor(new LatLon(20, 20));
        n4.setCoor(new LatLon(90, 180));
        ds.addPrimitive(n1);
        ds.addPrimitive(n2);
        ds.addPrimitive(n3);
        ds.addPrimitive(n4);
        Way way = new Way(1);
        assertFalse(way.getBBox().isValid());
        way.setNodes(Arrays.asList(n1));
        assertFalse(way.getBBox().isValid());
        way.setNodes(Arrays.asList(n2));
        assertTrue(way.getBBox().isValid());
        way.setNodes(Arrays.asList(n1, n2));
        assertTrue(way.getBBox().isValid());
        assertEquals(way.getBBox(), new BBox(10, 10));
    }

    /**
     * Test that {@link Way#cloneFrom} throws IAE for invalid arguments
     */
    @Test(expected = IllegalArgumentException.class)
    public void testCloneFromIAE() {
        new Way().cloneFrom(new Node());
    }

    /**
     * Test that {@link Way#load} throws IAE for invalid arguments
     */
    @Test(expected = IllegalArgumentException.class)
    public void testLoadIAE() {
        new Way().load(new NodeData());
    }
}
