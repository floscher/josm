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

	@Override
	public void visit(Visitor visitor) {
		visitor.visit(this);
	}

	@Override
	public void cloneFrom(OsmPrimitive osm) {
		super.cloneFrom(osm);
		segments.clear();
		segments.addAll(((Way)osm).segments);
	}
}
