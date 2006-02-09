package org.openstreetmap.josm.test;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import junit.framework.TestCase;

import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.io.OsmWriter;
import org.openstreetmap.josm.test.framework.Bug;
import org.openstreetmap.josm.test.framework.DataSetTestCaseHelper;

/**
 * Test various problems with generation of OSM-XML
 * @author Imi
 */
public class OsmWriterTest extends TestCase {

	private DataSet ds;
	private Element osm;
	private List<Element> nodes;
	private List<Element> lineSegments;
	private List<Element> tracks;
	private List<Element> deleted;
	
	/**
	 * Test that the id generation creates unique ids and all are negative
	 */
	@SuppressWarnings("unchecked")
	public void testIDGenerationUniqueNegative() {
		Set<Long> ids = new HashSet<Long>();
		for (Element e : (List<Element>)osm.getChildren()) {
			long id = Long.parseLong(e.getAttributeValue("uid"));
			assertTrue("id "+id+" is negative", id < 0);
			ids.add(id);
		}
		assertEquals(nodes.size()+lineSegments.size()+tracks.size(), ids.size());
	}

	/**
	 * Verify that generated ids of higher level primitives point to the 
	 * generated lower level ids (tracks point to segments which point to nodes).
	 */
	@Bug(47)
	public void testIDGenerationReferences() {
		long id1 = Long.parseLong(getAttr(osm, "node", 0, "uid"));
		long id2 = Long.parseLong(getAttr(osm, "node", 1, "uid"));
		long lsFrom = Long.parseLong(getAttr(osm, "segment", 0, "from"));
		long lsTo = Long.parseLong(getAttr(osm, "segment", 0, "to"));
		assertEquals(id1, lsFrom);
		assertEquals(id2, lsTo);
		assertEquals(id2, lsTo);

		long ls1 = Long.parseLong(getAttr(osm, "segment", 0, "uid"));
		long ls2 = Long.parseLong(getAttr(osm, "segment", 1, "uid"));
		long t1 = Long.parseLong(getAttr(osm.getChild("track"), "segment", 0, "uid"));
		long t2 = Long.parseLong(getAttr(osm.getChild("track"), "segment", 1, "uid"));
		assertEquals(ls1, t1);
		assertEquals(ls2, t2);
	}

	/**
	 * Verify that deleted objects that are not uploaded to the server does not show up
	 * in xml save output at all.
	 */
	@Bug(47)
	public void testDeleteNewDoesReallyRemove() throws IOException, JDOMException {
		ds.tracks.iterator().next().setDeleted(true);
		reparse();
		assertEquals(0, deleted.size());
	}

	
	
	
	
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		// create some data
		ds = DataSetTestCaseHelper.createCommon();
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
		StringWriter out = new StringWriter();

		OsmWriter osmWriter = new OsmWriter(out, ds);
		osmWriter.output();
		
		// reparse
		osm = new SAXBuilder().build(new StringReader(out.toString())).getRootElement();
		nodes = osm.getChildren("node");
		lineSegments = osm.getChildren("segment");
		tracks = osm.getChildren("track");
		deleted = osm.getChildren("deleted");
	}
}
