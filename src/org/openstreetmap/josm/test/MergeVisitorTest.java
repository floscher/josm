package org.openstreetmap.josm.test;

import java.util.Date;

import junit.framework.TestCase;

import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Segment;
import org.openstreetmap.josm.data.osm.visitor.MergeVisitor;
import org.openstreetmap.josm.test.framework.Bug;
import org.openstreetmap.josm.test.framework.DataSetTestCaseHelper;

public class MergeVisitorTest extends TestCase {

	
	private DataSet ds;
	private Node dsNode;
	private Node n;
	private MergeVisitor v;

	@Override protected void setUp() throws Exception {
		ds = new DataSet();
		dsNode = DataSetTestCaseHelper.createNode(ds);
		v = new MergeVisitor(ds);
		n = DataSetTestCaseHelper.createNode(null);
    }


	public void testNodesMergeUpdate() {
		dsNode.id = 1;
		n.id = 1;
		n.timestamp = new Date();
		v.visit(n);
		assertEquals(dsNode, n);
	}
	public void testNodesMergeModified() {
		dsNode.id = 1;
		n.id = 1;
		n.modified = true;
		v.visit(n);
		assertEquals(dsNode, n);
	}
	public void testNodesConflictBothModified() {
		n.modified = true;
		dsNode.modified = true;
		v.visit(n);
		assertEquals(1, v.conflicts.size());
		assertFalse(n.equals(dsNode));
	}
	public void testNodesConflict() {
		dsNode.id = 1;
		dsNode.timestamp = new Date();
		n.id = 1;
		n.modified = true;
		n.timestamp = new Date(dsNode.timestamp.getTime()-1);
		v.visit(n);
		assertEquals(1, v.conflicts.size());
		assertSame(dsNode, v.conflicts.keySet().iterator().next());
		assertSame(n, v.conflicts.values().iterator().next());
	}
	public void testNodesConflict2() {
		dsNode.id = 1;
		dsNode.timestamp = new Date();
		dsNode.modified = true;
		n.id = 1;
		n.timestamp = new Date(dsNode.timestamp.getTime()+1);
		v.visit(n);
		assertEquals(1, v.conflicts.size());
	}
	public void testNodesConflictModifyDelete() {
		dsNode.id = 1;
		dsNode.modified = true;
		n.id = 1;
		n.delete(true);
		v.visit(n);
		assertEquals(1, v.conflicts.size());
	}
	public void testNodesMergeSamePosition() {
		n.id = 1; // new node comes from server
		dsNode.modified = true; // our node is modified
		dsNode.coor = new LatLon(n.coor.lat(), n.coor.lon());
		v.visit(n);
		v.fixReferences();
		assertEquals(0, v.conflicts.size());
		assertEquals(1, dsNode.id);
		assertFalse("updating a new node clear the modified state", dsNode.modified);
	}

	public void testFixReferencesConflicts() {
		// make two nodes mergable
		dsNode.id = 1;
		n.id = 1;
		n.timestamp = new Date();
		// have an old segment with the old node
		Segment sold = new Segment(dsNode, dsNode);
		sold.id = 23;
		sold.modified = true;
		ds.segments.add(sold);
		// have a conflicting segment point to the new node
		Segment s = new Segment(n,n);
		s.id = 23;
		s.modified = true;

		v.visit(n); // merge
		assertEquals(n.timestamp, dsNode.timestamp);
		v.visit(s);
		v.fixReferences();
		assertSame(s.from, dsNode);
		assertSame(s.to, dsNode);
	}
	
	public void testNoConflictForSame() {
		dsNode.id = 1;
		dsNode.modified = true;
		n.cloneFrom(dsNode);
		v.visit(n);
		assertEquals(0, v.conflicts.size());
	}

	/**
	 * Merge of an old segment with a new one. This should
	 * be mergable (if the nodes matches).
	 */
	public void testMergeOldSegmentsWithNew() {
		Node[] n = createNodes(ds, 2);
		Segment ls1 = DataSetTestCaseHelper.createSegment(ds, n[0], n[1]);
		ls1.id = 3;

		Node newnode = new Node(new LatLon(n[1].coor.lat(), n[1].coor.lon()));
		Segment newls = new Segment(n[0], newnode);

		v.visit(newls);
		assertEquals("segment should have been merged.", 1, ds.segments.size());
	}
	
	/**
	 * Nodes beeing merged are equal but should be the same.
	 */
	@Bug(54)
	public void testEqualNotSame() {
		ds = new DataSet();
		// create a dataset with segment a-b
		Node n[] = createNodes(ds, 2);
		Segment ls1 = DataSetTestCaseHelper.createSegment(ds, n[0], n[1]);
		ls1.id = 1;

		// create an other dataset with segment a'-c (a' is equal, but not same to a)
		DataSet ds2 = new DataSet();
		Node n2[] = createNodes(ds2, 2);
		n2[0].coor = new LatLon(n[0].coor.lat(), n[0].coor.lon());
		n2[0].id = 0;
		n2[1].id = 42;

		Segment ls2 = DataSetTestCaseHelper.createSegment(ds, n2[0], n2[1]);
		v = new MergeVisitor(ds);
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
