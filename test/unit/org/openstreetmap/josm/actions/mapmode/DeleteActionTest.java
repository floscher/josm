// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions.mapmode;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.actions.mapmode.DeleteAction.DeleteMode;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unit tests for class {@link DeleteAction}.
 */
public class DeleteActionTest {

    /**
     * Setup test.
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules().platform().commands();

    /**
     * Unit test of {@link DeleteAction#enterMode} and {@link DeleteAction#exitMode}.
     */
    @Test
    public void testMode() {
        OsmDataLayer layer = new OsmDataLayer(new DataSet(), "", null);
        try {
            Main.getLayerManager().addLayer(layer);
            DeleteAction mapMode = new DeleteAction();
            MapMode oldMapMode = Main.map.mapMode;
            assertTrue(Main.map.selectMapMode(mapMode));
            assertEquals(mapMode, Main.map.mapMode);
            assertTrue(Main.map.selectMapMode(oldMapMode));
        } finally {
            Main.getLayerManager().removeLayer(layer);
        }
    }

    /**
     * Unit test of {@link DeleteMode} enum.
     */
    @Test
    public void testEnumDeleteMode() {
        TestUtils.superficialEnumCodeCoverage(DeleteMode.class);
    }
}
