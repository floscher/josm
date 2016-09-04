// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;

import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.actions.search.SearchAction;
import org.openstreetmap.josm.actions.search.SearchCompiler;
import org.openstreetmap.josm.data.osm.Tag;
import org.openstreetmap.josm.data.preferences.CollectionProperty;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unit tests of {@link RecentTagCollection} class.
 */
public class RecentTagCollectionTest {

    /**
     * Setup tests
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules().preferences();

    /**
     * Performs various tests on a {@link RecentTagCollection}.
     *
     * @throws SearchCompiler.ParseError if an error has been encountered while compiling
     */
    @Test
    public void testVarious() throws SearchCompiler.ParseError {
        final RecentTagCollection recentTags = new RecentTagCollection(2);
        assertTrue(recentTags.isEmpty());

        final Tag foo = new Tag("name", "foo");
        final Tag bar = new Tag("name", "bar");
        final Tag baz = new Tag("name", "baz");
        recentTags.add(foo);
        recentTags.add(bar);
        assertFalse(recentTags.isEmpty());
        assertEquals(Arrays.asList(foo, bar), recentTags.toList());
        recentTags.add(foo);
        assertEquals(Arrays.asList(bar, foo), recentTags.toList());
        recentTags.add(baz);
        assertEquals(Arrays.asList(foo, baz), recentTags.toList());

        final CollectionProperty pref = new CollectionProperty("properties.recent-tags", Collections.<String>emptyList());
        recentTags.saveToPreference(pref);
        assertEquals(Arrays.asList("name", "foo", "name", "baz"), pref.get());
        pref.put(Arrays.asList("key=", "=value"));
        recentTags.loadFromPreference(pref);
        assertEquals(Collections.singletonList(new Tag("key=", "=value")), recentTags.toList());

        recentTags.add(foo);
        recentTags.add(bar);
        recentTags.add(baz);
        final SearchAction.SearchSetting searchSetting = new SearchAction.SearchSetting();
        recentTags.ignoreTag(baz, searchSetting);
        recentTags.ignoreTag(new Tag("something", "else"), searchSetting);
        assertEquals("\"name\"=\"baz\" OR \"something\"=\"else\"", searchSetting.text);
        assertEquals(Collections.singletonList(bar), recentTags.toList());
        recentTags.add(baz);
        assertEquals(Collections.singletonList(bar), recentTags.toList());
        searchSetting.text = "";
        recentTags.setTagsToIgnore(searchSetting);
        assertEquals(Collections.singletonList(bar), recentTags.toList());
        recentTags.add(baz);
        assertEquals(Arrays.asList(bar, baz), recentTags.toList());
        recentTags.ignoreTag(new Tag("name", /*all values */""), searchSetting);
        assertEquals(Collections.emptyList(), recentTags.toList());
    }
}
