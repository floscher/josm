// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions.mapmode;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.BeforeClass;
import org.junit.Test;
import org.openstreetmap.josm.JOSMFixture;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;

/**
 * Unit tests for class {@link ParallelWayAction}.
 */
public class ParallelWayActionTest {

    /**
     * Setup test.
     */
    @BeforeClass
    public static void setUp() {
        JOSMFixture.createUnitTestFixture().init(true);
    }

    /**
     * Unit test of {@link ParallelWayAction#enterMode} and {@link ParallelWayAction#exitMode}.
     */
    @Test
    public void testMode() {
        OsmDataLayer layer = new OsmDataLayer(new DataSet(), "", null);
        try {
            Main.getLayerManager().addLayer(layer);
            ParallelWayAction mapMode = new ParallelWayAction(Main.map);
            MapMode oldMapMode = Main.map.mapMode;
            assertTrue(Main.map.selectMapMode(mapMode));
            assertEquals(mapMode, Main.map.mapMode);
            assertTrue(Main.map.selectMapMode(oldMapMode));
        } finally {
            Main.getLayerManager().removeLayer(layer);
        }
    }
}
