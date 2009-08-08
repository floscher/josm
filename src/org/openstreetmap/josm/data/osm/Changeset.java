// License: GPL. Copyright 2007 by Martijn van Oosterhout and others
package org.openstreetmap.josm.data.osm;

import org.openstreetmap.josm.data.osm.visitor.Visitor;
import static org.openstreetmap.josm.tools.I18n.tr;

/**
 * Represents a single changeset in JOSM. For now its only used during
 * upload but in the future we may do more.
 *
 */
public final class Changeset extends OsmPrimitive {
    /**
     * Time of last modification to this object. This is not set by JOSM but
     * read from the server and delivered back to the server unmodified.
     */
    public String end_timestamp = null;

    /**
     * Time of first modification to this object. This is not set by JOSM but
     * read from the server and delivered back to the server unmodified.
     */
    public String start_timestamp = null;

    @Override
    public void visit(Visitor v) {
        v.visit(this);
    }

    public int compareTo(OsmPrimitive other) {
        if (other instanceof Changeset) return Long.valueOf(id).compareTo(other.id);
        return 1;
    }

    @Override
    public String getName() {
        return tr("Changeset {0}",id);
    }
}
