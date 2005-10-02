package org.openstreetmap.josm.data.osm;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * One full track, consisting of several track segments chained together.
 *
 * @author imi
 */
public class Track extends OsmPrimitive {

	/**
	 * All track segments in this track
	 */
	public final List<LineSegment> segments = new ArrayList<LineSegment>();

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
		return segments.get(segments.size()-1).end;
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
		return segments.get(0).start;
	}
}
