// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.Stack;
import java.util.logging.Logger;

import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.Way;

import org.openstreetmap.josm.actions.upload.CyclicUploadDependencyException;

/**
 * Represents a collection of {@see OsmPrimitive}s which should be uploaded to the
 * API.
 * The collection is derived from the modified primitives of an {@see DataSet}.
 * 
 */
public class APIDataSet {
    private LinkedList<OsmPrimitive> toAdd;
    private LinkedList<OsmPrimitive> toUpdate;
    private LinkedList<OsmPrimitive> toDelete;

    /**
     * creates a new empty data set
     */
    public APIDataSet() {
        toAdd = new LinkedList<OsmPrimitive>();
        toUpdate = new LinkedList<OsmPrimitive>();
        toDelete = new LinkedList<OsmPrimitive>();
    }

    /**
     * initializes the API data set with the modified primitives in <code>ds</code>
     * 
     * @param ds the data set. Ignored, if null.
     */
    public void init(DataSet ds) {
        if (ds == null) return;
        toAdd.clear();
        toUpdate.clear();
        toDelete.clear();

        for (OsmPrimitive osm :ds.allPrimitives()) {
            if (osm.get("josm/ignore") != null) {
                continue;
            }
            if (osm.getId() == 0 && !osm.isDeleted()) {
                toAdd.addLast(osm);
            } else if (osm.isModified() && !osm.isDeleted()) {
                toUpdate.addLast(osm);
            } else if (osm.isDeleted() && osm.getId() != 0 && osm.isModified()) {
                toDelete.addFirst(osm);
            }
        }
        sortDeleted();
    }

    /**
     * Ensures that primitives are deleted in the following order: Relations, then Ways,
     * then Nodes.
     * 
     */
    protected void sortDeleted() {
        Collections.sort(
                toDelete,
                new Comparator<OsmPrimitive>() {
                    public int compare(OsmPrimitive o1, OsmPrimitive o2) {
                        if (o1 instanceof Node && o2 instanceof Node)
                            return 0;
                        else if (o1 instanceof Node)
                            return 1;
                        else if (o2 instanceof Node)
                            return -1;

                        if (o1 instanceof Way && o2 instanceof Way)
                            return 0;
                        else if (o1 instanceof Way && o2 instanceof Relation)
                            return 1;
                        else if (o2 instanceof Way && o1 instanceof Relation)
                            return -1;

                        return 0;
                    }
                }
        );
    }
    /**
     * initializes the API data set with the modified primitives in <code>ds</code>
     * 
     * @param ds the data set. Ignored, if null.
     */
    public APIDataSet(DataSet ds) {
        this();
        init(ds);
    }

    /**
     * initializes the API data set with the primitives in <code>primitives</code>
     * 
     * @param primitives the collection of primitives
     */
    public APIDataSet(Collection<OsmPrimitive> primitives) {
        this();
        toAdd.clear();
        toUpdate.clear();
        toDelete.clear();
        for (OsmPrimitive osm: primitives) {
            if (osm.getId() == 0 && !osm.isDeleted()) {
                toAdd.addLast(osm);
            } else if (osm.isModified() && !osm.isDeleted()) {
                toUpdate.addLast(osm);
            } else if (osm.isDeleted() && osm.getId() != 0 && osm.isModified()) {
                toDelete.addFirst(osm);
            }
        }
        sortDeleted();
    }

    /**
     * Replies true if there are no primitives to upload
     * 
     * @return true if there are no primitives to upload
     */
    public boolean isEmpty() {
        return toAdd.isEmpty() && toUpdate.isEmpty() && toDelete.isEmpty();
    }

    /**
     * Replies the primitives which should be added to the OSM database
     * 
     * @return the primitives which should be added to the OSM database
     */
    public List<OsmPrimitive> getPrimitivesToAdd() {
        return toAdd;
    }

    /**
     * Replies the primitives which should be updated in the OSM database
     * 
     * @return the primitives which should be updated in the OSM database
     */
    public List<OsmPrimitive> getPrimitivesToUpdate() {
        return toUpdate;
    }

    /**
     * Replies the primitives which should be deleted in the OSM database
     * 
     * @return the primitives which should be deleted in the OSM database
     */
    public List<OsmPrimitive> getPrimitivesToDelete() {
        return toDelete;
    }

    /**
     * Replies all primitives
     * 
     * @return all primitives
     */
    public List<OsmPrimitive> getPrimitives() {
        LinkedList<OsmPrimitive> ret = new LinkedList<OsmPrimitive>();
        ret.addAll(toAdd);
        ret.addAll(toUpdate);
        ret.addAll(toDelete);
        return ret;
    }

    /**
     * Adjusts the upload order for new relations. Child relations are uploaded first,
     * parent relations second.
     * 
     * This method detects cyclic dependencies in new relation. Relations with cyclic
     * dependencies can't be uploaded.
     * 
     * @throws CyclicUploadDependencyException thrown, if a cyclic dependency is detected
     */
    public void adjustRelationUploadOrder() throws CyclicUploadDependencyException{
        LinkedList<OsmPrimitive> newToAdd = new LinkedList<OsmPrimitive>();
        newToAdd.addAll(OsmPrimitive.getFilteredList(toAdd, Node.class));
        newToAdd.addAll(OsmPrimitive.getFilteredList(toAdd, Way.class));

        List<Relation> relationsToAdd = OsmPrimitive.getFilteredList(toAdd, Relation.class);
        List<Relation> noProblemRelations = filterRelationsNotReferringToNewRelations(relationsToAdd);
        newToAdd.addAll(noProblemRelations);
        relationsToAdd.removeAll(noProblemRelations);

        RelationUploadDependencyGraph graph = new RelationUploadDependencyGraph(relationsToAdd);
        newToAdd.addAll(graph.computeUploadOrder());
        toAdd = newToAdd;
    }

    /**
     * Replies the subset of relations in <code>relations</code> which are not referring to any
     * new relation
     * 
     * @param relations a list of relations
     * @return the subset of relations in <code>relations</code> which are not referring to any
     * new relation
     */
    protected List<Relation> filterRelationsNotReferringToNewRelations(Collection<Relation> relations) {
        List<Relation> ret = new LinkedList<Relation>();
        for (Relation relation: relations) {
            boolean refersToNewRelation = false;
            for (RelationMember m : relation.getMembers()) {
                if (m.isRelation() && m.getMember().getId() <= 0) {
                    refersToNewRelation = true;
                    break;
                }
            }
            if (!refersToNewRelation) {
                ret.add(relation);
            }
        }
        return ret;
    }

    /**
     * Utility class to sort a collection of of new relations with their dependencies
     * topologically.
     * 
     */
    private class RelationUploadDependencyGraph {
        private final Logger logger = Logger.getLogger(RelationUploadDependencyGraph.class.getName());
        private HashMap<Relation, Set<Relation>> children;
        private Collection<Relation> relations;
        private Set<Relation> visited;
        private List<Relation> uploadOrder;

        public RelationUploadDependencyGraph() {
            this.children = new HashMap<Relation, Set<Relation>>();
            this.visited = new HashSet<Relation>();
        }

        public RelationUploadDependencyGraph(Collection<Relation> relations) {
            this();
            build(relations);
        }

        public void build(Collection<Relation> relations) {
            this.relations = new HashSet<Relation>();
            for(Relation relation: relations) {
                if (relation.getId() > 0 ) {
                    continue;
                }
                this.relations.add(relation);
                for (RelationMember m: relation.getMembers()) {
                    if (m.isRelation() && m.getMember().getId() == 0) {
                        addDependency(relation, (Relation)m.getMember());
                    }
                }
            }
        }

        public Set<Relation> getChildren(Relation relation) {
            Set<Relation> p = children.get(relation);
            if (p == null) {
                p = new HashSet<Relation>();
                children.put(relation, p);
            }
            return p;
        }

        public void addDependency(Relation relation, Relation child) {
            getChildren(relation).add(child);
        }

        protected void visit(Stack<Relation> path, Relation current) throws CyclicUploadDependencyException{
            if (path.contains(current)) {
                path.push(current);
                throw new CyclicUploadDependencyException(path);
            }
            if (!visited.contains(current)) {
                path.push(current);
                visited.add(current);
                for (Relation dependent : getChildren(current)) {
                    visit(path,dependent);
                }
                uploadOrder.add(current);
                path.pop();
            }
        }

        public List<Relation> computeUploadOrder() throws CyclicUploadDependencyException {
            visited = new HashSet<Relation>();
            uploadOrder = new LinkedList<Relation>();
            Stack<Relation> path = new Stack<Relation>();
            for (Relation relation: relations) {
                visit(path, relation);
            }
            ArrayList<Relation> ret = new ArrayList<Relation>(relations);
            Collections.sort(
                    ret,
                    new Comparator<Relation>() {
                        public int compare(Relation o1, Relation o2) {
                            return new Integer(uploadOrder.indexOf(o1)).compareTo(uploadOrder.indexOf(o2));
                        }
                    }
            );
            return ret;
        }
    }
}
