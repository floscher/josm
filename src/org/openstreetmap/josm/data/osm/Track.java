package org.openstreetmap.josm.data.osm;

import java.util.ArrayList;
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
	public List<LineSegment> segments = new ArrayList<LineSegment>();
}
