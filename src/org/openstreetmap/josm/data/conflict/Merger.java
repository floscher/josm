package org.openstreetmap.josm.data.conflict;

import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;

import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Segment;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.visitor.AddVisitor;
import org.openstreetmap.josm.data.osm.visitor.Visitor;

/**
 * Merger provides an operation, that merges one dataset into an other, creating a list
 * of merged conflicts on the way.
 * 
 * @author Immanuel.Scholz
 */
public class Merger {

	/**
	 * The map of all conflicting primitives. Keys are elements from our dataset (given at
	 * constructor) and values are the associated primitives from the other dataset.
	 * 
	 * All references of the conflicting primitives point always to objects from our dataset.
	 * This ensures, that later one you can just call osm.copyFrom(conflicts.get(osm)) to
	 * "resolve" the conflict.
	 */
	public final Map<OsmPrimitive, OsmPrimitive> conflicts = new HashMap<OsmPrimitive, OsmPrimitive>();

	private final DataSet ds;

	/**
	 * Maps all resolved objects that were transferred to the own dataset. The key is 
	 * the object from the other dataset given to merge(). The value is the object that
	 * is the resolved one in the internal dataset. Both objects are from the same class.
	 */
	private Map<OsmPrimitive, OsmPrimitive> resolved = new HashMap<OsmPrimitive, OsmPrimitive>();


	private AddVisitor adder;

	/**
	 * Hold the result of a decission between two elements. <code>MY</code> is keep my own,
	 * THEIR<code>THEIR</code> means copy the other object over here and <code>CONFLICT</code>
	 * means that both objects have to be looked closer at to decide it finally (there are
	 * possible conflicts).
	 */
	private enum Solution {MY, THEIR, CONFLICT}

	/**
	 * @param ds The dataset that is the base to merge the other into.
	 */
	public Merger(DataSet ds) {
		this.ds = ds;
		adder = new AddVisitor(ds);
	}

	/**
	 * This function merges the given dataset into the one given at constructor. The output
	 * is dataset obtained at the constructor. Additional to that, a list of conflicts that
	 * need to be resolved is created as member variable <code>conflicts</code>.
	 * 
	 * @param other The dataset that should be merged into the one given at construction time.
	 */
	public void merge(DataSet other) {
		for (Node n : other.nodes)
			merge(n);
		for (Segment s : other.segments)
			merge(s);
		for (Way w : other.ways)
			merge(w);
	}


	/**
	 * Merge the object into the own dataset or add it as conflict.
	 */
	public void merge(OsmPrimitive their) {
		// find the matching partner in my dataset
		OsmPrimitive my;
		if (their.id == 0)
			my = findSimilar(their);
		else {
			my = find(their);
			if (my == null)
				my = findSimilar(their); // give it a second chance.
		}
		
		// partner are found, solve both primitives
		
		if (my == null) {
			// their is new? -> import their to my
			their.visit(adder);
			resolved.put(their, their);
			copyReferences(their);
			return;
		}
		Solution solution = solve(my, their);

		switch(solution) {
		case MY:
			resolved.put(my, my);
			return;
		case THEIR:
			resolved.put(their, my);
			copyReferences(their);
			my.cloneFrom(their);
			return;
		case CONFLICT:
			resolved.put(their, my); // thats correct. Put all references from the own in case of conflicts
			conflicts.put(my, their);
			copyReferences(their);
			return;
		}

	}

	/**
	 * Replace all references in the given primitive with those from resolved map
	 */
	private void copyReferences(OsmPrimitive osm) {
		osm.visit(new Visitor() {
			public void visit(Node n) {}
			public void visit(Segment s) {
				s.from = (Node)resolved.get(s.from);
				s.to = (Node)resolved.get(s.to);
				if (s.from == null || s.to == null)
					throw new NullPointerException();
            }
			public void visit(Way w) {
				Collection<Segment> segments = new LinkedList<Segment>(w.segments);
				w.segments.clear();
				for (Segment s : segments) {
					if (!resolved.containsKey(s))
						throw new NullPointerException();
					w.segments.add((Segment)resolved.get(s));
				}
            }
		});
    }

	/**
	 * Decide what solution is necessary for my and their object to be solved. If both
	 * objects are as good as each other, use my object (since it is cheaper to keep it than
	 * change everything).
	 *
	 * @return Which object should be used or conflict in case of problems.
	 */
	private Solution solve(OsmPrimitive my, OsmPrimitive their) {
		Date myDate = my.timestamp == null ? new Date(0) : my.timestamp;
		Date theirDate = their.timestamp == null ? new Date(0) : their.timestamp;
		Date baseDate = theirDate.before(myDate) ? theirDate : myDate;

		if (my.id == 0 && their.id != 0)
			return Solution.THEIR;
		if (my.id != 0 && their.id == 0)
			return Solution.MY;
		if (my.id == 0 && their.id == 0)
			return my.realEqual(their) ? Solution.MY : Solution.CONFLICT;

		// from here on, none primitives is new

		boolean myChanged = my.modified  || myDate.after(baseDate);
		boolean theirChanged = their.modified  || theirDate.after(baseDate);
		if (myChanged && theirChanged)
			return my.realEqual(their) ? Solution.MY : Solution.CONFLICT;
		if (theirChanged)
			return Solution.THEIR;
		if (my instanceof Segment && ((Segment)my).incomplete && !((Segment)their).incomplete)
			return Solution.THEIR;
		return Solution.MY;
	}

	/**
	 * Try to find the object in the own dataset.
	 * @return Either the object from the own dataset or <code>null</code> if not there.
	 */
	private OsmPrimitive find(OsmPrimitive osm) {
		for (OsmPrimitive own : ds.allPrimitives())
			if (own.equals(osm))
				return own;
		return null;
	}

	/**
     * Find an primitive from our dataset that matches the given primitive by having the 
     * same location. Do not look for the id for this.
     * 
     * @return For nodes, return the first node with the same location (in tolerance to the
     * servers imprecision epsilon). For segments return for same starting/ending nodes.
     * For ways, return if consist of exact the same segments.
     */
    private OsmPrimitive findSimilar(OsmPrimitive their) {
    	final OsmPrimitive[] ret = new OsmPrimitive[1];
    	Visitor v = new Visitor(){
			public void visit(Node n) {
				// go for an exact match first
				for (Node my : ds.nodes) {
					if (my.coor.equals(n.coor)) {
						ret[0] = my;
						return;
					}
				}
				// second chance with epsilon
				for (Node my : ds.nodes) {
					if (my.coor.equalsEpsilon(n.coor)) {
						ret[0] = my;
						return;
					}
				}
            }
			public void visit(Segment s) {
				for (Segment my : ds.segments) {
					if (my.from == s.from && my.to == s.to) {
						ret[0] = my;
						return;
					}
				}
            }
			public void visit(Way w) {
				for (Way my : ds.ways) {
					boolean eq = true;
					Iterator<Segment> myIt = my.segments.iterator();
					Iterator<Segment> theirIt = w.segments.iterator();
					while (myIt.hasNext() && theirIt.hasNext()) {
						if (myIt.next() != theirIt.next()) {
							eq = false;
							break;
						}
					}
					if (eq && !myIt.hasNext() && !theirIt.hasNext()) {
						ret[0] = my;
						return;
					}
				}
            }
    	};
    	their.visit(v);
    	return ret[0];
    }
}
