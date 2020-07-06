// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io.remotecontrol.handler;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.data.imagery.ImageryInfo;
import org.openstreetmap.josm.io.remotecontrol.handler.RequestHandler.RequestHandlerBadRequestException;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unit tests of {@link ImageryHandler} class.
 */
public class ImageryHandlerTest {
    /**
     * Setup test.
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules();

    private static ImageryHandler newHandler(String url) throws RequestHandlerBadRequestException {
        ImageryHandler req = new ImageryHandler();
        if (url != null)
            req.setUrl(url);
        return req;
    }

    /**
     * Unit test for bad request - no param.
     */
    @Test
    public void testBadRequestNoParam() {
        Exception e = assertThrows(RequestHandlerBadRequestException.class, () -> newHandler(null).handle());
        assertEquals("Parameter must not be null", e.getMessage());

    }

    /**
     * Unit test for bad request - invalid URL.
     */
    @Test
    public void testBadRequestInvalidUrl() {
        Exception e = assertThrows(RequestHandlerBadRequestException.class, () -> newHandler("invalid_url").handle());
        assertEquals("The following keys are mandatory, but have not been provided: url", e.getMessage());
    }

    /**
     * Unit test for bad request - incomplete URL.
     */
    @Test
    public void testBadRequestIncompleteUrl() {
        Exception e = assertThrows(RequestHandlerBadRequestException.class, () -> newHandler("https://localhost").handle());
        assertEquals("The following keys are mandatory, but have not been provided: url", e.getMessage());
    }

    /**
     * Unit test for nominal request - local data file.
     */
    @Test
    public void testNominalRequest() {
        assertDoesNotThrow(() -> newHandler("https://localhost?url=foo").handle());
    }

    /**
     * Unit test for {@link ImageryHandler#getOptionalParams()}
     * @throws Exception if any error occurs
     */
    @Test
    public void testOptionalParams() throws Exception {
        List<String> optionalParams = Arrays.asList(newHandler("").getOptionalParams());
        assertThat(optionalParams, hasItem("type"));
        assertThat(optionalParams, hasItem("min-zoom"));
        assertThat(optionalParams, hasItem("max-zoom"));
        assertThat(optionalParams, hasItem("category"));
    }

    /**
     * Unit test for {@link ImageryHandler#buildImageryInfo()}
     * @throws Exception if any error occurs
     */
    @Test
    public void testBuildImageryInfo() throws Exception {
        String url = "https://localhost/imagery?title=osm"
                + "&type=tms&min_zoom=3&max_zoom=23&category=osmbasedmap&country_code=XA"
                + "&url=https://a.tile.openstreetmap.org/%7Bzoom%7D/%7Bx%7D/%7By%7D.png";
        ImageryInfo imageryInfo = newHandler(url).buildImageryInfo();
        assertEquals("osm", imageryInfo.getName());
        assertEquals(ImageryInfo.ImageryType.TMS, imageryInfo.getImageryType());
        assertEquals("https://a.tile.openstreetmap.org/{zoom}/{x}/{y}.png", imageryInfo.getUrl());
        assertEquals(3, imageryInfo.getMinZoom());
        assertEquals(23, imageryInfo.getMaxZoom());
        assertEquals(ImageryInfo.ImageryCategory.OSMBASEDMAP, imageryInfo.getImageryCategory());
        assertEquals("XA", imageryInfo.getCountryCode());
    }

    /**
     * Non-regression test for bug #19483.
     * @throws Exception if any error occurs
     */
    @Test
    public void testTicket19483() throws Exception {
        String url = "https://localhost/imagery?url=" +
                "tms[3-7]%3Ahttps%3A%2F%2Fservices.digitalglobe.com%2Fearthservice%2Ftmsaccess%2F" +
                "tms%2F1.0.0%2FDigitalGlobe%3AImageryTileService%40EPSG%3A3857%40jpg%2F%7Bz%7D%2F%7Bx%7D%2F%7B-y%7D.jpg%3F" +
                "connectId%3D0123456789";
        ImageryInfo imageryInfo = newHandler(url).buildImageryInfo();
        assertEquals(ImageryInfo.ImageryType.WMS, imageryInfo.getImageryType());
        /* do not interpret the URL, take it as is and error later */
        assertEquals("tms[3,7]:https://services.digitalglobe.com/earthservice/tmsaccess/tms/1.0.0/DigitalGlobe:ImageryTileService" +
                "@EPSG:3857@jpg/{z}/{x}/{-y}.jpg?connectId=0123456789", imageryInfo.getUrl());
    }
}
