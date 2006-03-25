package org.openstreetmap.josm.test;

import junit.framework.TestCase;

import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.LineSegment;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.visitor.MergeVisitor;
import org.openstreetmap.josm.test.framework.Bug;
import org.openstreetmap.josm.test.framework.DataSetTestCaseHelper;

public class MergeVisitorTest extends TestCase {

	
	/**
	 * Merge of an old line segment with a new one. This should
	 * be mergable (if the nodes matches).
	 */
	public void testMergeOldLineSegmentsWithNew() {
		DataSet ds = new DataSet();
		Node[] n = createNodes(ds, 2);
		LineSegment ls1 = DataSetTestCaseHelper.createLineSegment(ds, n[0], n[1]);
		ls1.id = 3;

		Node newnode = new Node(new LatLon(n[1].coor.lat(), n[1].coor.lon()));
		LineSegment newls = new LineSegment(n[0], newnode);

		MergeVisitor v = new MergeVisitor(ds);
		v.visit(newls);
		assertEquals("line segment should have been merged.", 1, ds.lineSegments.size());
	}
	
	/**
	 * Nodes beeing merged are equal but not the same.
	 */
	@Bug(54)
	public void testEqualNotSame() {
		// create a dataset with line segment a-b
		DataSet ds = new DataSet();
		Node n[] = createNodes(ds, 2);
		LineSegment ls1 = DataSetTestCaseHelper.createLineSegment(ds, n[0], n[1]);
		ls1.id = 1;
		
		// create an other dataset with line segment a'-c (a' is equal, but not same to a)
		DataSet ds2 = new DataSet();
		Node n2[] = createNodes(ds2, 2);
		n2[0].coor = new LatLon(n[0].coor.lat(), n[0].coor.lon());
		n2[1].id = 42;
		LineSegment ls2 = DataSetTestCaseHelper.createLineSegment(ds, n2[0], n2[1]);
		
		MergeVisitor v = new MergeVisitor(ds);
		for (OsmPrimitive osm : ds2.allPrimitives())
			osm.visit(v);
		v.fixReferences();
		
		assertSame(ls1.from, ls2.from);
	}
	
	
	/**
	 * Create that amount of nodes and add them to the dataset. The id will be 1,2,3,4...
	 * @param amount Number of nodes to create.
	 * @return The created nodes.
	 */
	private Node[] createNodes(DataSet ds, int amount) {
		Node[] nodes = new Node[amount];
		for (int i = 0; i < amount; ++i) {
			nodes[i] = DataSetTestCaseHelper.createNode(ds);
			nodes[i].id = i+1;
		}
		return nodes;
	}
}
