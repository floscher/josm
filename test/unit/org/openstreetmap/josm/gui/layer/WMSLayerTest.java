// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.layer;

import static org.junit.Assert.assertEquals;

import org.junit.BeforeClass;
import org.junit.Test;
import org.openstreetmap.josm.JOSMFixture;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.imagery.ImageryInfo;
import org.openstreetmap.josm.data.imagery.ImageryInfo.ImageryType;

/**
 * Unit tests of {@link WMSLayer} class.
 */
public class WMSLayerTest {

    /**
     * Setup tests
     */
    @BeforeClass
    public static void setUpBeforeClass() {
        JOSMFixture.createUnitTestFixture().init(true);
    }

    /**
     * Unit test of {@link WMSLayer#WMSLayer}.
     */
    @Test
    public void testWMSLayer() {
        WMSLayer wms = new WMSLayer(new ImageryInfo("test wms", "http://localhost"));
        Main.getLayerManager().addLayer(wms);
        try {
            assertEquals(ImageryType.WMS, wms.getInfo().getImageryType());
        } finally {
            // Ensure we clean the place before leaving, even if test fails.
            Main.getLayerManager().removeLayer(wms);
        }
    }
}
