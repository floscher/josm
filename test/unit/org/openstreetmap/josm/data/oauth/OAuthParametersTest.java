// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.oauth;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import nl.jqno.equalsverifier.EqualsVerifier;

import org.junit.Test;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.io.OsmApi;

/**
 * Unit tests for class {@link OAuthParameters}.
 */
public class OAuthParametersTest {

    /**
     * Unit test of method {@link OAuthParameters#createDefault}.
     */
    @Test
    public void testCreateDefault() {
        OAuthParameters def = OAuthParameters.createDefault();
        assertNotNull(def);
        assertEquals(def, OAuthParameters.createDefault(OsmApi.DEFAULT_API_URL));
        OAuthParameters dev = OAuthParameters.createDefault("http://api06.dev.openstreetmap.org/api");
        assertNotNull(dev);
        assertNotEquals(def, dev);
        Main.logLevel = 5; // enable trace for line coverage
        assertEquals(def, OAuthParameters.createDefault("wrong_url"));
        OAuthParameters dev2 = new OAuthParameters(dev);
        assertEquals(dev, dev2);
    }

    /**
     * Unit test of methods {@link OAuthParameters#equals} and {@link OAuthParameters#hashCode}.
     */
    @Test
    public void equalsContract() {
        EqualsVerifier.forClass(OAuthParameters.class).usingGetClass().verify();
    }
}
