package org.openstreetmap.josm.data.osm;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;

import org.openstreetmap.josm.data.osm.visitor.Visitor;


/**
 * One track line segment consisting of a pair of nodes (start/end) 
 *
 * @author imi
 */
public class LineSegment extends OsmPrimitive {

	/**
	 * The starting node of the line segment
	 */
	Node start;
	
	/**
	 * The ending node of the line segment
	 */
	Node end;

	/**
	 * The tracks, this line segment is part of.
	 */
	transient Collection<Track> parent = new LinkedList<Track>();

	/**
	 * Create an line segment from the given starting and ending node
	 * @param start	Starting node of the line segment.
	 * @param end	Ending node of the line segment.
	 */
	public LineSegment(Node start, Node end) {
		this.start = start;
		this.end = end;
		start.parentSegment.add(this);
		end.parentSegment.add(this);
	}

	/**
	 * Return all parent tracks this line segment is part of. The list is readonly.
	 */
	public Collection<Track> getParents() {
		return Collections.unmodifiableCollection(parent);
	}

	public void setStart(Node start) {
		this.start.parentSegment.remove(this);
		this.start = start;
		start.parentSegment.add(this);
	}
	public Node getStart() {
		return start;
	}
	public void setEnd(Node end) {
		this.end.parentSegment.remove(this);
		this.end = end;
		end.parentSegment.add(this);
	}
	public Node getEnd() {
		return end;
	}

	/**
	 * The LineSegment is going to be destroyed. Unlink all back references.
	 */
	void destroy() {
		start.parentSegment.remove(this);
		end.parentSegment.remove(this);
	}

	/**
	 * Return start and end in a list.
	 */
	@Override
	public Collection<Node> getAllNodes() {
		LinkedList<Node> nodes = new LinkedList<Node>();
		nodes.add(getStart());
		nodes.add(getEnd());
		return nodes;
	}

	@Override
	public void visit(Visitor visitor) {
		visitor.visit(this);
	}
}	
