// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.data.osm;

import java.awt.geom.Area;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.openstreetmap.josm.data.SelectionChangedListener;
import static org.openstreetmap.josm.tools.I18n.tr;

/**
 * DataSet is the data behind the application. It can consists of only a few points up to the whole
 * osm database. DataSet's can be merged together, saved, (up/down/disk)loaded etc.
 *
 * Note that DataSet is not an osm-primitive and so has no key association but a few members to
 * store some information.
 *
 * @author imi
 */
public class DataSet implements Cloneable {

    /**
     * The API version that created this data set, if any.
     */
    public String version;

    /**
     * All nodes goes here, even when included in other data (ways etc). This enables the instant
     * conversion of the whole DataSet by iterating over this data structure.
     */
    public Collection<Node> nodes = new LinkedList<Node>();

    /**
     * All ways (Streets etc.) in the DataSet.
     *
     * The way nodes are stored only in the way list.
     */
    public Collection<Way> ways = new LinkedList<Way>();

    /**
     * All relations/relationships
     */
    public Collection<Relation> relations = new LinkedList<Relation>();

    /**
     * All data sources of this DataSet.
     */
    public Collection<DataSource> dataSources = new LinkedList<DataSource>();

    /**
     * A list of listeners to selection changed events. The list is static, as listeners register
     * themselves for any dataset selection changes that occur, regardless of the current active
     * dataset. (However, the selection does only change in the active layer)
     */
    public static Collection<SelectionChangedListener> selListeners = new LinkedList<SelectionChangedListener>();

    /**
     * @return A collection containing all primitives of the dataset. The data is ordered after:
     * first come nodes, then ways, then relations. Ordering in between the categories is not
     * guaranteed.
     */
    public List<OsmPrimitive> allPrimitives() {
        List<OsmPrimitive> o = new LinkedList<OsmPrimitive>();
        o.addAll(nodes);
        o.addAll(ways);
        o.addAll(relations);
        return o;
    }

    /**
     * @return A collection containing all not-deleted primitives (except keys).
     */
    public Collection<OsmPrimitive> allNonDeletedPrimitives() {
        Collection<OsmPrimitive> o = new LinkedList<OsmPrimitive>();
        for (OsmPrimitive osm : allPrimitives())
            if (osm.visible && !osm.deleted) {
                o.add(osm);
            }
        return o;
    }

    public Collection<OsmPrimitive> allNonDeletedCompletePrimitives() {
        Collection<OsmPrimitive> o = new LinkedList<OsmPrimitive>();
        for (OsmPrimitive osm : allPrimitives())
            if (osm.visible && !osm.deleted && !osm.incomplete) {
                o.add(osm);
            }
        return o;
    }

    public Collection<OsmPrimitive> allNonDeletedPhysicalPrimitives() {
        Collection<OsmPrimitive> o = new LinkedList<OsmPrimitive>();
        for (OsmPrimitive osm : allPrimitives())
            if (osm.visible && !osm.deleted && !osm.incomplete && !(osm instanceof Relation)) {
                o.add(osm);
            }
        return o;
    }

    /**
     * Adds a primitive to the dataset
     * 
     * @param primitive the primitive. Ignored if null.
     */
    public void addPrimitive(OsmPrimitive primitive) {
        if (primitive == null)
            return;
        if (primitive instanceof Node) {
            nodes.add((Node) primitive);
        } else if (primitive instanceof Way) {
            ways.add((Way) primitive);
        } else if (primitive instanceof Relation) {
            relations.add((Relation) primitive);
        }
    }

    /**
     * Removes a primitive from the dataset. This method only removes the
     * primitive form the respective collection of primitives managed
     * by this dataset, i.e. from {@see #nodes}, {@see #ways}, or
     * {@see #relations}. References from other primitives to this
     * primitive are left unchanged.
     * 
     * @param primitive the primitive. Ignored if null.
     */
    public void removePrimitive(OsmPrimitive primitive) {
        if (primitive == null)
            return;
        if (primitive instanceof Node) {
            nodes.remove(primitive);
        } else if (primitive instanceof Way) {
            ways.remove(primitive);
        } else if (primitive instanceof Relation) {
            relations.remove(primitive);
        }
    }

    public Collection<OsmPrimitive> getSelectedNodesAndWays() {
        Collection<OsmPrimitive> sel = getSelected(nodes);
        sel.addAll(getSelected(ways));
        return sel;
    }


    /**
     * Return a list of all selected objects. Even keys are returned.
     * @return List of all selected objects.
     */
    public Collection<OsmPrimitive> getSelected() {
        Collection<OsmPrimitive> sel = getSelected(nodes);
        sel.addAll(getSelected(ways));
        sel.addAll(getSelected(relations));
        return sel;
    }

    /**
     * Return selected nodes.
     */
    public Collection<OsmPrimitive> getSelectedNodes() {
        return getSelected(nodes);
    }

    /**
     * Return selected ways.
     */
    public Collection<OsmPrimitive> getSelectedWays() {
        return getSelected(ways);
    }

    /**
     * Return selected relations.
     */
    public Collection<OsmPrimitive> getSelectedRelations() {
        return getSelected(relations);
    }

    public void setSelected(Collection<? extends OsmPrimitive> selection) {
        clearSelection(nodes);
        clearSelection(ways);
        clearSelection(relations);
        for (OsmPrimitive osm : selection) {
            osm.selected = true;
        }
        fireSelectionChanged(selection);
    }

    public void setSelected(OsmPrimitive... osm) {
        if (osm.length == 1 && osm[0] == null) {
            setSelected();
            return;
        }
        clearSelection(nodes);
        clearSelection(ways);
        clearSelection(relations);
        for (OsmPrimitive o : osm)
            if (o != null) {
                o.selected = true;
            }
        fireSelectionChanged(Arrays.asList(osm));
    }

    /**
     * Remove the selection from every value in the collection.
     * @param list The collection to remove the selection from.
     */
    private void clearSelection(Collection<? extends OsmPrimitive> list) {
        if (list == null)
            return;
        for (OsmPrimitive osm : list) {
            osm.selected = false;
        }
    }

    /**
     * Return all selected items in the collection.
     * @param list The collection from which the selected items are returned.
     */
    private Collection<OsmPrimitive> getSelected(Collection<? extends OsmPrimitive> list) {
        Collection<OsmPrimitive> sel = new HashSet<OsmPrimitive>();
        if (list == null)
            return sel;
        for (OsmPrimitive osm : list)
            if (osm.selected && !osm.deleted) {
                sel.add(osm);
            }
        return sel;
    }

    /**
     * Remember to fire an selection changed event. A call to this will not fire the event
     * immediately. For more,
     * @see SelectionChangedListener
     */
    public static void fireSelectionChanged(Collection<? extends OsmPrimitive> sel) {
        for (SelectionChangedListener l : selListeners) {
            l.selectionChanged(sel);
        }
    }

    @Override public DataSet clone() {
        DataSet ds = new DataSet();
        for (Node n : nodes) {
            ds.nodes.add(new Node(n));
        }
        for (Way w : ways) {
            ds.ways.add(new Way(w));
        }
        for (Relation e : relations) {
            ds.relations.add(new Relation(e));
        }
        for (DataSource source : dataSources) {
            ds.dataSources.add(new DataSource(source.bounds, source.origin));
        }
        ds.version = version;
        return ds;
    }

    /**
     * Returns the total area of downloaded data (the "yellow rectangles").
     * @return Area object encompassing downloaded data.
     */
    public Area getDataSourceArea() {
        if (dataSources.isEmpty()) return null;
        Area a = new Area();
        for (DataSource source : dataSources) {
            // create area from data bounds
            a.add(new Area(source.bounds.asRect()));
        }
        return a;
    }

    // Provide well-defined sorting for collections of OsmPrimitives.
    // FIXME: probably not a good place to put this code.
    public static OsmPrimitive[] sort(Collection<? extends OsmPrimitive> list) {
        OsmPrimitive[] selArr = new OsmPrimitive[list.size()];
        final HashMap<Object, String> h = new HashMap<Object, String>();
        selArr = list.toArray(selArr);
        Arrays.sort(selArr, new Comparator<OsmPrimitive>() {
            public int compare(OsmPrimitive a, OsmPrimitive b) {
                if (a.getClass() == b.getClass()) {
                    String as = h.get(a);
                    if (as == null) {
                        as = a.getName();
                        h.put(a, as);
                    }
                    String bs = h.get(b);
                    if (bs == null) {
                        bs = b.getName();
                        h.put(b, bs);
                    }
                    int res = as.compareTo(bs);
                    if (res != 0)
                        return res;
                }
                return a.compareTo(b);
            }
        });
        return selArr;
    }

    /**
     * returns a  primitive with a given id from the data set. null, if no such primitive
     * exists
     *
     * @param id  the id, > 0 required
     * @return the primitive
     * @exception IllegalArgumentException thrown, if id <= 0
     */
    public OsmPrimitive getPrimitiveById(long id) {
        if (id <= 0)
            throw new IllegalArgumentException(tr("parameter {0} > 0 required. Got {1}.", "id", id));
        for (OsmPrimitive primitive : nodes) {
            if (primitive.id == id) return primitive;
        }
        for (OsmPrimitive primitive : ways) {
            if (primitive.id == id) return primitive;
        }
        for (OsmPrimitive primitive : relations) {
            if (primitive.id == id) return primitive;
        }
        return null;
    }

    public Set<Long> getPrimitiveIds() {
        HashSet<Long> ret = new HashSet<Long>();
        for (OsmPrimitive primitive : nodes) {
            ret.add(primitive.id);
        }
        for (OsmPrimitive primitive : ways) {
            ret.add(primitive.id);
        }
        for (OsmPrimitive primitive : relations) {
            ret.add(primitive.id);
        }
        return ret;
    }

    /**
     * Replies the set of ids of all complete primitivies (i.e. those with
     * ! primitive.incomplete)
     * 
     * @return the set of ids of all complete primitivies
     */
    public Set<Long> getCompletePrimitiveIds() {
        HashSet<Long> ret = new HashSet<Long>();
        for (OsmPrimitive primitive : nodes) {
            if (!primitive.incomplete) {
                ret.add(primitive.id);
            }
        }
        for (OsmPrimitive primitive : ways) {
            if (! primitive.incomplete) {
                ret.add(primitive.id);
            }
        }
        for (OsmPrimitive primitive : relations) {
            if (! primitive.incomplete) {
                ret.add(primitive.id);
            }
        }
        return ret;
    }

    protected void deleteWay(Way way) {
        way.nodes.clear();
        way.delete(true);
    }

    /**
     * removes all references from ways in this dataset to a particular node
     * 
     * @param node the node
     */
    public void unlinkNodeFromWays(Node node) {
        for (Way way: ways) {
            if (way.nodes.contains(node)) {
                way.nodes.remove(node);
                if (way.nodes.size() < 2) {
                    deleteWay(way);
                }
            }
        }
    }

    /**
     * removes all references from relations in this dataset  to this primitive
     * 
     * @param primitive the primitive
     */
    public void unlinkPrimitiveFromRelations(OsmPrimitive primitive) {
        for (Relation relation : relations) {
            Iterator<RelationMember> it = relation.members.iterator();
            while(it.hasNext()) {
                RelationMember member = it.next();
                if (member.member.equals(primitive)) {
                    it.remove();
                }
            }
        }
    }

    /**
     * removes all references from from other primitives  to the
     * referenced primitive
     * 
     * @param referencedPrimitive the referenced primitive
     */
    public void unlinkReferencesToPrimitive(OsmPrimitive referencedPrimitive) {
        if (referencedPrimitive instanceof Node) {
            unlinkNodeFromWays((Node)referencedPrimitive);
            unlinkPrimitiveFromRelations(referencedPrimitive);
        } else {
            unlinkPrimitiveFromRelations(referencedPrimitive);
        }
    }


}
