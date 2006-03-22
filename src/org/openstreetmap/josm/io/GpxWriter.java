package org.openstreetmap.josm.io;

import java.io.IOException;
import java.io.Writer;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.Map.Entry;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.Namespace;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.LineSegment;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Way;

/**
 * Exports a dataset to GPX data. All information available are tried to store in
 * the gpx. If no corresponding tag is available in GPX, use 
 * <code>&lt;extensions&gt;</code> instead.
 * 
 * GPX-Way segments are stored as 2-node-pairs, so no &lt;trkseg&gt; with more
 * or less than 2 &lt;trkpt&gt; are exported.
 * 
 * @author imi
 */
public class GpxWriter {

	/**
	 * The GPX namespace used.
	 */
	public static final Namespace GPX = Namespace.getNamespace("http://www.topografix.com/GPX/1/0");
	/**
	 * The OSM namespace used (for extensions).
	 */
	public static final Namespace OSM = Namespace.getNamespace("osm", "http://www.openstreetmap.org");
	/**
	 * The JOSM namespace (for JOSM-extensions).
	 */
	public static final Namespace JOSM = Namespace.getNamespace("josm", "http://wiki.eigenheimstrasse.de/wiki/JOSM");

	/**
	 * This is the output writer to store the resulting data in.
	 */
	private Writer out;
	/**
	 * The dataset beeing processed.
	 */
	private DataSet ds;
	/**
	 * Map all new primitives to the element which hold them. After inserting everything,
	 * the writer sets ids to every element that was in the list and had more than one
	 * element using it. 
	 */
	private HashMap<OsmPrimitive, Collection<Element>> usedNewPrimitives = new HashMap<OsmPrimitive, Collection<Element>>();
	/**
	 * The counter for new created objects used more than once.
	 * Starting at -1 and goes down.
	 */
	private long newIdCounter = -1;
	
	/**
	 * Create a GpxWrite from an output writer. As example to write in a file,
	 * use FileWriter.
	 *
	 * @param out The Writer to store the result data in.
	 */
	public GpxWriter(Writer out, DataSet ds) {
		this.out = out;
		this.ds = ds;
	}


	/**
	 * Do the output in the former set writer.
	 * @exception IOException In case of IO errors, throw this exception.
	 */
	public void output() throws IOException {
		Element root = parseDataSet();
		root.addNamespaceDeclaration(OSM);
		root.addNamespaceDeclaration(JOSM);
		Document d = new Document(root);
		XMLOutputter xmlOut = new XMLOutputter(Format.getPrettyFormat());
		xmlOut.output(d, out);
	}


	/**
	 * Write the whole DataSet in an JDom-Element and return the new element.
	 * @return The created element, out of the dataset.
	 */
	@SuppressWarnings("unchecked")
	private Element parseDataSet() {
		Element e = new Element("gpx", GPX);
		e.setAttribute("version", "1.0");
		e.setAttribute("creator", "JOSM");
		// for getting all unreferenced waypoints in the wpt-list
		LinkedList<Node> unrefNodes = new LinkedList<Node>(ds.nodes);
		// for getting all unreferenced line segments
		LinkedList<LineSegment> unrefLs = new LinkedList<LineSegment>(ds.lineSegments);

		// waies
		for (Way t : ds.waies) {
			if (t.isDeleted() && t.id == 0)
				continue;
			Element tElem = new Element("trk", GPX);
			HashMap<String, String> keys = null;
			if (t.keys != null) {
				keys = new HashMap<String, String>(t.keys);
				addAndRemovePropertyTag("name", tElem, keys);
				addAndRemovePropertyTag("cmt", tElem, keys);
				addAndRemovePropertyTag("desc", tElem, keys);
				addAndRemovePropertyTag("src", tElem, keys);
				addAndRemovePropertyLinkTag(tElem, keys);
				addAndRemovePropertyTag("number", tElem, keys);
				addAndRemovePropertyTag("type", tElem, keys);
			}
			addPropertyExtensions(tElem, keys, t);

			// line segments
			for (LineSegment ls : t.segments) {
				tElem.getChildren().add(parseLineSegment(ls));
				unrefNodes.remove(ls.from);
				unrefNodes.remove(ls.to);
				unrefLs.remove(ls);
			}

			e.getChildren().add(tElem);
		}
		
		// encode pending line segments as waies
		for (LineSegment ls : unrefLs) {
			if (ls.isDeleted() && ls.id == 0)
				continue;
			Element t = new Element("trk", GPX);
			t.getChildren().add(parseLineSegment(ls));
			unrefNodes.remove(ls.from);
			unrefNodes.remove(ls.to);
			Element ext = new Element("extensions", GPX);
			ext.getChildren().add(new Element("segment", JOSM));
			t.getChildren().add(ext);
			e.getChildren().add(t);
		}

		// waypoints (missing nodes)
		for (Node n : unrefNodes) {
			if (n.isDeleted() && n.id == 0)
				continue;
			e.getChildren().add(parseWaypoint(n, "wpt"));
		}

		// link all ids used more than once
		for (Entry<OsmPrimitive, Collection<Element>> entry : usedNewPrimitives.entrySet()) {
			if (entry.getValue().size() > 1) {
				long id = newIdCounter--;
				for (Element element : entry.getValue()) {
					Element ext = element.getChild("extensions", GPX);
					if (ext == null)
						element.getChildren().add(ext = new Element("extensions", GPX));
					ext.getChildren().add(new Element("uid", JOSM).setText(""+id));
				}
			}
		}

		return e;
	}


	/**
	 * Parse a line segment and store it into a JDOM-Element. Return that element.
	 */
	@SuppressWarnings("unchecked")
	private Element parseLineSegment(LineSegment ls) {
		Element lsElem = new Element("trkseg", GPX);
		addPropertyExtensions(lsElem, ls.keys, ls);
		lsElem.getChildren().add(parseWaypoint(ls.from, "trkpt"));
		lsElem.getChildren().add(parseWaypoint(ls.to, "trkpt"));
		return lsElem;
	}

	/**
	 * Parse a waypoint (node) and store it into an JDOM-Element. Return that
	 * element.
	 * 
	 * @param n The Node to parse and store
	 * @param name The name of the tag (different names for nodes in GPX)
	 * @return The resulting GPX-Element
	 */
	private Element parseWaypoint(Node n, String name) {
		Element e = new Element(name, GPX);
		e.setAttribute("lat", Double.toString(n.coor.lat));
		e.setAttribute("lon", Double.toString(n.coor.lon));
		HashMap<String, String> keys = null;
		if (n.keys != null) {
			keys = new HashMap<String, String>(n.keys);
			addAndRemovePropertyTag("ele", e, keys);
			addAndRemovePropertyTag("time", e, keys);
			addAndRemovePropertyTag("magvar", e, keys);
			addAndRemovePropertyTag("geoidheight", e, keys);
			addAndRemovePropertyTag("name", e, keys);
			addAndRemovePropertyTag("cmt", e, keys);
			addAndRemovePropertyTag("desc", e, keys);
			addAndRemovePropertyTag("src", e, keys);
			addAndRemovePropertyLinkTag(e, keys);
			addAndRemovePropertyTag("sym", e, keys);
			addAndRemovePropertyTag("type", e, keys);
			addAndRemovePropertyTag("fix", e, keys);
			addAndRemovePropertyTag("sat", e, keys);
			addAndRemovePropertyTag("hdop", e, keys);
			addAndRemovePropertyTag("vdop", e, keys);
			addAndRemovePropertyTag("pdop", e, keys);
			addAndRemovePropertyTag("ageofdgpsdata", e, keys);
			addAndRemovePropertyTag("dgpsid", e, keys);
		}
		addPropertyExtensions(e, keys, n);
		return e;
	}


	/**
	 * Add a link-tag to the element, if the property list contain a value named 
	 * "link". The property is removed from the map afterwards.
	 * 
	 * For the format, @see GpxReader#parseKeyValueLink(OsmPrimitive, Element).
	 * @param e		The element to add the link to.
	 * @param keys	The map containing the link property.
	 */
	@SuppressWarnings("unchecked")
	private void addAndRemovePropertyLinkTag(Element e, Map<String, String> keys) {
		String value = keys.get("link");
		if (value != null) {
			StringTokenizer st = new StringTokenizer(value, ";");
			if (st.countTokens() != 2)
				return;
			Element link = new Element("link", GPX);
			link.getChildren().add(new Element("type", GPX).setText(st.nextToken()));
			link.getChildren().add(0,new Element("text", GPX).setText(st.nextToken()));
			e.getChildren().add(link);
			keys.remove("link");
		}
	}


	/**
	 * Helper to add a property with a given name as tag to the element. This
	 * will look like &lt;name&gt;<i>keys.get(name)</i>&lt;/name&gt; 
	 * 
	 * After adding, the property is removed from the map.
	 * 
	 * If the property does not exist, nothing is done.
	 * 
	 * @param name The properties name
	 * @param e The element to add the tag to.
	 * @param osm The data to get the property from.
	 */
	@SuppressWarnings("unchecked")
	private void addAndRemovePropertyTag(String name, Element e, Map<String, String> keys) {
		String value = keys.get(name);
		if (value != null) {
			e.getChildren().add(new Element(name, GPX).setText(value));
			keys.remove(name);
		}
	}
	
	/**
	 * Add the property in the entry as &lt;extensions&gt; to the element 
	 * @param e		The element to add the property to.
	 * @param prop	The property to add.
	 */
	@SuppressWarnings("unchecked")
	private void addPropertyExtensions(Element e, Map<String, String> keys, OsmPrimitive osm) {
		LinkedList<Element> extensions = new LinkedList<Element>();
		if (keys != null && !keys.isEmpty()) {
			for (Entry<String, String> prop : keys.entrySet()) {
				Element propElement = new Element("property", OSM);
				propElement.setAttribute("key", prop.getKey());
				propElement.setAttribute("value", prop.getValue());
				extensions.add(propElement);
			}
		}
		
		if (osm.id != 0) {
			Element propElement = new Element("uid", JOSM);
			propElement.setText(""+osm.id);
			extensions.add(propElement);
		} else {
			Collection<Element> l = usedNewPrimitives.get(osm);
			if (l == null)
				l = new LinkedList<Element>();
			l.add(e);
			usedNewPrimitives.put(osm, l);
		}
		if (osm.modified) {
			Element modElement = new Element("modified", JOSM);
			extensions.add(modElement);
		}
		if (osm.isDeleted()) {
			Element modElement = new Element("deleted", JOSM);
			extensions.add(modElement);
		}
		if (osm.modifiedProperties) {
			Element modElement = new Element("modifiedProperties", JOSM);
			extensions.add(modElement);
		}
		
		if (extensions.isEmpty())
			return;

		Element ext = e.getChild("extensions", GPX);
		if (ext == null)
			e.getChildren().add(ext = new Element("extensions", GPX));
		ext.getChildren().addAll(extensions);
	}
}
