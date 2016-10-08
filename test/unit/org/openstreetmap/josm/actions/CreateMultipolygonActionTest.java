// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import static org.junit.Assert.assertEquals;

import java.io.FileInputStream;
import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;

import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.actions.search.SearchAction.SearchSetting;
import org.openstreetmap.josm.actions.search.SearchCompiler;
import org.openstreetmap.josm.actions.search.SearchCompiler.ParseError;
import org.openstreetmap.josm.command.SequenceCommand;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.io.OsmReader;
import org.openstreetmap.josm.testutils.JOSMTestRules;
import org.openstreetmap.josm.tools.Pair;
import org.openstreetmap.josm.tools.SubclassFilteredCollection;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unit test of {@link CreateMultipolygonAction}
 */
public class CreateMultipolygonActionTest {

    /**
     * Setup test.
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules().projection();

    private static Map<String, String> getRefToRoleMap(Relation relation) {
        Map<String, String> refToRole = new TreeMap<>();
        for (RelationMember i : relation.getMembers()) {
            refToRole.put(i.getMember().get("ref"), i.getRole());
        }
        return refToRole;
    }

    private static SearchSetting regexpSearch(String search) {
        SearchSetting setting = new SearchSetting();
        setting.text = search;
        setting.regexSearch = true;
        return setting;
    }

    @SuppressWarnings("unchecked")
    private static Pair<SequenceCommand, Relation> createMultipolygonCommand(Collection<Way> ways, String pattern, Relation r)
            throws ParseError {
        return CreateMultipolygonAction.createMultipolygonCommand(
            (Collection<Way>) (Collection<?>) SubclassFilteredCollection.filter(ways, SearchCompiler.compile(regexpSearch(pattern))), r);
    }

    @Test
    public void testCreate1() throws Exception {
        DataSet ds = OsmReader.parseDataSet(new FileInputStream(TestUtils.getTestDataRoot() + "create_multipolygon.osm"), null);
        Pair<SequenceCommand, Relation> mp = CreateMultipolygonAction.createMultipolygonCommand(ds.getWays(), null);
        assertEquals("Sequence: Create multipolygon", mp.a.getDescriptionText());
        assertEquals("{1=outer, 1.1=inner, 1.1.1=outer, 1.1.2=outer, 1.2=inner}", getRefToRoleMap(mp.b).toString());
    }

    @Test
    public void testCreate2() throws Exception {
        DataSet ds = OsmReader.parseDataSet(new FileInputStream(TestUtils.getTestDataRoot() + "create_multipolygon.osm"), null);
        Pair<SequenceCommand, Relation> mp = createMultipolygonCommand(ds.getWays(), "ref=1 OR ref:1.1.", null);
        assertEquals("{1=outer, 1.1.1=inner, 1.1.2=inner}", getRefToRoleMap(mp.b).toString());
    }

    @Test
    public void testUpdate1() throws Exception {
        DataSet ds = OsmReader.parseDataSet(new FileInputStream(TestUtils.getTestDataRoot() + "create_multipolygon.osm"), null);
        Pair<SequenceCommand, Relation> mp = createMultipolygonCommand(ds.getWays(), "ref=\".*1$\"", null);
        assertEquals(3, mp.b.getMembersCount());
        assertEquals("{1=outer, 1.1=inner, 1.1.1=outer}", getRefToRoleMap(mp.b).toString());
        Pair<SequenceCommand, Relation> mp2 = createMultipolygonCommand(ds.getWays(), "ref=1.2", mp.b);
        assertEquals(4, mp2.b.getMembersCount());
        assertEquals("{1=outer, 1.1=inner, 1.1.1=outer, 1.2=inner}", getRefToRoleMap(mp2.b).toString());
    }

    @Test
    public void testUpdate2() throws Exception {
        DataSet ds = OsmReader.parseDataSet(new FileInputStream(TestUtils.getTestDataRoot() + "create_multipolygon.osm"), null);
        Pair<SequenceCommand, Relation> mp = createMultipolygonCommand(ds.getWays(), "ref=1 OR ref:1.1.1", null);
        assertEquals("{1=outer, 1.1.1=inner}", getRefToRoleMap(mp.b).toString());
        Pair<SequenceCommand, Relation> mp2 = createMultipolygonCommand(ds.getWays(), "ref=1.1 OR ref=1.2 OR ref=1.1.2", mp.b);
        assertEquals("{1=outer, 1.1=inner, 1.1.1=outer, 1.1.2=outer, 1.2=inner}", getRefToRoleMap(mp2.b).toString());
    }
}
