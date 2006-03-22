package org.openstreetmap.josm.test.framework;

import java.util.Arrays;

import org.openstreetmap.josm.data.GeoPoint;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.LineSegment;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.Way;


/**
 * Test cases that need to manupulate a data set can use this helper.
 *  
 * @author Imi
 */
public class DataSetTestCaseHelper {

	/**
	 * Create a common dataset consisting of:
	 * - 5 random nodes
	 * - ls between node 0 and 1
	 * - ls between node 1 and 2
	 * - ls between node 3 and 4
	 * - a way with ls 0 and 1
	 */
	public static DataSet createCommon() {
		DataSet ds = new DataSet();
		Node n1 = createNode(ds);
		Node n2 = createNode(ds);
		Node n3 = createNode(ds);
		Node n4 = createNode(ds);
		Node n5 = createNode(ds);
		LineSegment ls1 = createLineSegment(ds, n1, n2);
		LineSegment ls2 = createLineSegment(ds, n2, n3);
		createLineSegment(ds, n4, n5);
		createWay(ds, ls1, ls2);
		return ds;
	}

	public static Way createWay(DataSet ds, LineSegment... lineSegments) {
		Way t = new Way();
		t.segments.addAll(Arrays.asList(lineSegments));
		ds.waies.add(t);
		return t;
	}
	
	/**
	 * Create a line segment with out of the given nodes.
	 */
	public static LineSegment createLineSegment(DataSet ds, Node n1, Node n2) {
		LineSegment ls = new LineSegment(n1, n2);
		ds.lineSegments.add(ls);
		return ls;
	}

	/**
	 * Add a random node.
	 */
	public static Node createNode(DataSet ds) {
		Node node = new Node(new GeoPoint(Math.random(), Math.random()));
		ds.nodes.add(node);
		return node;
	}

}
