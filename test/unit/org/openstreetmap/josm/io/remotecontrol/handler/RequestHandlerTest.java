// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io.remotecontrol.handler;

import static org.junit.Assert.assertEquals;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.io.remotecontrol.PermissionPrefWithDefault;
import org.openstreetmap.josm.io.remotecontrol.handler.RequestHandler.RequestHandlerBadRequestException;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unit tests of {@link RequestHandler} class.
 */
public class RequestHandlerTest {

    /**
     * Setup test.
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules();

    Map<String, String> getRequestParameter(String url) throws RequestHandlerBadRequestException {
        final RequestHandler req = new RequestHandler() {
            @Override
            protected void validateRequest() throws RequestHandlerBadRequestException {
            }

            @Override
            protected void handleRequest() throws RequestHandlerErrorException, RequestHandlerBadRequestException {
            }

            @Override
            public String getPermissionMessage() {
                return null;
            }

            @Override
            public PermissionPrefWithDefault getPermissionPref() {
                return null;
            }

            @Override
            public String[] getMandatoryParams() {
                return new String[0];
            }
        };
        req.setUrl(url);
        return req.args;
    }

    /**
     * Test request parameter - case 1
     * @throws RequestHandlerBadRequestException never
     */
    @Test
    public void testRequestParameter1() throws RequestHandlerBadRequestException {
        final Map<String, String> expected = new HashMap<>();
        expected.put("query", "a");
        expected.put("b", "=c");
        assertEquals(expected, getRequestParameter("http://example.com/?query=a&b==c"));
    }

    /**
     * Test request parameter - case 2
     * @throws RequestHandlerBadRequestException never
     */
    @Test
    public void testRequestParameter2() throws RequestHandlerBadRequestException {
        assertEquals(Collections.singletonMap("query", "a&b==c"),
                getRequestParameter("http://example.com/?query=a%26b==c"));
    }

    /**
     * Test request parameter - case 3
     * @throws RequestHandlerBadRequestException never
     */
    @Test
    public void testRequestParameter3() throws RequestHandlerBadRequestException {
        assertEquals(Collections.singleton("blue+light blue"),
                getRequestParameter("http://example.com/blue+light%20blue?blue%2Blight+blue").keySet());
    }

    /**
     * Test request parameter - case 4
     * @see <a href="http://blog.lunatech.com/2009/02/03/what-every-web-developer-must-know-about-url-encoding">
     *      What every web developer must know about URL encoding</a>
     * @throws RequestHandlerBadRequestException never
     */
    @Test
    public void testRequestParameter4() throws RequestHandlerBadRequestException {
        assertEquals(Collections.singletonMap("/?:@-._~!$'()* ,;", "/?:@-._~!$'()* ,;=="), getRequestParameter(
            "http://example.com/:@-._~!$&'()*+,=;:@-._~!$&'()*+,=:@-._~!$&'()*+,==?/?:@-._~!$'()*+,;=/?:@-._~!$'()*+,;==#/?:@-._~!$&'()*+,;="
        ));
    }

    /**
     * Test request parameter - case 5
     * @throws RequestHandlerBadRequestException never
     */
    @Test
    public void testRequestParameter5() throws RequestHandlerBadRequestException {
        final Map<String, String> expected = new HashMap<>();
        expected.put("space", " ");
        expected.put("tab", "\t");
        assertEquals(expected, getRequestParameter("http://example.com/?space=%20&tab=%09"));
    }

    /**
     * Test request parameter - case 6
     * @throws RequestHandlerBadRequestException never
     */
    @Test
    public void testRequestParameter6() throws RequestHandlerBadRequestException {
        final Map<String, String> expected = new HashMap<>();
        expected.put("addtags", "wikipedia:de=Weiße_Gasse|maxspeed=5");
        expected.put("select", "way23071688,way23076176,way23076177,");
        expected.put("left", "13.739727546842");
        expected.put("right", "13.740890970188");
        expected.put("top", "51.049987191025");
        expected.put("bottom", "51.048466954325");
        assertEquals(expected, getRequestParameter("http://localhost:8111/load_and_zoom"+
                "?addtags=wikipedia%3Ade=Wei%C3%9Fe_Gasse%7Cmaxspeed=5"+
                "&select=way23071688,way23076176,way23076177,"+
                "&left=13.739727546842&right=13.740890970188&top=51.049987191025&bottom=51.048466954325"));
    }

    /**
     * Test request parameter - invalid case
     * @throws RequestHandlerBadRequestException always
     */
    @Test(expected = RequestHandlerBadRequestException.class)
    public void testRequestParameterInvalid() throws RequestHandlerBadRequestException {
        getRequestParameter("http://localhost:8111/load_and_zoom"+
                "?addtags=wikipedia:de=Wei%C3%9Fe_Gasse|maxspeed=5"+
                "&select=way23071688,way23076176,way23076177,"+
                "&left=13.739727546842&right=13.740890970188&top=51.049987191025&bottom=51.048466954325");
    }
}
