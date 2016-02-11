// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.layer.markerlayer;

import static org.junit.Assert.assertEquals;

import java.net.MalformedURLException;
import java.net.URL;

import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.openstreetmap.josm.JOSMFixture;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.gpx.Extensions;
import org.openstreetmap.josm.data.gpx.GpxConstants;
import org.openstreetmap.josm.data.gpx.GpxData;
import org.openstreetmap.josm.data.gpx.WayPoint;

/**
 * Unit tests of {@link AudioMarker} class.
 */
public class AudioMarkerTest {

    /**
     * Setup tests
     */
    @BeforeClass
    public static void setUpBeforeClass() {
        JOSMFixture.createUnitTestFixture().init();
    }

    /**
     * Unit test of {@link AudioMarker#AudioMarker}.
     * @throws MalformedURLException never
     */
    @Test
    @Ignore("looks like it makes AudioPlayerTest.testPlay fail")
    public void testAudioMarker() throws MalformedURLException {
        URL url = new URL("file://something.wav");
        AudioMarker marker = new AudioMarker(
                LatLon.ZERO,
                null,
                url,
                new MarkerLayer(new GpxData(), null, null, null),
                1d, 2d);
        assertEquals(url, marker.url());
        assertEquals("2", marker.getText());
        WayPoint wpt = marker.convertToWayPoint();
        assertEquals(LatLon.ZERO, wpt.getCoor());
        Extensions ext = (Extensions) wpt.get(GpxConstants.META_EXTENSIONS);
        assertEquals("2.0", ext.get("offset"));
    }
}
