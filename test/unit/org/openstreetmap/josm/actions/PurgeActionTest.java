// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import org.junit.BeforeClass;
import org.junit.Test;
import org.openstreetmap.josm.JOSMFixture;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.io.IllegalDataException;
import org.openstreetmap.josm.io.OsmReader;

/**
 * Unit tests for class {@link PurgeAction}.
 */
public class PurgeActionTest {

    /**
     * Setup test.
     */
    @BeforeClass
    public static void setUpBeforeClass() {
        JOSMFixture.createUnitTestFixture().init(true);
    }

    /**
     * Non-regression test for ticket #12038.
     * @throws IOException if any I/O error occurs
     * @throws FileNotFoundException if the data file cannot be found
     * @throws IllegalDataException if OSM parsing fails
     */
    @Test
    public void testCopyStringWayRelation() throws FileNotFoundException, IOException, IllegalDataException {
        try (InputStream is = TestUtils.getRegressionDataStream(12038, "data.osm")) {
            DataSet ds = OsmReader.parseDataSet(is, null);
            Main.map.mapView.addLayer(new OsmDataLayer(ds, null, null));
            for (Way w : ds.getWays()) {
                if (w.getId() == 222191929L) {
                    ds.addSelected(w);
                }
            }
            new PurgeAction().actionPerformed(null);
            for (Way w : ds.getWays()) {
                if (w.getId() == 222191929L) {
                    assertTrue(w.isIncomplete());
                    assertEquals(0, w.getNodesCount());
                }
            }
        }
    }
}
