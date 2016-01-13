// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.layer;

import static org.junit.Assert.assertEquals;

import org.junit.BeforeClass;
import org.junit.Test;
import org.openstreetmap.josm.JOSMFixture;
import org.openstreetmap.josm.data.imagery.ImageryInfo;
import org.openstreetmap.josm.data.imagery.ImageryInfo.ImageryType;

/**
 * Unit tests of {@link WMTSLayer} class.
 */
public class WMTSLayerTest {

    /**
     * Setup tests
     */
    @BeforeClass
    public static void setUpBeforeClass() {
        JOSMFixture.createUnitTestFixture().init();
    }

    /**
     * Unit test of {@link WMTSLayer#WMTSLayer}.
     */
    @Test
    public void testTMSLayer() {
        WMTSLayer wmts = new WMTSLayer(new ImageryInfo("test wmts", "http://localhost", "wmts", null, null));
        assertEquals(ImageryType.WMTS, wmts.getInfo().getImageryType());
    }
}
