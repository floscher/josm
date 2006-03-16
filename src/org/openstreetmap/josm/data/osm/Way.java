package org.openstreetmap.josm.data.osm;

import java.util.ArrayList;
import java.util.List;

import org.openstreetmap.josm.data.osm.visitor.Visitor;

/**
 * One full way, consisting of several way segments chained together.
 *
 * @author imi
 */
public class Way extends OsmPrimitive {

	/**
	 * All way segments in this way
	 */
	public final List<LineSegment> segments = new ArrayList<LineSegment>();

	
	/**
	 * Return the last node in the way. This is the node, which no line segment
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
	 * Return the last segment.
	 * @see #getEndingNode()
	 */
	public LineSegment getEndingSegment() {
		if (segments.isEmpty())
			return null;
		return segments.get(segments.size()-1);
	}

	/**
	 * Return the first node in the way. This is the node, which no line segment
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
