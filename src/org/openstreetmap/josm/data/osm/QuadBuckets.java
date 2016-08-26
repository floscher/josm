// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.coor.QuadTiling;

/**
 * Note: bbox of primitives added to QuadBuckets has to stay the same. In case of coordinate change, primitive must
 * be removed and readded.
 *
 * This class is (no longer) thread safe.
 * @param <T> type of primitives
 * @since 2165
 */
public class QuadBuckets<T extends OsmPrimitive> implements Collection<T> {
    private static final boolean consistency_testing = false;
    private static final int NW_INDEX = 1;
    private static final int NE_INDEX = 3;
    private static final int SE_INDEX = 2;
    private static final int SW_INDEX = 0;

    static void abort(String s) {
        throw new AssertionError(s);
    }

    public static final int MAX_OBJECTS_PER_LEVEL = 16;

    static class QBLevel<T extends OsmPrimitive> {
        private final int level;
        private final int index;
        private final BBox bbox;
        private final long quad;
        private final QBLevel<T> parent;
        private boolean isLeaf = true;

        private List<T> content;
        // child order by index is sw, nw, se, ne
        private QBLevel<T> nw, ne, sw, se;

        private final QuadBuckets<T> buckets;

        private QBLevel<T> getChild(int index) {
            switch (index) {
            case NE_INDEX:
                if (ne == null) {
                    ne = new QBLevel<>(this, index, buckets);
                }
                return ne;
            case NW_INDEX:
                if (nw == null) {
                    nw = new QBLevel<>(this, index, buckets);
                }
                return nw;
            case SE_INDEX:
                if (se == null) {
                    se = new QBLevel<>(this, index, buckets);
                }
                return se;
            case SW_INDEX:
                if (sw == null) {
                    sw = new QBLevel<>(this, index, buckets);
                }
                return sw;
            default:
                return null;
            }
        }

        @SuppressWarnings("unchecked")
        private QBLevel<T>[] getChildren() {
            return new QBLevel[] {sw, nw, se, ne};
        }

        @Override
        public String toString() {
            return super.toString() + '[' + level + "]: " + bbox();
        }

        /**
         * Constructor for root node
         * @param buckets quadbuckets
         */
        QBLevel(final QuadBuckets<T> buckets) {
            level = 0;
            index = 0;
            quad = 0;
            parent = null;
            bbox = new BBox(-180, 90, 180, -90);
            this.buckets = buckets;
        }

        QBLevel(QBLevel<T> parent, int parentIndex, final QuadBuckets<T> buckets) {
            this.parent = parent;
            this.level = parent.level + 1;
            this.index = parentIndex;
            this.buckets = buckets;

            int shift = (QuadTiling.NR_LEVELS - level) * 2;
            long mult = 1;
            // Java blows the big one. It seems to wrap when you shift by > 31
            if (shift >= 30) {
                shift -= 30;
                mult = 1 << 30;
            }
            long quadpart = mult * (parentIndex << shift);
            this.quad = parent.quad | quadpart;
            this.bbox = calculateBBox(); // calculateBBox reference quad
        }

        private BBox calculateBBox() {
            LatLon bottomLeft = this.coor();
            double lat = bottomLeft.lat() + parent.height() / 2;
            double lon = bottomLeft.lon() + parent.width() / 2;
            return new BBox(bottomLeft.lon(), bottomLeft.lat(), lon, lat);
        }

        QBLevel<T> findBucket(BBox bbox) {
            if (!hasChildren())
                return this;
            else {
                int idx = bbox.getIndex(level);
                if (idx == -1)
                    return this;
                return getChild(idx).findBucket(bbox);
            }
        }

        boolean removeContent(T o) {
            // If two threads try to remove item at the same time from different buckets of this QBLevel,
            // it might happen that one thread removes bucket but don't remove parent because it still sees
            // another bucket set. Second thread do the same. Due to thread memory caching, it's possible that
            // changes made by threads will show up in children array too late, leading to QBLevel with all children
            // set to null
            if (content == null)
                return false;
            boolean ret = this.content.remove(o);
            if (this.content.isEmpty()) {
                this.content = null;
            }
            if (this.canRemove()) {
                this.removeFromParent();
            }
            return ret;
        }

        /*
         * There is a race between this and qb.nextContentNode().
         * If nextContentNode() runs into this bucket, it may
         * attempt to null out 'children' because it thinks this
         * is a dead end.
         */
        void doSplit() {
            List<T> tmpcontent = content;
            content = null;

            for (T o : tmpcontent) {
                int idx = o.getBBox().getIndex(level);
                if (idx == -1) {
                    doAddContent(o);
                } else {
                    getChild(idx).doAdd(o);
                }
            }
            isLeaf = false; // It's not enough to check children because all items could end up in this level (index == -1)
        }

        boolean doAddContent(T o) {
            // The split_lock will keep two concurrent calls from overwriting content
            if (content == null) {
                content = new ArrayList<>();
            }
            return content.add(o);
        }

        boolean matches(final T o, final BBox searchBbox) {
            if (o instanceof Node) {
                final LatLon latLon = ((Node) o).getCoor();
                // node without coords -> bbox[0,0,0,0]
                return searchBbox.bounds(latLon != null ? latLon : LatLon.ZERO);
            }
            return o.getBBox().intersects(searchBbox);
        }

        private void searchContents(BBox searchBbox, List<T> result) {
            /*
             * It is possible that this was created in a split
             * but never got any content populated.
             */
            if (content == null)
                return;

            for (T o : content) {
                if (matches(o, searchBbox)) {
                    result.add(o);
                }
            }
        }

        /*
         * This is stupid. I tried to have a QBLeaf and QBBranch
         * class descending from a QBLevel. It's more than twice
         * as slow. So, this throws OO out the window, but it
         * is fast. Runtime type determination must be slow.
         */
        boolean isLeaf() {
            return isLeaf;
        }

        boolean hasChildren() {
            return nw != null || ne != null || sw != null || se != null;
        }

        QBLevel<T> findNextSibling() {
            return (parent == null) ? null : parent.firstSiblingOf(this);
        }

        boolean hasContent() {
            return content != null;
        }

        QBLevel<T> nextSibling() {
            QBLevel<T> next = this;
            QBLevel<T> sibling = next.findNextSibling();
            // Walk back up the tree to find the next sibling node.
            // It may be either a leaf or branch.
            while (sibling == null) {
                next = next.parent;
                if (next == null) {
                    break;
                }
                sibling = next.findNextSibling();
            }
            return sibling;
        }

        QBLevel<T> firstChild() {
            if (sw != null)
                return sw;
            if (nw != null)
                return nw;
            if (se != null)
                return se;
            return ne;
        }

        QBLevel<T> firstSiblingOf(final QBLevel<T> child) {
            switch (child.index) {
            case SW_INDEX:
                if (nw != null)
                    return nw;
            case NW_INDEX:
                if (se != null)
                    return se;
            case SE_INDEX:
                return ne;
            }
            return null;
        }

        QBLevel<T> nextNode() {
            if (!this.hasChildren())
                return this.nextSibling();
            return this.firstChild();
        }

        QBLevel<T> nextContentNode() {
            QBLevel<T> next = this.nextNode();
            if (next == null)
                return next;
            if (next.hasContent())
                return next;
            return next.nextContentNode();
        }

        void doAdd(T o) {
            if (consistency_testing) {
                if (!matches(o, this.bbox())) {
                    o.getBBox().getIndex(level);
                    o.getBBox().getIndex(level - 1);
                    abort("\nobject " + o + " does not belong in node at level: " + level + " bbox: " + this.bbox());
                }
            }
            doAddContent(o);
            if (isLeaf() && content.size() > MAX_OBJECTS_PER_LEVEL && level < QuadTiling.NR_LEVELS) {
                doSplit();
            }
        }

        void add(T o) {
            findBucket(o.getBBox()).doAdd(o);
        }

        private void search(BBox searchBbox, List<T> result) {
            if (!this.bbox().intersects(searchBbox))
                return;
            else if (bbox().bounds(searchBbox)) {
                buckets.searchCache = this;
            }

            if (this.hasContent()) {
                searchContents(searchBbox, result);
            }

            //TODO Coincidence vector should be calculated here and only buckets that match search_bbox should be checked

            if (nw != null) {
                nw.search(searchBbox, result);
            }
            if (ne != null) {
                ne.search(searchBbox, result);
            }
            if (se != null) {
                se.search(searchBbox, result);
            }
            if (sw != null) {
                sw.search(searchBbox, result);
            }
        }

        public String quads() {
            return Long.toHexString(quad);
        }

        int indexOf(QBLevel<T> findThis) {
            QBLevel<T>[] children = getChildren();
            for (int i = 0; i < QuadTiling.TILES_PER_LEVEL; i++) {
                if (children[i] == findThis)
                    return i;
            }
            return -1;
        }

        double width() {
            return bbox.width();
        }

        double height() {
            return bbox.height();
        }

        public BBox bbox() {
            return bbox;
        }

        /*
         * This gives the coordinate of the bottom-left
         * corner of the box
         */
        final LatLon coor() {
            return QuadTiling.tile2LatLon(this.quad);
        }

        void removeFromParent() {
            if (parent == null)
                return;

            if (!canRemove()) {
                abort("attempt to remove non-empty child: " + this.content + ' ' + Arrays.toString(this.getChildren()));
            }

            if (parent.nw == this) {
                parent.nw = null;
            } else if (parent.ne == this) {
                parent.ne = null;
            } else if (parent.sw == this) {
                parent.sw = null;
            } else if (parent.se == this) {
                parent.se = null;
            }

            if (parent.canRemove()) {
                parent.removeFromParent();
            }
        }

        boolean canRemove() {
            if (content != null && !content.isEmpty())
                return false;
            if (this.hasChildren())
                return false;
            return true;
        }
    }

    private QBLevel<T> root;
    private QBLevel<T> searchCache;
    private int size;

    /**
     * Constructs a new {@code QuadBuckets}.
     */
    public QuadBuckets() {
        clear();
    }

    @Override
    public final void clear() {
        root = new QBLevel<>(this);
        searchCache = null;
        size = 0;
    }

    @Override
    public boolean add(T n) {
        root.add(n);
        size++;
        return true;
    }

    @Override
    public boolean retainAll(Collection<?> objects) {
        for (T o : this) {
            if (objects.contains(o)) {
                continue;
            }
            if (!this.remove(o))
                return false;
        }
        return true;
    }

    @Override
    public boolean removeAll(Collection<?> objects) {
        boolean changed = false;
        for (Object o : objects) {
            changed = changed | remove(o);
        }
        return changed;
    }

    @Override
    public boolean addAll(Collection<? extends T> objects) {
        boolean changed = false;
        for (T o : objects) {
            changed = changed | this.add(o);
        }
        return changed;
    }

    @Override
    public boolean containsAll(Collection<?> objects) {
        for (Object o : objects) {
            if (!this.contains(o))
                return false;
        }
        return true;
    }

    @Override
    public boolean remove(Object o) {
        @SuppressWarnings("unchecked")
        T t = (T) o;
        searchCache = null; // Search cache might point to one of removed buckets
        QBLevel<T> bucket = root.findBucket(t.getBBox());
        if (bucket.removeContent(t)) {
            size--;
            return true;
        } else
            return false;
    }

    @Override
    public boolean contains(Object o) {
        @SuppressWarnings("unchecked")
        T t = (T) o;
        QBLevel<T> bucket = root.findBucket(t.getBBox());
        return bucket != null && bucket.content != null && bucket.content.contains(t);
    }

    /**
     * Converts to list.
     * @return elements as list
     */
    public List<T> toList() {
        List<T> a = new ArrayList<>();
        for (T n : this) {
            a.add(n);
        }
        return a;
    }

    @Override
    public Object[] toArray() {
        return this.toList().toArray();
    }

    @Override
    public <A> A[] toArray(A[] template) {
        return this.toList().toArray(template);
    }

    class QuadBucketIterator implements Iterator<T> {
        private QBLevel<T> currentNode;
        private int contentIndex;
        private int iteratedOver;
        QuadBuckets<T> qb;

        final QBLevel<T> nextContentNode(QBLevel<T> q) {
            if (q == null)
                return null;
            QBLevel<T> orig = q;
            QBLevel<T> next;
            next = q.nextContentNode();
            if (orig == next) {
                abort("got same leaf back leaf: " + q.isLeaf());
            }
            return next;
        }

        QuadBucketIterator(QuadBuckets<T> qb) {
            if (!qb.root.hasChildren() || qb.root.hasContent()) {
                currentNode = qb.root;
            } else {
                currentNode = nextContentNode(qb.root);
            }
            iteratedOver = 0;
            this.qb = qb;
        }

        @Override
        public boolean hasNext() {
            if (this.peek() == null)
                return false;
            return true;
        }

        T peek() {
            if (currentNode == null)
                return null;
            while ((currentNode.content == null) || (contentIndex >= currentNode.content.size())) {
                contentIndex = 0;
                currentNode = nextContentNode(currentNode);
                if (currentNode == null) {
                    break;
                }
            }
            if (currentNode == null || currentNode.content == null)
                return null;
            return currentNode.content.get(contentIndex);
        }

        @Override
        public T next() {
            T ret = peek();
            if (ret == null)
                throw new NoSuchElementException();
            contentIndex++;
            iteratedOver++;
            return ret;
        }

        @Override
        public void remove() {
            // two uses
            // 1. Back up to the thing we just returned
            // 2. move the index back since we removed
            //    an element
            contentIndex--;
            T object = peek();
            if (currentNode.removeContent(object))
                qb.size--;
        }
    }

    @Override
    public Iterator<T> iterator() {
        return new QuadBucketIterator(this);
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public boolean isEmpty() {
        return size == 0;
    }

    public List<T> search(BBox searchBbox) {
        List<T> ret = new ArrayList<>();
        // Doing this cuts down search cost on a real-life data set by about 25%
        if (searchCache == null) {
            searchCache = root;
        }
        // Walk back up the tree when the last search spot can not cover the current search
        while (searchCache != null && !searchCache.bbox().bounds(searchBbox)) {
            searchCache = searchCache.parent;
        }

        if (searchCache == null) {
            searchCache = root;
            Main.info("bbox: " + searchBbox + " is out of the world");
        }

        // Save parent because searchCache might change during search call
        QBLevel<T> tmp = searchCache.parent;

        searchCache.search(searchBbox, ret);

        // A way that spans this bucket may be stored in one
        // of the nodes which is a parent of the search cache
        while (tmp != null) {
            tmp.searchContents(searchBbox, ret);
            tmp = tmp.parent;
        }
        return ret;
    }
}
