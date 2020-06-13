// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unit tests of {@link MultiFetchOverpassObjectReader}.
 */
public class MultiFetchOverpassObjectReaderTest {

    /**
     * Setup test.
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules().preferences();

    /**
     * Test {@link MultiFetchOverpassObjectReader#buildRequestString}
     */
    @Test
    public void testBuildRequestNodesString() {
        List<OsmPrimitive> objects = Arrays.asList(new Node(123), new Node(126), new Node(130));
        String requestString;
        // nodes without parents
        requestString = MultiFetchOverpassObjectReader.genOverpassQuery(objects, true, false, false);
        assertEquals("node(id:123,126,130)->.n;(.n;);out meta;", requestString);
        requestString = MultiFetchOverpassObjectReader.genOverpassQuery(objects, true, false, true);
        assertEquals("node(id:123,126,130)->.n;(.n;);out meta;", requestString);

        // nodes with parents
        requestString = MultiFetchOverpassObjectReader.genOverpassQuery(objects, true, true, false);
        assertEquals("node(id:123,126,130)->.n;.n;way(bn)->.wn;.n;rel(bn)->.rn;(.n;.wn;node(w);.rn;);out meta;",
                requestString);
        requestString = MultiFetchOverpassObjectReader.genOverpassQuery(objects, true, true, true);
        assertEquals("node(id:123,126,130)->.n;.n;way(bn)->.wn;.n;rel(bn)->.rn;(.n;.wn;node(w);.rn;);out meta;",
                requestString);

        // simulate download referrers
        requestString = MultiFetchOverpassObjectReader.genOverpassQuery(objects, false, true, false);
        assertEquals("node(id:123,126,130)->.n;.n;way(bn)->.wn;.n;rel(bn)->.rn;(.wn;node(w);.rn;);out meta;",
                requestString);

    }

    /**
     * Test {@link MultiFetchOverpassObjectReader#buildRequestString}
     */
    @Test
    public void testBuildRequestWaysString() {
        List<OsmPrimitive> objects = Arrays.asList(new Way(123), new Way(126), new Way(130));
        String requestString;
        // ways without parents (always with nodes)
        requestString = MultiFetchOverpassObjectReader.genOverpassQuery(objects, true, false, false);
        assertEquals("way(id:123,126,130)->.w;(.w;>;);out meta;", requestString);
        requestString = MultiFetchOverpassObjectReader.genOverpassQuery(objects, true, false, true);
        assertEquals("way(id:123,126,130)->.w;(.w;>;);out meta;", requestString);

        // ways with parents (always with nodes)
        requestString = MultiFetchOverpassObjectReader.genOverpassQuery(objects, true, true, false);
        assertEquals("way(id:123,126,130)->.w;.w;rel(bw)->.pw;(.w;>;.pw;);out meta;", requestString);
        requestString = MultiFetchOverpassObjectReader.genOverpassQuery(objects, true, true, true);
        assertEquals("way(id:123,126,130)->.w;.w;rel(bw)->.pw;(.w;>;.pw;);out meta;", requestString);

        // simulate download referrers
        requestString = MultiFetchOverpassObjectReader.genOverpassQuery(objects, false, true, false);
        assertEquals("way(id:123,126,130)->.w;.w;rel(bw)->.pw;(.pw;);out meta;", requestString);

    }

    /**
     * Test {@link MultiFetchOverpassObjectReader#buildRequestString}
     */
    @Test
    public void testBuildRequestRelationsString() {
        List<OsmPrimitive> objects = Arrays.asList(new Relation(123), new Relation(126), new Relation(130));
        String requestString;
        // objects without parents or children
        requestString = MultiFetchOverpassObjectReader.genOverpassQuery(objects, true, false, false);
        assertEquals("relation(id:123,126,130)->.r;(.r;);out meta;", requestString);
        // objects without parents, with children
        requestString = MultiFetchOverpassObjectReader.genOverpassQuery(objects, true, false, true);
        assertEquals("relation(id:123,126,130)->.r;.r;rel(r)->.rm;(.r;.r;>;.rm;);out meta;", requestString);
        // objects with parents, without children
        requestString = MultiFetchOverpassObjectReader.genOverpassQuery(objects, true, true, false);
        assertEquals("relation(id:123,126,130)->.r;.r;rel(br)->.pr;(.r;.pr;);out meta;", requestString);
        // objects with parents and with children
        requestString = MultiFetchOverpassObjectReader.genOverpassQuery(objects, true, true, true);
        assertEquals("relation(id:123,126,130)->.r;.r;rel(br)->.pr;.r;rel(r)->.rm;(.r;.pr;.r;>;.rm;);out meta;",
                requestString);
        // simulate download referrers
        requestString = MultiFetchOverpassObjectReader.genOverpassQuery(objects, false, true, false);
        assertEquals("relation(id:123,126,130)->.r;.r;rel(br)->.pr;(.pr;);out meta;", requestString);

    }

    /**
     * Test {@link MultiFetchOverpassObjectReader#buildRequestString}
     */
    @Test
    public void testBuildComplexString() {
        List<OsmPrimitive> objects = Arrays.asList(new Relation(123), new Relation(126), new Relation(130), new Way(88), new Way(99),
                new Node(1));
        // all request strings should start with the same list of objects
        final String ids = "relation(id:123,126,130)->.r;way(id:88,99)->.w;node(1)->.n;";
        String requestString;

        // objects without parents (ways always with nodes)
        requestString = MultiFetchOverpassObjectReader.genOverpassQuery(objects, true, false, false);
        assertEquals(ids + "(.r;.w;>;.n;);out meta;", requestString);
        // objects without parents (ways always with nodes), recurse down one level for sub relations
        requestString = MultiFetchOverpassObjectReader.genOverpassQuery(objects, true, false, true);
        assertEquals(ids + ".r;rel(r)->.rm;(.r;.r;>;.rm;.w;>;.n;);out meta;", requestString);

        // objects with parents
        requestString = MultiFetchOverpassObjectReader.genOverpassQuery(objects, true, true, false);
        assertEquals(
                ids + ".r;rel(br)->.pr;.w;rel(bw)->.pw;.n;way(bn)->.wn;.n;rel(bn)->.rn;(.r;.pr;.w;>;.pw;.n;.wn;node(w);.rn;);out meta;",
                requestString);

        // objects with parents, recurse down one level for sub relations
        requestString = MultiFetchOverpassObjectReader.genOverpassQuery(objects, true, true, true);
        assertEquals(ids + ".r;rel(br)->.pr;.w;rel(bw)->.pw;.n;way(bn)->.wn;.n;rel(bn)->.rn;.r;rel(r)->.rm;"
                + "(.r;.pr;.r;>;.rm;.w;>;.pw;.n;.wn;node(w);.rn;);out meta;", requestString);
        // simulate download referrers
        requestString = MultiFetchOverpassObjectReader.genOverpassQuery(objects, false, true, false);
        assertEquals(
                ids + ".r;rel(br)->.pr;.w;rel(bw)->.pw;.n;way(bn)->.wn;.n;rel(bn)->.rn;(.pr;.pw;.wn;node(w);.rn;);out meta;",
                requestString);
    }
}
