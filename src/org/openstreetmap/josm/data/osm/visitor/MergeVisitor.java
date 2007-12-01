// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.data.osm.visitor;

import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;

import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Way;

/**
 * A visitor that get a data set at construction time and merge every visited object
 * into it.
 * 
 * @author imi
 */
public class MergeVisitor implements Visitor {

	/**
	 * Map from primitives in the database to visited primitives. (Attention: The other way 
	 * round than merged)
	 */
	public Map<OsmPrimitive, OsmPrimitive> conflicts = new HashMap<OsmPrimitive, OsmPrimitive>();

	private final DataSet ds;
	private final DataSet mergeds;

	private final HashMap<Long, Node> nodeshash = new HashMap<Long, Node>();
	private final HashMap<Long, Way> wayshash = new HashMap<Long, Way>();
	private final HashMap<Long, Relation> relshash = new HashMap<Long, Relation>();

	/**
	 * A list of all primitives that got replaced with other primitives.
	 * Key is the primitives in the other's dataset and the value is the one that is now
	 * in ds.nodes instead.
	 */
	private final Map<OsmPrimitive, OsmPrimitive> merged
		= new HashMap<OsmPrimitive, OsmPrimitive>();

	public MergeVisitor(DataSet ds, DataSet mergeds) {
		this.ds = ds;
		this.mergeds = mergeds;

		for (Node n : ds.nodes) if (n.id != 0) nodeshash.put(n.id, n);
		for (Way w : ds.ways) if (w.id != 0) wayshash.put(w.id, w);
		for (Relation r : ds.relations) if (r.id != 0) relshash.put(r.id, r);
	}

	/**
	 * Merge the node if the id matches with any of the internal set or if
	 * either id is zero, merge if lat/lon matches.
	 */
	public void visit(Node other) {
		if (mergeAfterId(ds.nodes, nodeshash, other))
			return;

		Node my = null;
		for (Node n : ds.nodes) {
			if (match(n, other) && ((mergeds == null) || (!mergeds.nodes.contains(n)))) {
				my = n;
				break;
			}
		}
		if (my == null)
			ds.nodes.add(other);
		else {
			merged.put(other, my);
			mergeCommon(my, other);
			if (my.modified && !other.modified)
				return;
			if (!my.coor.equalsEpsilon(other.coor)) {
				my.coor = other.coor;
				my.eastNorth = other.eastNorth;
				my.modified = other.modified;
			}
		}
	}

	/**
	 * Merge the way if id matches or if all nodes match and the
	 * id is zero of either way.
	 */
	public void visit(Way other) {
		fixWay(other);
		if (mergeAfterId(ds.ways, wayshash, other))
			return;

		Way my = null;
		for (Way w : ds.ways) {
			if (match(other, w) && ((mergeds == null) || (!mergeds.ways.contains(w)))) {
				my = w;
				break;
			}
		}
		if (my == null) {
			ds.ways.add(other);
		} else {
			merged.put(other, my);
			mergeCommon(my, other);
			if (my.modified && !other.modified)
				return;
			boolean same = true;
			Iterator<Node> it = other.nodes.iterator();
			for (Node n : my.nodes) {
				if (!match(n, it.next()))
					same = false;
			}
			if (!same) {
				my.modified = other.modified;
			}
		}
	}

	/**
	 * Merge the relation if id matches or if all members match and the
	 * id of either relation is zero.
	 */
	public void visit(Relation other) {
		fixRelation(other);
		if (mergeAfterId(ds.relations, relshash, other))
			return;

		Relation my = null;
		for (Relation e : ds.relations) {
			if (match(other, e) && ((mergeds == null) || (!mergeds.relations.contains(e)))) {
				my = e;
				break;
			}
		}

		if (my == null) {
			// Add the relation
			ds.relations.add(other);
		} else {
			merged.put(other, my);
			mergeCommon(my, other);
			if (my.modified && !other.modified)
				return;
			boolean same = true;
			if (other.members.size() != my.members.size()) {
					same = false;
			} else {
				for (RelationMember em : my.members) {
					if (!other.members.contains(em)) {
						same = false;
						break;
					}
				}
			}
			if (!same) {
				my.modified = other.modified;
			}
		}
	}

	/**
	 * Postprocess the dataset and fix all merged references to point to the actual
	 * data.
	 */
	public void fixReferences() {
		for (Way w : ds.ways) fixWay(w);
		for (Relation r : ds.relations) fixRelation(r);
		for (OsmPrimitive osm : conflicts.values())
			if (osm instanceof Way)
				fixWay((Way)osm);
			else if (osm instanceof Relation)
				fixRelation((Relation) osm);
	}

	private void fixWay(Way w) {
	    boolean replacedSomething = false;
	    LinkedList<Node> newNodes = new LinkedList<Node>();
	    for (Node n : w.nodes) {
	    	Node otherN = (Node) merged.get(n);
	    	newNodes.add(otherN == null ? n : otherN);
	    	if (otherN != null)
	    		replacedSomething = true;
	    }
	    if (replacedSomething) {
	    	w.nodes.clear();
	    	w.nodes.addAll(newNodes);
		}
    }

	private void fixRelation(Relation r) {
	    boolean replacedSomething = false;
	    LinkedList<RelationMember> newMembers = new LinkedList<RelationMember>();
	    for (RelationMember m : r.members) {
	    	OsmPrimitive otherP = merged.get(m.member);
			if (otherP == null) {
				newMembers.add(m);
			} else {
				RelationMember mnew = new RelationMember(m);
				mnew.member = otherP;
				newMembers.add(mnew);
	    		replacedSomething = true;
			}
	    }
	    if (replacedSomething) {
	    	r.members.clear();
	    	r.members.addAll(newMembers);
		}
	}

	/**
	 * @return Whether the nodes match (in sense of "be mergable").
	 */
	private boolean match(Node n1, Node n2) {
		if (n1.id == 0 || n2.id == 0)
			return n1.coor.equalsEpsilon(n2.coor);
		return n1.id == n2.id;
	}

	/**
	 * @return Whether the ways match (in sense of "be mergable").
	 */
	private boolean match(Way w1, Way w2) {
		if (w1.id == 0 || w2.id == 0) {
			if (w1.nodes.size() != w2.nodes.size())
			return false;
			Iterator<Node> it = w1.nodes.iterator();
			for (Node n : w2.nodes)
				if (!match(n, it.next()))
					return false;
			return true;
		}
		return w1.id == w2.id;
	}
	/**
	 * @return Whether the relations match (in sense of "be mergable").
	 */
	private boolean match(Relation w1, Relation w2) {
		// FIXME this is not perfect yet...
		if (w1.id == 0 || w2.id == 0) {
			if (w1.members.size() != w2.members.size())
				return false;
			for (RelationMember em : w1.members) {
				if (!w2.members.contains(em)) {
					return false;
				}
			}
			return true;
		}
		return w1.id == w2.id;
	}


	/**
	 * Merge the common parts of an osm primitive.
	 * @param my The object, the information gets merged into
	 * @param other The object, the information gets merged from
	 */
	private void mergeCommon(OsmPrimitive my, OsmPrimitive other) {
		if (other.deleted)
			my.delete(true);
		if (my.id == 0 || !my.modified || other.modified) {
			if (my.id == 0 && other.id != 0) {
				my.id = other.id;
				my.modified = other.modified; // match a new node
			} else if (my.id != 0 && other.id != 0 && other.modified)
				my.modified = true;
		}
		if (other.keys == null)
			return;
		if (my.keySet().containsAll(other.keys.keySet()))
			return;
		if (my.keys == null)
			my.keys = other.keys;
		else
			my.keys.putAll(other.keys);
		
		my.modified = true;
	}

	/**
	 * @return <code>true</code>, if no merge is needed or merge is performed already.
	 */
	private <P extends OsmPrimitive> boolean mergeAfterId(
			Collection<P> primitives, HashMap<Long, P> hash, P other) {
		// Fast-path merging of identical objects
		if (hash.containsKey(other.id)) {
			P my = hash.get(other.id);
			if (my.realEqual(other, true)) {
				merged.put(other, my);
				return true;
			}
		}

		for (P my : primitives) {
			Date myd = my.timestamp == null ? new Date(0) : my.getTimestamp();
			Date otherd = other.timestamp == null ? new Date(0) : other.getTimestamp();
			if (my.realEqual(other, false)) {
				merged.put(other, my);
				return true; // no merge needed.
			}
			if (my.realEqual(other, true)) {
				// they differ in modified/timestamp combination only. Auto-resolve it.
				merged.put(other, my);
				if (myd.before(otherd)) {
					my.modified = other.modified;
					my.timestamp = other.timestamp;
				}
				return true; // merge done.
			}
			if (my.id == other.id && my.id != 0) {
				if (my.incomplete || other.incomplete) {
					if (my.incomplete) {
						my.cloneFrom(other);
					}
				} else if (my.modified && other.modified) {
					conflicts.put(my, other);
				} else if (!my.modified && !other.modified) {
					if (myd.before(otherd)) {
						my.cloneFrom(other);
					}
				} else if (other.modified) {
					if (myd.after(otherd)) {
						conflicts.put(my, other);
					} else {
						my.cloneFrom(other);
					}
				} else if (my.modified) {
					if (myd.before(otherd)) {
						conflicts.put(my, other);
					}
				}
				merged.put(other, my);
				return true;
			}
		}
		return false;
	}
}
