package org.openstreetmap.josm.test;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;

import junit.framework.TestCase;

import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.Namespace;
import org.jdom.input.SAXBuilder;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.io.GpxWriter;
import org.openstreetmap.josm.test.framework.Bug;
import org.openstreetmap.josm.test.framework.DataSetTestCaseHelper;

public class GpxWriterTest extends TestCase {

	private static Namespace GPX = Namespace.getNamespace("http://www.topografix.com/GPX/1/0");
	private static Namespace JOSM = Namespace.getNamespace("http://wiki.eigenheimstrasse.de/wiki/JOSM");

	private DataSet ds;

	private Element root;

	/**
	 * Verify that deleted objects that are not uploaded to the server does not show up
	 * in gpx save output at all.
	 */
	@Bug(47)
	public void testDeleteNewDoesReallyRemove() throws JDOMException, IOException {
		ds.waies.iterator().next().setDeleted(true);
		root = reparse();
		assertEquals("way has vanished and 3 trk (segments) left", 3, root.getChildren("trk", GPX).size());
	}

	
	/**
	 * Verify, that new created elements, if and only if they occoure more than once in
	 * the file, have a negative id attached.
	 */
	@Bug(47)
	public void testNewCreateAddIdWhenMoreThanOnce() {
		// the trk with the two trkseg's only occoure once -> no extension id
		Element realWay = null;
		for (Object o : root.getChildren("trk", GPX)) {
			Element e = (Element)o;
			if (e.getChildren("trkseg", GPX).size() != 2)
				continue;
			Element ext = e.getChild("extensions", GPX);
			if (ext != null)
				assertEquals("no id for way (used only once)", 0, ext.getChildren("uid", JOSM).size());
			realWay = e;
		}
		assertNotNull("way not found in GPX file", realWay);
		
		// the second point of the first segment of the waies has an id
		Element trkseg = (Element)realWay.getChildren("trkseg", GPX).get(0);
		Element trkpt = (Element)trkseg.getChildren("trkpt", GPX).get(1);
		assertEquals("waypoint used twice but has no extensions at all", 1, trkpt.getChildren("extensions", GPX).size());
		Element ext = trkpt.getChild("extensions", GPX);
		assertEquals("waypoint used twice but has no id", 1, ext.getChildren("uid", JOSM).size());
	}


	/**
	 * Parse the intern dataset and return the root gpx - element.
	 */
	private Element reparse() throws IOException, JDOMException {
		StringWriter out = new StringWriter();
		GpxWriter writer = new GpxWriter(out, ds);
		writer.output();
		Element root = new SAXBuilder().build(new StringReader(out.toString())).getRootElement();
		return root;
	}


	@Override
	protected void setUp() throws Exception {
		super.setUp();
		ds = DataSetTestCaseHelper.createCommon();
		root = reparse();
	}
}
