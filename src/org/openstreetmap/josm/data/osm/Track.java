package org.openstreetmap.josm.data.osm;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.openstreetmap.josm.data.osm.visitor.Visitor;

/**
 * One full track, consisting of several track segments chained together.
 *
 * @author imi
 */
public class Track extends OsmPrimitive {

	/**
	 * All track segments in this track
	 */
	private final List<LineSegment> segments = new ArrayList<LineSegment>();

	
	/**
	 * Add the line segment to the track.
	 */
	public void add(LineSegment ls) {
		segments.add(ls);
		ls.parent.add(this);
	}

	/**
	 * Add the line segment at first position to the track. First position means,
	 * the line segment's start becomes the starting node.
	 * @param ls The line segment to add at starting position.
	 * @see #getStartingNode()
	 */
	public void addStart(LineSegment ls) {
		segments.add(ls);
		ls.parent.add(this);
	}

	/**
	 * Add all LineSegment's to the list of segments. 
	 * @param lineSegments The line segments to add.
	 */
	public void addAll(Collection<? extends LineSegment> lineSegments) {
		segments.addAll(lineSegments);
		for (LineSegment ls : lineSegments)
			ls.parent.add(this);
	}
	
	/**
	 * Remove the line segment from the track.
	 */
	public void remove(LineSegment ls) {
		if (segments.remove(ls))
			if (!ls.parent.remove(this))
				throw new IllegalStateException("Parent violation detected.");
	}

	/**
	 * Return an read-only collection. Do not alter the object returned.
	 * @return The read-only Collection of all segments.
	 */
	public Collection<LineSegment> segments() {
		return Collections.unmodifiableCollection(segments);
	}

	/**
	 * Return a merge of getAllNodes - calls to the line segments.
	 */
	@Override
	public Collection<Node> getAllNodes() {
		ArrayList<Node> nodes = new ArrayList<Node>();
		for (LineSegment ls : segments)
			nodes.addAll(ls.getAllNodes());
		return nodes;
	}
	/**
	 * The track is going to be destroyed. Unlink all back references.
	 */
	void destroy() {
		for (LineSegment ls : segments) {
			ls.parent.remove(this);
			if (ls.parent.isEmpty())
				ls.destroy();
		}
		segments.clear();
	}

	/**
	 * Tracks are equal, when all segments and the keys are equal
	 */
	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof Track))
			return false;
		if (!super.equals(obj))
			return false;
		Track t = (Track)obj;
		int size = segments.size();
		if (size != t.segments.size())
			return false;
		for (int i = 0; i < size; ++i)
			if (!segments.get(i).equals(t.segments.get(i)))
				return false;
		return true;
	}

	@Override
	public int hashCode() {
		int hash = super.hashCode();
		for (LineSegment ls : segments)
			hash += ls.hashCode();
		return hash;
	}

	/**
	 * Return the last node in the track. This is the node, which no line segment
	 * has as start, but at least one has it as end. If there are not exact one
	 * such nodes found, <code>null</code> is returned.
	 *
	 * TODO Currently does return just the end node in the list.
	 *
	 * @return The ending node, if there is one.
	 */
	public Node getEndingNode() {
		if (segments.isEmpty())
			return null;
		return segments.get(segments.size()-1).getEnd();
	}
	
	/**
	 * Return the last segment.
	 * @see #getEndingNode()
	 */
	public LineSegment getEndingSegment() {
		if (segments.isEmpty())
			return null;
		return segments.get(segments.size()-1);
	}

	/**
	 * Return the first node in the track. This is the node, which no line segment
	 * has as end, but at least one as starting node. If there are not exact one
	 * such nodes found, <code>null</code> is returned.
	 * 
	 * TODO Currently does return just the first node in the list.
	 * 
	 * @return The starting node, if there is one.
	 */
	public Node getStartingNode() {
		if (segments.isEmpty())
			return null;
		return segments.get(0).getStart();
	}
	
	/**
	 * Return the first segment.
	 * @see #getStartingNode()
	 */
	public LineSegment getStartingSegment() {
		if (segments.isEmpty())
			return null;
		return segments.get(0);
	}

	@Override
	public void visit(Visitor visitor) {
		visitor.visit(this);
	}
}
