package org.openstreetmap.josm.data.osm;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;

import org.openstreetmap.josm.data.SelectionChangedListener;
import org.openstreetmap.josm.testframework.MotherObject;

public class DataSetTest extends MotherObject {

	private final class TestSelectionChangeListener implements SelectionChangedListener {
	    public Collection<? extends OsmPrimitive> called;
		public void selectionChanged(Collection<? extends OsmPrimitive> newSelection) {
	    	called = newSelection;
	    }
    }

	private DataSet ds;
	private Node node1;
	private Node node2;
	private Node node3;
	private Segment segment;
	private Way way;

	@Override protected void setUp() throws Exception {
		super.setUp();
		ds = createDataSet();
		Iterator<Node> it = ds.nodes.iterator();
		node1 = it.next();
		node2 = it.next();
		node3 = it.next();
		segment = ds.segments.iterator().next();
		way = ds.ways.iterator().next();
	}

	public void testAllPrimitives() {
		Collection<OsmPrimitive> all = ds.allPrimitives();
		assertContainsSame(all, node1, node2, node3, segment, way);
	}

	public void testAllNonDeletedPrimitives() {
		assertEquals(5, ds.allNonDeletedPrimitives().size());
		node1.deleted = true;
		assertEquals(4, ds.allNonDeletedPrimitives().size());
	}

	public void testClearSelection() {
		node1.selected = true;
		ds.clearSelection();
		assertFalse(node1.selected);
	}

	public void testGetSelected() {
		node1.selected = true;
		segment.selected = true;
		Collection<OsmPrimitive> sel = ds.getSelected();
		assertContainsSame(sel, node1, segment);
	}

	public void testSetSelected() {
		Collection<OsmPrimitive> sel = new LinkedList<OsmPrimitive>();
		sel.add(node1);
		sel.add(way);
		ds.setSelected(sel);
		assertTrue(node1.selected);
		assertFalse(node2.selected);
		assertTrue(way.selected);
	}

	public void testSetSelectedOsmPrimitive() {
		ds.setSelected(node3);
		assertTrue(node3.selected);
		assertFalse(node2.selected);
	}

	public void testFireSelectionChanged() {
		TestSelectionChangeListener l = new TestSelectionChangeListener();
		ds.addSelectionChangedListener(l);
		ds.setSelected(segment);
		assertNotNull(l.called);
		assertEquals(1, l.called.size());
		assertSame(segment, l.called.iterator().next());
	}

	public void testAddRemoveSelectionChangedListener() {
		TestSelectionChangeListener l = new TestSelectionChangeListener();
		ds.addSelectionChangedListener(l);
		ds.removeSelectionChangedListener(l);
		ds.setSelected(way);
		assertNull(l.called);
	}

	public void testAddAllSelectionListener() {
		DataSet ds2 = new DataSet();
		TestSelectionChangeListener l1 = new TestSelectionChangeListener();
		TestSelectionChangeListener l2 = new TestSelectionChangeListener();
		ds2.addSelectionChangedListener(l1);
		ds2.addSelectionChangedListener(l2);
		ds.addAllSelectionListener(ds2);
		ds2.removeSelectionChangedListener(l1);
		ds.setSelected(node2);
		assertNotNull(l1.called);
		assertNotNull(l2.called);
	}

	public void testRealEqual() {
		Collection<OsmPrimitive> all = new LinkedList<OsmPrimitive>();
		all.add(new Node(node1));
		all.add(new Node(node2));
		all.add(new Node(node3));
		all.add(createSegment(segment.id, node1, node2));
		all.add(createWay(way.id, way.segments.iterator().next()));
		assertTrue(ds.realEqual(all));
	}
}
