// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.openstreetmap.josm.TestUtils;

/**
 * Unit test of {@link ChangesetCache}
 */
public class ChangesetCacheTest {

    /**
     * Unit test of {@link ChangesetCache#ChangesetCache}
     */
    @Test
    public void testConstructor() {
        assertNotNull(ChangesetCache.getInstance());
    }

    @SuppressWarnings("unchecked")
    private static List<ChangesetCacheListener> getListeners(ChangesetCache cache) throws ReflectiveOperationException {
        return (List<ChangesetCacheListener>) TestUtils.getPrivateField(cache, "listeners");
    }

    @Test
    public void testAddAndRemoveListeners() throws ReflectiveOperationException {
        ChangesetCache cache = ChangesetCache.getInstance();
        cache.clear();

        // should work
        cache.addChangesetCacheListener(null);

        // should work
        ChangesetCacheListener listener = new ChangesetCacheListener() {
            @Override
            public void changesetCacheUpdated(ChangesetCacheEvent event) {}
        };
        cache.addChangesetCacheListener(listener);
        // adding a second time - should work too
        cache.addChangesetCacheListener(listener);
        assertEquals(1, getListeners(cache).size()); // ... but only added once

        cache.removeChangesetCacheListener(null);

        cache.removeChangesetCacheListener(listener);
        assertTrue(getListeners(cache).isEmpty());
    }

    @Test
    public void testUpdateGetRemoveCycle() {
        ChangesetCache cache = ChangesetCache.getInstance();
        cache.clear();

        cache.update(new Changeset(1));
        assertEquals(1, cache.size());
        assertNotNull(cache.get(1));
        assertEquals(1, cache.get(1).getId());
        cache.remove(1);
        assertEquals(0, cache.size());
    }

    @Test
    public void testUpdateTwice() {
        ChangesetCache cache = ChangesetCache.getInstance();
        cache.clear();

        Changeset cs = new Changeset(1);
        cs.setIncomplete(false);
        cs.put("key1", "value1");
        cs.setOpen(true);
        cache.update(cs);

        Changeset cs2 = new Changeset(cs);
        assertNotNull(cs2);
        cs2.put("key2", "value2");
        cs2.setOpen(false);
        cache.update(cs2);

        assertEquals(1, cache.size());
        assertNotNull(cache.get(1));

        cs = cache.get(1);
        assertEquals("value1", cs.get("key1"));
        assertEquals("value2", cs.get("key2"));
        assertFalse(cs.isOpen());
    }

    @Test
    public void testContains() throws ReflectiveOperationException {
        ChangesetCache cache = ChangesetCache.getInstance();
        getListeners(cache).clear();
        cache.clear();

        Changeset cs = new Changeset(1);
        cache.update(cs);

        assertTrue(cache.contains(1));
        assertTrue(cache.contains(cs));
        assertTrue(cache.contains(new Changeset(cs)));

        assertFalse(cache.contains(2));
        assertFalse(cache.contains(new Changeset(2)));
        assertFalse(cache.contains(null));
    }

    @Test
    public void testFireingEventsAddAChangeset() throws ReflectiveOperationException {
        ChangesetCache cache = ChangesetCache.getInstance();
        cache.clear();
        getListeners(cache).clear();

        // should work
        ChangesetCacheListener listener = new ChangesetCacheListener() {
            @Override
            public void changesetCacheUpdated(ChangesetCacheEvent event) {
                assertNotNull(event);
                assertEquals(1, event.getAddedChangesets().size());
                assertTrue(event.getRemovedChangesets().isEmpty());
                assertTrue(event.getUpdatedChangesets().isEmpty());
                assertEquals(cache, event.getSource());
            }
        };
        cache.addChangesetCacheListener(listener);
        cache.update(new Changeset(1));
        cache.removeChangesetCacheListener(listener);
    }

    @Test
    public void testFireingEventsUpdateChangeset() throws ReflectiveOperationException {
        ChangesetCache cache = ChangesetCache.getInstance();
        cache.clear();
        getListeners(cache).clear();

        // should work
        ChangesetCacheListener listener = new ChangesetCacheListener() {
            @Override
            public void changesetCacheUpdated(ChangesetCacheEvent event) {
                assertNotNull(event);
                assertTrue(event.getAddedChangesets().isEmpty());
                assertTrue(event.getRemovedChangesets().isEmpty());
                assertEquals(1, event.getUpdatedChangesets().size());
                assertEquals(cache, event.getSource());
            }
        };
        cache.update(new Changeset(1));

        cache.addChangesetCacheListener(listener);
        cache.update(new Changeset(1));
        cache.removeChangesetCacheListener(listener);
    }

    @Test
    public void testFireingEventsRemoveChangeset() throws ReflectiveOperationException {
        ChangesetCache cache = ChangesetCache.getInstance();
        cache.clear();
        getListeners(cache).clear();

        // should work
        ChangesetCacheListener listener = new ChangesetCacheListener() {
            @Override
            public void changesetCacheUpdated(ChangesetCacheEvent event) {
                assertNotNull(event);
                assertTrue(event.getAddedChangesets().isEmpty());
                assertEquals(1, event.getRemovedChangesets().size());
                assertTrue(event.getUpdatedChangesets().isEmpty());
                assertEquals(cache, event.getSource());
            }
        };
        cache.update(new Changeset(1));

        cache.addChangesetCacheListener(listener);
        cache.remove(1);
        cache.removeChangesetCacheListener(listener);
    }

    /**
     * Unit test of method {@link ChangesetCache#getOpenChangesets}.
     */
    @Test
    public void testGetOpenChangesets() {
        final ChangesetCache cache = ChangesetCache.getInstance();
        // empty cache => empty list
        Assert.assertTrue(
                "Empty cache should produce an empty list.",
                cache.getOpenChangesets().isEmpty()
        );

        // cache with only closed changesets => empty list
        Changeset closedCs = new Changeset(1);
        closedCs.setOpen(false);
        cache.update(closedCs);
        Assert.assertTrue(
                "Cache with only closed changesets should produce an empty list.",
                cache.getOpenChangesets().isEmpty()
        );

        // cache with open and closed changesets => list with only the open ones
        Changeset openCs = new Changeset(2);
        openCs.setOpen(true);
        cache.update(openCs);
        Assert.assertEquals(
                Collections.singletonList(openCs),
                cache.getOpenChangesets()
        );
    }
}
