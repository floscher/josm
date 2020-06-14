// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io.remotecontrol.handler;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.io.remotecontrol.handler.RequestHandler.RequestHandlerBadRequestException;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unit tests of {@link AddWayHandler} class.
 */
public class AddWayHandlerTest {
    /**
     * Setup test.
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules();

    private static AddWayHandler newHandler(String url) throws RequestHandlerBadRequestException {
        AddWayHandler req = new AddWayHandler();
        if (url != null)
            req.setUrl(url);
        return req;
    }

    /**
     * Unit test for bad request - no layer.
     */
    @Test
    public void testBadRequestNoLayer() {
        Exception e = assertThrows(RequestHandlerBadRequestException.class, () -> newHandler("https://localhost?way=0,0;1,1").handle());
        assertEquals("There is no layer opened to add way", e.getMessage());
    }

    /**
     * Unit test for bad request - no param.
     */
    @Test
    public void testBadRequestNoParam() {
        OsmDataLayer layer = new OsmDataLayer(new DataSet(), "", null);
        try {
            MainApplication.getLayerManager().addLayer(layer);
            Exception e = assertThrows(RequestHandlerBadRequestException.class, () -> newHandler(null).handle());
            assertEquals("Invalid coordinates: []", e.getMessage());
        } finally {
            MainApplication.getLayerManager().removeLayer(layer);
        }
    }

    /**
     * Unit test for bad request - invalid URL.
     */
    @Test
    public void testBadRequestInvalidUrl() {
        Exception e = assertThrows(RequestHandlerBadRequestException.class, () -> newHandler("invalid_url").handle());
        assertEquals("The following keys are mandatory, but have not been provided: way", e.getMessage());
    }

    /**
     * Unit test for bad request - incomplete URL.
     */
    @Test
    public void testBadRequestIncompleteUrl() {
        Exception e = assertThrows(RequestHandlerBadRequestException.class, () -> newHandler("https://localhost").handle());
        assertEquals("The following keys are mandatory, but have not been provided: way", e.getMessage());
    }

    /**
     * Unit test for nominal request - local data file.
     */
    @Test
    public void testNominalRequest() {
        OsmDataLayer layer = new OsmDataLayer(new DataSet(), "", null);
        try {
            MainApplication.getLayerManager().addLayer(layer);
            assertDoesNotThrow(() -> newHandler("https://localhost?way=0,0;1,1").handle());
        } finally {
            MainApplication.getLayerManager().removeLayer(layer);
        }
    }
}
