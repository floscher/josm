package org.openstreetmap.josm.test;

import junit.framework.TestCase;

import org.openstreetmap.josm.data.GeoPoint;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.LineSegment;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.visitor.MergeVisitor;
import org.openstreetmap.josm.test.framework.DataSetTestCaseHelper;

public class MergeVisitorTest extends TestCase {

	
	/**
	 * Merge of an old line segment with a new one. This should
	 * be mergable (if the nodes matches).
	 */
	public void testMergeOldLineSegmentsWithNew() {
		DataSet ds = new DataSet();
		Node n1 = DataSetTestCaseHelper.createNode(ds);
		n1.id = 1;
		Node n2 = DataSetTestCaseHelper.createNode(ds);
		n2.id = 2;
		LineSegment ls1 = DataSetTestCaseHelper.createLineSegment(ds, n1, n2);
		ls1.id = 3;

		Node newnode = new Node();
		newnode.coor = new GeoPoint(n2.coor.lat, n2.coor.lon);
		LineSegment newls = new LineSegment(n1, newnode);

		MergeVisitor v = new MergeVisitor(ds);
		v.visit(newls);
		assertEquals("line segment should have been merged.", 1, ds.lineSegments.size());
	}
}
