// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm.history;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Date;

import org.junit.Test;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;

public class HistoryWayTest {

    @Test
    public void HistoryWayTest() {
        Date d = new Date();
        HistoryWay way = new HistoryWay(
                1,
                2,
                true,
                "testuser",
                3,
                4,
                d
        );

        assertEquals(1, way.getId());
        assertEquals(2, way.getVersion());
        assertEquals(true, way.isVisible());
        assertEquals("testuser", way.getUser());
        assertEquals(3, way.getUid());
        assertEquals(4, way.getChangesetId());
        assertEquals(d, way.getTimestamp());

        assertEquals(0, way.getNumNodes());
    }

    @Test
    public void getType() {
        Date d = new Date();
        HistoryWay way = new HistoryWay(
                1,
                2,
                true,
                "testuser",
                3,
                4,
                d
        );

        assertEquals(OsmPrimitiveType.WAY, way.getType());
    }

    @Test
    public void nodeManipulation() {
        Date d = new Date();
        HistoryWay way = new HistoryWay(
                1,
                2,
                true,
                "testuser",
                3,
                4,
                d
        );

        way.addNode(1);
        assertEquals(1, way.getNumNodes());
        assertEquals(1, way.getNodeId(0));
        try {
            way.getNodeId(1);
            fail("expected expection of type " + IndexOutOfBoundsException.class.toString());
        } catch(IndexOutOfBoundsException e) {
            // OK
        }

        way.addNode(5);
        assertEquals(2, way.getNumNodes());
        assertEquals(5, way.getNodeId(1));
    }

    @Test
    public void iterating() {
        Date d = new Date();
        HistoryWay way = new HistoryWay(
                1,
                2,
                true,
                "testuser",
                3,
                4,
                d
        );

        way.addNode(1);
        way.addNode(2);
        ArrayList<Long> ids = new ArrayList<Long>();
        for (long id : way.getNodes()) {
            ids.add(id);
        }

        assertEquals(2, ids.size());
        assertEquals(1, ids.get(0));
        assertEquals(2, ids.get(1));
    }

}
