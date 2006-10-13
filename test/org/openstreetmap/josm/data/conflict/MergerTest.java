package org.openstreetmap.josm.data.conflict;

import java.util.Date;

import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.Segment;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.testframework.Bug;
import org.openstreetmap.josm.testframework.DataSetTestCaseHelper;
import org.openstreetmap.josm.testframework.MotherObject;

public class MergerTest extends MotherObject {

	private DataSet ds;
	private Node my;
	private Node their;
	private Merger merger;

	private Segment createSegment(DataSet ds, boolean incomplete, boolean deleted, int id) {
    	Node n1 = DataSetTestCaseHelper.createNode(ds);
    	Node n2 = DataSetTestCaseHelper.createNode(ds);
    	Segment s = DataSetTestCaseHelper.createSegment(ds, n1, n2);
    	s.incomplete = incomplete;
    	s.id = id;
    	s.deleted = deleted;
    	s.timestamp = new Date();
    	return s;
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


	@Override protected void setUp() throws Exception {
		ds = new DataSet();
		my = DataSetTestCaseHelper.createNode(ds);
		merger = new Merger(ds);
		their = DataSetTestCaseHelper.createNode(null);
	}


	public void testNodesMergeUpdate() {
		my.id = 1;
		their.id = 1;
		their.timestamp = new Date();
		merger.merge(their);
		assertEquals(my, their);
	}
	public void testNodesMergeModified() {
		my.id = 1;
		their.id = 1;
		their.modified = true;
		merger.merge(their);
		assertEquals(my, their);
	}
	public void testNodesConflictBothModified() {
		their.modified = true;
		my.modified = true;
		their.id = 1;
		my.id = 1;
		merger.merge(their);
		assertEquals(1, merger.conflicts.size());
	}
	public void testNodesConflict() {
		my.id = 1;
		my.timestamp = new Date();
		their.id = 1;
		their.modified = true;
		their.timestamp = new Date(my.timestamp.getTime()-1);
		merger.merge(their);
		assertEquals(1, merger.conflicts.size());
		assertSame(my, merger.conflicts.keySet().iterator().next());
		assertSame(their, merger.conflicts.values().iterator().next());
	}

	public void testConflictWhenMergingUnmodifiedNewerPrimitiveOverModified() {
		my.id = their.id = 1;
		my.modified = true;

		my.timestamp = new Date();
		their.timestamp = new Date(my.timestamp.getTime()+1);
		
		merger.merge(their);
		
		assertEquals(1, merger.conflicts.size());
	}

	public void testNodesConflictModifyDelete() {
		my.id = 1;
		my.modified = true;
		their.id = 1;
		their.delete(true);
		merger.merge(their);
		assertEquals(1, merger.conflicts.size());
	}

	public void testNodesMergeSamePosition() {
		their.id = 1; // new node comes from server
		my.modified = true; // our node is modified
		my.coor = new LatLon(their.coor.lat(), their.coor.lon());
		merger.merge(their);
		assertEquals(0, merger.conflicts.size());
		assertEquals(1, my.id);
		assertFalse("updating a new node clear the modified state", my.modified);
	}

	public void testNoConflictNewNodesMerged() {
		assertEquals(0, their.id);
		assertEquals(0, my.id);
		merger.merge(their);
		assertEquals(0,merger.conflicts.size());
		assertTrue(ds.nodes.contains(their));
		assertEquals(2, ds.nodes.size());
	}

	/**
	 * Test that two new segments that have different from/to are not merged
	 */
	@Bug(101)
	public void testNewSegmentsWithDifferentNodesNotMerged() {
		Node my2 = createNode();
		Segment mySeg = new Segment(my, my2);
		my.id = 1;
		my2.id = 2;
		ds.nodes.add(my2);
		ds.segments.add(mySeg);
		
		DataSet ds2 = new DataSet();
		their = new Node(my);
		Node their2 = new Node(my2);
		Segment theirSeg = new Segment(their2, their);
		ds2.nodes.add(their);
		ds2.nodes.add(their2);
		ds2.segments.add(theirSeg);

		merger.merge(ds2);
		
		assertEquals(2, ds.segments.size());
	}
	
	public void testReferencesOfConflictingSegmentsPointToMyDataset() {
		// make two nodes mergable
		my.id = their.id = 1;
		their.timestamp = new Date();
		// have an old segment with the old node
		Segment sold = new Segment(my, my);
		ds.segments.add(sold);
		// have a conflicting segment point to the new node
		Segment s = new Segment(their,DataSetTestCaseHelper.createNode(null));
		sold.id = s.id = 23;
		sold.modified = s.modified = true;

		DataSet ds2 = new DataSet();
		ds2.nodes.add(s.from);
		ds2.nodes.add(s.to);
		ds2.segments.add(s);

		merger.merge(ds2);

		assertEquals(their.timestamp, my.timestamp);
		assertEquals(1, merger.conflicts.size());
		assertSame(s.from, my);
	}

	public void testNoConflictForSame() {
		my.id = 1;
		my.modified = true;
		their.cloneFrom(my);
		merger.merge(their);
		assertEquals(0, merger.conflicts.size());
	}

	/**
	 * Merge of an old segment with a new one. This should
	 * be mergable (if the nodes matches).
	 */
	public void testMergeOldSegmentsWithNew() {
		ds = new DataSet();
		Node n1 = createNode();
		Node n2 = createNode();
		Segment ls = new Segment(n1, n2);
		ls.id = 3;
		ds.nodes.add(n1);
		ds.nodes.add(n2);
		ds.segments.add(ls);
		merger = new Merger(ds);
		
		DataSet ds2 = new DataSet();
		Node newN1 = new Node(n1);
		Node newN2 = new Node(n2);
		Segment newLs = new Segment(newN1, newN2);
		ds2.nodes.add(newN1);
		ds2.nodes.add(newN2);
		ds2.segments.add(newLs);

		merger.merge(ds2);

		assertEquals("segment should have been merged.", 1, ds.segments.size());
	}

	/**
	 * Incomplete segments should always loose.
	 */
	public void testImportIncomplete() throws Exception {
		Segment s1 = DataSetTestCaseHelper.createSegment(ds, my, my);
		s1.id = 1;
		Segment s2 = new Segment(s1);
		s1.incomplete = true;
		s2.timestamp = new Date();
		merger.merge(s2);
		assertTrue(s1.realEqual(s2));
	}
	/**
	 * Incomplete segments should extend existing ways.
	 */
	public void testImportIncompleteExtendWays() throws Exception {
		Segment s1 = DataSetTestCaseHelper.createSegment(ds, my, my);
		Way w = DataSetTestCaseHelper.createWay(ds, new Segment[]{s1});
		s1.id = 1;
		Segment s2 = new Segment(s1);
		s1.incomplete = true;
		merger.merge(s2);
		assertEquals(1, w.segments.size());
		assertEquals(s2, w.segments.get(0));
		assertFalse(s2.incomplete);
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
		merger = new Merger(ds);
		merger.merge(ds2);

		assertSame(ls1.from, ls2.from);
	}


	public void testCloneWayNotIncomplete() {
		ds = new DataSet();
		Node[] n = createNodes(ds, 2);
		Segment s = DataSetTestCaseHelper.createSegment(ds, n[0], n[1]);
		Way w = DataSetTestCaseHelper.createWay(ds, s);
		
		merger = new Merger(ds);
		DataSet ds2 = new DataSet();
		ds2.nodes.add(n[0]);
		ds2.nodes.add(n[1]);
		ds2.segments.add(s);
		ds2.ways.add(w);
		merger.merge(ds2);

		Way w2 = new Way(w);
		w2.timestamp = new Date();
		Segment s2 = new Segment(s);
		s2.incomplete = true;
		w2.segments.clear();
		w2.segments.add(s2);
		merger.merge(w2);

		assertSame("Do not import incomplete segments when merging ways.", s, w.segments.iterator().next());
	}

	/**
	 * When merging an incomplete way over a dataset that contain already all
	 * necessary segments, the way must be completed.
	 */
	@Bug(117)
	public void testMergeIncompleteOnExistingDoesNotComplete() {
		// create a dataset with an segment (as base for the later incomplete way)
		ds = new DataSet();
		Node[] n = createNodes(ds, 2);
		Segment s = DataSetTestCaseHelper.createSegment(ds, n[0], n[1]);
		s.id = 23;
		// create an incomplete way which references the former segment
		Way w = new Way();
		Segment incompleteSegment = new Segment(s.id);
		w.segments.add(incompleteSegment);
		w.id = 42;

		// merge both
		merger = new Merger(ds);
		merger.merge(w);
		
		assertTrue(ds.ways.contains(w));
		assertEquals(1, w.segments.size());
		assertFalse(w.segments.get(0).incomplete);
	}
	
	/**
	 * Deleted segments should raise an conflict when merged over changed segments. 
	 */
	public void testMergeDeletedOverChangedConflict() {
		ds = new DataSet();
		createSegment(ds, false, false, 23).modified = true;
		Segment s = createSegment(null, false, true, 23);
		s.timestamp = new Date(new Date().getTime()+1);
		
		merger = new Merger(ds);
		merger.merge(s);
		
		assertEquals(1, merger.conflicts.size());
	}
}
