// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.io;

import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map.Entry;

import org.openstreetmap.josm.data.osm.Changeset;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.DataSource;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.visitor.Visitor;
import org.openstreetmap.josm.tools.DateUtils;

/**
 * Save the dataset into a stream as osm intern xml format. This is not using any
 * xml library for storing.
 * @author imi
 */
public class OsmWriter extends XmlWriter implements Visitor {

    public final String DEFAULT_API_VERSION = "0.6";

    /**
     * The counter for newly created objects. Starts at -1 and goes down.
     */
    private long newIdCounter = -1;

    /**
     * All newly created ids and their primitive that uses it. This is a back reference
     * map to allow references to use the correnct primitives.
     */
    public HashMap<OsmPrimitive, Long> usedNewIds = new HashMap<OsmPrimitive, Long>();

    private boolean osmConform;
    private boolean withBody = true;
    private String version;
    private Changeset changeset;

    public OsmWriter(PrintWriter out, boolean osmConform, String version) {
        super(out);
        this.osmConform = osmConform;
        this.version = (version == null ? DEFAULT_API_VERSION : version);
    }

    public void setWithBody(boolean wb) {
        this.withBody = wb;
    }
    public void setChangeset(Changeset cs) {
        this.changeset = cs;
    }
    public void setVersion(String v) {
        this.version = v;
    }

    public void header() {
        out.println("<?xml version='1.0' encoding='UTF-8'?>");
        out.print("<osm version='");
        out.print(version);
        out.println("' generator='JOSM'>");
    }
    public void footer() {
        out.println("</osm>");
    }

    public void writeContent(DataSet ds) {
        for (Node n : ds.nodes)
            if (shouldWrite(n)) {
                visit(n);
            }
        for (Way w : ds.ways)
            if (shouldWrite(w)) {
                visit(w);
            }
        for (Relation e : ds.relations)
            if (shouldWrite(e)) {
                visit(e);
            }
    }

    private boolean shouldWrite(OsmPrimitive osm) {
        return osm.id != 0 || !osm.deleted;
    }

    public void writeDataSources(DataSet ds) {
        for (DataSource s : ds.dataSources) {
            out.println("  <bounds minlat='"
                    + s.bounds.min.lat()+"' minlon='"
                    + s.bounds.min.lon()+"' maxlat='"
                    + s.bounds.max.lat()+"' maxlon='"
                    + s.bounds.max.lon()
                    +"' origin='"+XmlWriter.encode(s.origin)+"' />");
        }
    }

    public void visit(Node n) {
        if (n.incomplete) return;
        addCommon(n, "node");
        out.print(" lat='"+n.getCoor().lat()+"' lon='"+n.getCoor().lon()+"'");
        if (!withBody) {
            out.println("/>");
        } else {
            addTags(n, "node", true);
        }
    }

    public void visit(Way w) {
        if (w.incomplete) return;
        addCommon(w, "way");
        if (!withBody) {
            out.println("/>");
        } else {
            out.println(">");
            for (Node n : w.getNodes()) {
                out.println("    <nd ref='"+getUsedId(n)+"' />");
            }
            addTags(w, "way", false);
        }
    }

    public void visit(Relation e) {
        if (e.incomplete) return;
        addCommon(e, "relation");
        if (!withBody) {
            out.println("/>");
        } else {
            out.println(">");
            for (RelationMember em : e.getMembers()) {
                out.print("    <member type='");
                out.print(OsmPrimitiveType.from(em.member).getAPIName());
                out.println("' ref='"+getUsedId(em.member)+"' role='" +
                        XmlWriter.encode(em.role == null ? "" : em.role) + "' />");
            }
            addTags(e, "relation", false);
        }
    }

    public void visit(Changeset cs) {
        addCommon(cs, "changeset");
        out.println(">\n");
        addTags(cs, "changeset", false);
    }

    public final void footer(PrintWriter out) {
        out.println("</osm>");
    }

    /**
     * Return the id for the given osm primitive (may access the usedId map)
     */
    private long getUsedId(OsmPrimitive osm) {
        if (osm.id != 0)
            return osm.id;
        if (usedNewIds.containsKey(osm))
            return usedNewIds.get(osm);
        usedNewIds.put(osm, newIdCounter);
        return osmConform ? 0 : newIdCounter--;
    }

    private void addTags(OsmPrimitive osm, String tagname, boolean tagOpen) {
        if (osm.hasKeys()) {
            if (tagOpen) {
                out.println(">");
            }
            for (Entry<String, String> e : osm.entrySet()) {
                if ((osm instanceof Changeset) || !("created_by".equals(e.getKey())))
                    out.println("    <tag k='"+ XmlWriter.encode(e.getKey()) +
                            "' v='"+XmlWriter.encode(e.getValue())+ "' />");
            }
            out.println("  </" + tagname + ">");
        } else if (tagOpen) {
            out.println(" />");
        } else {
            out.println("  </" + tagname + ">");
        }
    }

    /**
     * Add the common part as the form of the tag as well as the XML attributes
     * id, action, user, and visible.
     */
    private void addCommon(OsmPrimitive osm, String tagname) {
        long id = getUsedId(osm);
        out.print("  <"+tagname);
        if (id != 0) {
            out.print(" id='"+getUsedId(osm)+"'");
        }
        if (!osmConform) {
            String action = null;
            if (osm.deleted) {
                action = "delete";
            } else if (osm.modified) {
                action = "modify";
            }
            if (action != null) {
                out.print(" action='"+action+"'");
            }
        }
        if (!osm.isTimestampEmpty()) {
            out.print(" timestamp='"+DateUtils.fromDate(osm.getTimestamp())+"'");
        }
        // user and visible added with 0.4 API
        if (osm.user != null) {
            out.print(" user='"+XmlWriter.encode(osm.user.name)+"'");
        }
        out.print(" visible='"+osm.visible+"'");
        if (osm.version != -1) {
            out.print(" version='"+osm.version+"'");
        }
        if (this.changeset != null && this.changeset.id != 0) {
            out.print(" changeset='"+this.changeset.id+"'" );
        }
    }

    public void close() {
        out.close();
    }

    public void flush() {
        out.flush();
    }
}
