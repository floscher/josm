package org.openstreetmap.josm.test;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import junit.framework.TestCase;

import org.jdom.Attribute;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Key;
import org.openstreetmap.josm.data.osm.LineSegment;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Track;
import org.openstreetmap.josm.io.OsmWriter;
import org.openstreetmap.josm.test.framework.Bug;
import org.openstreetmap.josm.test.framework.DataSetTestCaseHelper;

/**
 * Test various problems with generation of OSM-XML
 * @author Imi
 */
public class OsmWriterTest extends TestCase {

	private Node n1;
	private Node n2;
	private Node n3;
	private Node n4;
	private Node n5;
	private LineSegment ls1;
	private LineSegment ls2;
	private LineSegment ls3;
	private Track w;
	
	private DataSet ds;
	private Element osm;
	private List<Element> nodes;
	private List<Element> lineSegments;
	private List<Element> ways;
	private StringWriter out;
	

	public void testNode() throws Exception {
		ds = new DataSet();
		Node n = DataSetTestCaseHelper.createNode(ds);
		n.id = 42;
		reparse();
		assertEquals(42, Long.parseLong(getAttr(osm, "node", 0, "id")));
		assertEquals(n.coor.lat, Double.parseDouble(getAttr(osm, "node", 0, "lat")));
		assertEquals(n.coor.lon, Double.parseDouble(getAttr(osm, "node", 0, "lon")));
	}

	@Bug(59)
	public void testSpecialChars() throws Exception {
		StringBuilder sb = new StringBuilder();
		for (int i = 32; i < 0xd800; ++i)
			sb.append((char)i);
		String s = sb.toString();
		n1.put(Key.get(s), s);
		reparse();
		assertEquals(1, nodes.get(0).getChildren().size());
		Attribute key = ((Element)nodes.get(0).getChildren().get(0)).getAttribute("k");
		assertEquals(s, key.getValue());
		Attribute value = ((Element)nodes.get(0).getChildren().get(0)).getAttribute("v");
		assertEquals(s, value.getValue());
	}
	
	public void testLineSegment() throws Exception {
		ds = new DataSet();
		LineSegment ls = DataSetTestCaseHelper.createLineSegment(ds, DataSetTestCaseHelper.createNode(ds), DataSetTestCaseHelper.createNode(ds));
		ls.put(Key.get("foo"), "bar");
		reparse();
		assertEquals(1, lineSegments.size());
		assertEquals("foo", getAttr(osm.getChild("segment"), "tag", 0, "k"));
		assertEquals("bar", getAttr(osm.getChild("segment"), "tag", 0, "v"));
	}
	
	
	/**
	 * Test that the id generation creates unique ids and all are negative
	 */
	@SuppressWarnings("unchecked")
	public void testIDGenerationUniqueNegative() {
		Set<Long> ids = new HashSet<Long>();
		for (Element e : (List<Element>)osm.getChildren()) {
			long id = Long.parseLong(e.getAttributeValue("id"));
			assertTrue("id "+id+" is negative", id < 0);
			ids.add(id);
		}
		assertEquals(nodes.size()+lineSegments.size()+ways.size(), ids.size());
	}

	/**
	 * Verify that generated ids of higher level primitives point to the 
	 * generated lower level ids (ways point to segments which point to nodes).
	 */
	@Bug(47)
	public void testIDGenerationReferences() {
		long id1 = Long.parseLong(getAttr(osm, "node", 0, "id"));
		long id2 = Long.parseLong(getAttr(osm, "node", 1, "id"));
		long lsFrom = Long.parseLong(getAttr(osm, "segment", 0, "from"));
		long lsTo = Long.parseLong(getAttr(osm, "segment", 0, "to"));
		assertEquals(id1, lsFrom);
		assertEquals(id2, lsTo);
		assertEquals(id2, lsTo);

		long ls1 = Long.parseLong(getAttr(osm, "segment", 0, "id"));
		long ls2 = Long.parseLong(getAttr(osm, "segment", 1, "id"));
		long t1 = Long.parseLong(getAttr(osm.getChild("way"), "seg", 0, "id"));
		long t2 = Long.parseLong(getAttr(osm.getChild("way"), "seg", 1, "id"));
		assertEquals(ls1, t1);
		assertEquals(ls2, t2);
	}

	/**
	 * Verify that deleted objects that are not uploaded to the server does not show up
	 * in xml save output at all.
	 */
	@Bug(47)
	public void testDeleteNewDoesReallyRemove() throws Exception {
		ds.tracks.iterator().next().setDeleted(true);
		reparse();
		//assertEquals(0, deleted.size());
	}

	
	/**
	 * Verify that action tag is set correctly.
	 */
	public void testActionTag() throws Exception {
		int id = 1;
		for (OsmPrimitive osm : ds.allPrimitives())
			osm.id = id++; // make all objects "old".
		n1.setDeleted(true);
		ls1.modified = true;
		ls1.modifiedProperties = true;
		ls3.modified = true;
		w.modifiedProperties = true;
		reparse();
		
		boolean foundNode = false;
		for (Element n : nodes) {
			if (n.getAttributeValue("id").equals(""+n1.id)) {
				assertEquals("delete", n.getAttributeValue("action"));
				foundNode = true;
			}
		}
		assertTrue("Node found in output", foundNode);

		boolean foundLs1 = false;
		boolean foundLs3 = false;
		for (Element lsElem : lineSegments) {
			String idStr = lsElem.getAttributeValue("id");
			String action = lsElem.getAttributeValue("action");
			if (idStr.equals(""+ls1.id)) {
				assertEquals("Attribute action on modified data is ok", "modify", action);
				foundLs1 = true;
			} else if (idStr.equals(""+ls3.id)) {
				assertEquals("Attribute action on modified/object data is ok", "modify/object", action);
				foundLs3 = true;
			}
		}
		assertTrue("LineSegments found in output", foundLs1 && foundLs3);
		
		assertEquals("Way found in output", 1, ways.size());
		assertEquals("Attribute action on modifiedProperty data is ok", "modify/property", ways.get(0).getAttributeValue("action"));
	}

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		
		// create some data
		ds = new DataSet();
		n1 = DataSetTestCaseHelper.createNode(ds);
		n2 = DataSetTestCaseHelper.createNode(ds);
		n3 = DataSetTestCaseHelper.createNode(ds);
		n4 = DataSetTestCaseHelper.createNode(ds);
		n5 = DataSetTestCaseHelper.createNode(ds);
		ls1 = DataSetTestCaseHelper.createLineSegment(ds, n1, n2);
		ls2 = DataSetTestCaseHelper.createLineSegment(ds, n2, n3);
		ls3 = DataSetTestCaseHelper.createLineSegment(ds, n4, n5);
		w = DataSetTestCaseHelper.createWay(ds, ls1, ls2);
		
		reparse();
	}

	/**
	 * Get an attribute out of an object of the root element.
	 */
	private String getAttr(Element root, String objName, int objPos, String attrName) {
		Element e = (Element)root.getChildren(objName).get(objPos);
		return e.getAttributeValue(attrName);
	}

	/**
	 * Reparse the dataset into the lists members..
	 */
	@SuppressWarnings("unchecked")
	private void reparse() throws IOException, JDOMException {
		out = new StringWriter();
		OsmWriter.output(out, ds, false);
		
		// reparse
		osm = new SAXBuilder().build(new StringReader(out.toString())).getRootElement();
		nodes = osm.getChildren("node");
		lineSegments = osm.getChildren("segment");
		ways = osm.getChildren("way");
	}
}
