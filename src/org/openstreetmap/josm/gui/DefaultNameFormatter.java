// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui;

import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trn;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.coor.CoordinateFormat;
import org.openstreetmap.josm.data.osm.Changeset;
import org.openstreetmap.josm.data.osm.NameFormatter;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.Way;

/**
 * This is the default implementation of a {@see NameFormatter} for names of {@see OsmPrimitive}s.
 *
 */
public class DefaultNameFormatter implements NameFormatter {

    static private DefaultNameFormatter instance;

    /**
     * Replies the unique instance of this formatter
     * 
     * @return the unique instance of this formatter
     */
    static public DefaultNameFormatter getInstance() {
        if (instance == null) {
            instance = new DefaultNameFormatter();
        }
        return instance;
    }

    /** the default list of tags which are used as naming tags in relations */
    static public final String[] DEFAULT_NAMING_TAGS_FOR_RELATIONS = {"name", "ref", "restriction", "note"};

    /** the current list of tags used as naming tags in relations */
    static private List<String> namingTagsForRelations =  null;

    /**
     * Replies the list of naming tags used in relations. The list is given (in this order) by:
     * <ul>
     *   <li>by the tag names in the preference <tt>relation.nameOrder</tt></li>
     *   <li>by the default tags in {@see #DEFAULT_NAMING_TAGS_FOR_RELATIONS}
     * </ul>
     * 
     * @return the list of naming tags used in relations
     */
    static public List<String> getNamingtagsForRelations() {
        if (namingTagsForRelations == null) {
            namingTagsForRelations = new ArrayList<String>(
                    Main.pref.getCollection("relation.nameOrder", Arrays.asList(DEFAULT_NAMING_TAGS_FOR_RELATIONS))
            );
        }
        return namingTagsForRelations;
    }


    /**
     * Decorates the name of primitive with its id, if the preference
     * <tt>osm-primitives.showid</tt> is set.
     * 
     * @param name  the name without the id
     * @param primitive the primitive
     * @return the decorated name
     */
    protected String decorateNameWithId(String name, OsmPrimitive primitive) {
        if (Main.pref.getBoolean("osm-primitives.showid"))
            return name + tr(" [id: {0}]", primitive.id);
        else
            return name;
    }

    /**
     * Formats a name for a node
     * 
     * @param node the node
     * @return the name
     */
    public String format(Node node) {
        String name = "";
        if (node.incomplete) {
            name = tr("incomplete");
        } else {
            if (Main.pref.getBoolean("osm-primitives.localize-name", true)) {
                name = node.getLocalName();
            } else {
                name = node.getName();
            }
            if (name == null) {
                name = node.id == 0 ? tr("node") : ""+ node.id;
            }
            name += " (" + node.getCoor().latToString(CoordinateFormat.getDefaultFormat()) + ", " + node.getCoor().lonToString(CoordinateFormat.getDefaultFormat()) + ")";
        }
        name = decorateNameWithId(name, node);
        return name;
    }

    /**
     * Formats a name for a way
     * 
     * @param way the way
     * @return the name
     */
    public String format(Way way) {
        String name = "";
        if (way.incomplete) {
            name = tr("incomplete");
        } else {
            if (Main.pref.getBoolean("osm-primitives.localize-name", true)) {
                name = way.getLocalName();
            } else {
                name = way.getName();
            }
            if (name == null) {
                name = way.get("ref");
            }
            if (name == null) {
                name =
                    (way.get("highway") != null) ? tr("highway") :
                        (way.get("railway") != null) ? tr("railway") :
                            (way.get("waterway") != null) ? tr("waterway") :
                                (way.get("landuse") != null) ? tr("landuse") : "";
            }

            int nodesNo = new HashSet<Node>(way.getNodes()).size();
            String nodes = trn("{0} node", "{0} nodes", nodesNo, nodesNo);
            name += (name.length() > 0) ? " ("+nodes+")" : nodes;
            if(way.errors != null) {
                name = "*"+name;
            }
        }
        name = decorateNameWithId(name, way);
        return name;
    }

    /**
     * Formats a name for a relation
     * 
     * @param relation the relation
     * @return the name
     */
    public String format(Relation relation) {
        String name;
        if (relation.incomplete) {
            name = tr("incomplete");
        } else {
            name = relation.get("type");
            if (name == null) {
                name = tr("relation");
            }

            name += " (";
            String nameTag = null;
            for (String n : getNamingtagsForRelations()) {
                if (n.equals("name")) {
                    if (Main.pref.getBoolean("osm-primitives.localize-name", true)) {
                        nameTag = relation.getLocalName();
                    } else {
                        nameTag = relation.getName();
                    }
                } else {
                    nameTag =  relation.get(n);
                }
                if (nameTag != null) {
                    break;
                }
            }
            if (nameTag == null) {
                name += Long.toString(relation.id) + ", ";
            } else {
                name += "\"" + nameTag + "\", ";
            }

            int mbno = relation.getMembersCount();
            name += trn("{0} member", "{0} members", mbno, mbno) + ")";
            if(relation.errors != null) {
                name = "*"+name;
            }
        }
        name = decorateNameWithId(name, relation);
        return name;
    }

    /**
     * Formats a name for a changeset
     * 
     * @param changeset the changeset
     * @return the name
     */
    public String format(Changeset changeset) {
        return tr("Changeset {0}",changeset.id);
    }
}
