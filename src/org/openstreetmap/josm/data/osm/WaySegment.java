// License: GPL. Copyright 2007 by Gabriel Ebner
package org.openstreetmap.josm.data.osm;

/**
 * A segment consisting of 2 consecutive nodes out of a way.
 */
public final class WaySegment implements Comparable<WaySegment> {
    /**
     * The way.
     */
    public Way way;

    /**
     * The index of one of the 2 nodes in the way.  The other node has the
     * index <code>lowerIndex + 1</code>.
     */
    public int lowerIndex;

    public WaySegment(Way w, int i) {
        way = w;
        lowerIndex = i;
    }

    public Node getFirstNode(){
        return way.getNode(lowerIndex);
    }

    public Node getSecondNode(){
        return way.getNode(lowerIndex + 1);
    }

    /**
     * returns this way segment as complete way.
     * @return
     */
    public Way toWay() {
        Way w = new Way();
        w.addNode(getFirstNode());
        w.addNode(getSecondNode());
        return w;
    }

    @Override public boolean equals(Object o) {
        return o != null && o instanceof WaySegment
            && ((WaySegment) o).way == way
            && ((WaySegment) o).lowerIndex == lowerIndex;
    }

    @Override public int hashCode() {
        return way.hashCode() ^ lowerIndex;
    }

    @Override
    public int compareTo(WaySegment o) {
        return equals(o) ? 0 : toWay().compareTo(o.toWay());
    }
}
