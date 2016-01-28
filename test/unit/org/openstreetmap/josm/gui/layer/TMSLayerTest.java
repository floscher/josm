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
 * Unit tests of {@link TMSLayer} class.
 */
public class TMSLayerTest {

    /**
     * Setup tests
     */
    @BeforeClass
    public static void setUpBeforeClass() {
        JOSMFixture.createUnitTestFixture().init(true);
    }

    /**
     * Creates a new TMS layer.
     * @return a new TMS layer
     */
    public static TMSLayer createTmsLayer() {
        return new TMSLayer(new ImageryInfo("test tms", "http://localhost", "tms", null, null));
    }

    /**
     * Creates a new Bing layer.
     * @return a new Bing layer
     */
    public static TMSLayer createBingLayer() {
        return new TMSLayer(new ImageryInfo("test bing", "http://localhost", "bing", null, null));
    }

    /**
     * Creates a new Scanex layer.
     * @return a new Scanex layer
     */
    public static TMSLayer createScanexLayer() {
        return new TMSLayer(new ImageryInfo("test scanex", "http://localhost", "scanex", null, null));
    }

    private static void test(ImageryType expected, TMSLayer layer) {
        Main.main.addLayer(layer);
        try {
            assertEquals(expected, layer.getInfo().getImageryType());
        } finally {
            // Ensure we clean the place before leaving, even if test fails.
            Main.main.removeLayer(layer);
        }
    }

    /**
     * Unit test of {@link TMSLayer#TMSLayer}.
     */
    @Test
    public void testTMSLayer() {
        test(ImageryType.TMS, createTmsLayer());
        test(ImageryType.BING, createBingLayer());
        test(ImageryType.SCANEX, createScanexLayer());
    }
}
