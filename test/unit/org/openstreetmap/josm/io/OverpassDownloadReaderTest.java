// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.regex.Matcher;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.io.OverpassDownloadReader.OverpassOutpoutFormat;
import org.openstreetmap.josm.testutils.JOSMTestRules;
import org.openstreetmap.josm.tools.OverpassTurboQueryWizard;
import org.openstreetmap.josm.tools.Utils;

import com.github.tomakehurst.wiremock.junit.WireMockRule;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unit tests of {@link OverpassDownloadReader} class.
 */
public class OverpassDownloadReaderTest {

    /**
     * Base test environment is enough
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules().preferences();

    /**
     * HTTP mock.
     */
    @Rule
    public WireMockRule wireMockRule = new WireMockRule(options().dynamicPort().usingFilesUnderDirectory(TestUtils.getTestDataRoot()));

    private static final String NOMINATIM_URL_PATH = "/search?format=xml&q=";

    /**
     * Setup test.
     */
    @Before
    public void setUp() {
        NameFinder.NOMINATIM_URL_PROP.put("http://localhost:" + wireMockRule.port() + NOMINATIM_URL_PATH);
    }

    private String getExpandedQuery(String search) {
        final String query = OverpassTurboQueryWizard.getInstance().constructQuery(search);
        final String request = new OverpassDownloadReader(new Bounds(1, 2, 3, 4), null, query)
                .getRequestForBbox(1, 2, 3, 4)
                .substring("interpreter?data=".length());
        return Utils.decodeUrl(request);
    }

    /**
     * Tests evaluating the extended query feature {@code bbox}.
     */
    @Test
    public void testBbox() {
        final String query = getExpandedQuery("amenity=drinking_water");
        assertEquals("" +
                "[out:xml][timeout:25][bbox:2.0,1.0,4.0,3.0];\n" +
                "(\n" +
                "  node[\"amenity\"=\"drinking_water\"];\n" +
                "  way[\"amenity\"=\"drinking_water\"];\n" +
                "  relation[\"amenity\"=\"drinking_water\"];\n" +
                ");\n" +
                "(._;>;);\n" +
                "out meta;", query);
    }

    private void stubNominatim(String query) {
        wireMockRule.stubFor(get(urlEqualTo(NOMINATIM_URL_PATH + query))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "text/xml")
                    .withBodyFile("nominatim/" + query + ".xml")));
    }

    /**
     * Tests evaluating the extended query feature {@code geocodeArea}.
     */
    @Test
    public void testGeocodeArea() {
        stubNominatim("London");
        final String query = getExpandedQuery("amenity=drinking_water in London");
        assertEquals("" +
                "[out:xml][timeout:25];\n" +
                "area(3600065606)->.searchArea;\n" +
                "(\n" +
                "  node[\"amenity\"=\"drinking_water\"](area.searchArea);\n" +
                "  way[\"amenity\"=\"drinking_water\"](area.searchArea);\n" +
                "  relation[\"amenity\"=\"drinking_water\"](area.searchArea);\n" +
                ");\n" +
                "(._;>;);\n" +
                "out meta;", query);
    }

    /**
     * Tests evaluating the extended query feature {@code geocodeArea}.
     */
    @Test
    public void testGeocodeUnknownArea() {
        stubNominatim("foo-bar-baz-does-not-exist");
        final String query = OverpassDownloadReader.expandExtendedQueries("{{geocodeArea:foo-bar-baz-does-not-exist}}");
        assertEquals("// Failed to evaluate {{geocodeArea:foo-bar-baz-does-not-exist}}\n", query);
    }

    /**
     * Tests evaluating the overpass output format statements.
     */
    @Test
    public void testOutputFormatStatement() {
        for (OverpassOutpoutFormat oof : OverpassOutpoutFormat.values()) {
            Matcher m = OverpassDownloadReader.OUTPUT_FORMAT_STATEMENT.matcher("[out:"+oof.getDirective()+"]");
            assertTrue(m.matches());
            assertEquals(oof.getDirective(), m.group(1));
        }

        assertTrue(OverpassDownloadReader.OUTPUT_FORMAT_STATEMENT.matcher(
                "[out:pbf][timeout:25][bbox:{{bbox}}];\n" +
                "(\n" +
                "  node[\"amenity\"=\"pharmacy\"];\n" +
                "  way[\"amenity\"=\"pharmacy\"];\n" +
                "  relation[\"amenity\"=\"pharmacy\"];\n" +
                ");\n" +
                "(._;>;);\n" +
                "out meta;").matches());
    }
}
