package org.openstreetmap.josm.io;

import java.io.IOException;
import java.io.Reader;
import java.util.Collection;
import java.util.StringTokenizer;

import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.openstreetmap.josm.data.GeoPoint;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.LineSegment;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.visitor.AddVisitor;

/**
 * Reads the old osm 0.2 format. 
 * 
 * @author imi
 */
public class OsmReaderOld {

	/**
	 * The data source from this reader.
	 */
	public Reader source;

	/**
	 * Construct a parser from a specific data source.
	 * @param source The data source, as example a FileReader to read from a file.
	 */
	public OsmReaderOld(Reader source) {
		this.source = source;
	}

	/**
	 * Read the input stream and return a DataSet from the stream.
	 */
	public DataSet parse() throws JDOMException, IOException {
		try {
			final SAXBuilder builder = new SAXBuilder();
			Element root = builder.build(source).getRootElement();
			return parseDataSet(root);
		} catch (NumberFormatException nfe) {
			throw new JDOMException("NumberFormatException. Probably a tag is missing.", nfe);
		} catch (NullPointerException npe) {
			throw new JDOMException("NullPointerException. Probably a tag name mismatch.", npe);
		} catch (ClassCastException cce) {
			throw new JDOMException("ClassCastException. Probably a tag does not contain the correct type.", cce);
		}
	}


	/**
	 * Read one node.
	 * @param e 	The element to parse
	 * @return		The Waypoint read from the element
	 * @throws JDOMException In case of a parsing error.
	 */
	private Node parseNode(Element e) throws JDOMException {
		Node data = new Node(new GeoPoint(
			Double.parseDouble(e.getAttributeValue("lat")),
			Double.parseDouble(e.getAttributeValue("lon"))));
		if (Double.isNaN(data.coor.lat) || 
				data.coor.lat < -90 || data.coor.lat > 90 ||
				data.coor.lon < -180 || data.coor.lon > 180)
			throw new JDOMException("Illegal lat or lon value: "+data.coor.lat+"/"+data.coor.lon);
		parseCommon(data, e);
		return data;
	}

	/**
	 * Parse any (yet unknown) object and return it.
	 */
	private OsmPrimitive parseObject(Element e, DataSet data) throws JDOMException {
		if (e.getName().equals("node"))
			return parseNode(e);
		else if (e.getName().equals("segment"))
			return parseLineSegment(e, data);
		else if (e.getName().equals("way"))
			return parseWay(e, data);
		else if (e.getName().equals("property")) {
			parseProperty(e, data);
			return null;
		}
		throw new JDOMException("unknown tag: "+e.getName());
	}
	
	/**
	 * Read a data set from the element.
	 * @param e 	The element to parse
	 * @return		The DataSet read from the element
	 * @throws JDOMException In case of a parsing error.
	 */
	private DataSet parseDataSet(Element e) throws JDOMException {
		DataSet data = new DataSet();
		AddVisitor visitor = new AddVisitor(data);
		for (Object o : e.getChildren()) {
			Element child = (Element)o;
			OsmPrimitive osm = parseObject(child, data);
			if (osm != null)
				osm.visit(visitor);
		}
		
		// clear all negative ids (new to this file)
		for (OsmPrimitive osm : data.allPrimitives())
			if (osm.id < 0)
				osm.id = 0;

		return data;
	}

	/**
	 * Parse and return an line segment. The node information of the "from" and
	 * "to" attributes must already be in the dataset.
	 * @param e		The line segment element to parse.
	 * @param data	The dataset to obtain the node information from.
	 * @return The parsed line segment.
	 * @throws JDOMException In case of parsing errors.
	 */
	private LineSegment parseLineSegment(Element e, DataSet data) throws JDOMException {
		long startId = Long.parseLong(e.getAttributeValue("from"));
		long endId = Long.parseLong(e.getAttributeValue("to"));
		
		Node start = null, end = null;
		for (Node n : data.nodes) {
			if (n.id == startId)
				start = n;
			if (n.id == endId)
				end = n;
		}
		if (start == null || end == null)
			throw new JDOMException("The 'from' or 'to' object has not been transfered before.");
		LineSegment ls = new LineSegment(start, end);
		parseCommon(ls, e);
		return ls;
	}

	/**
	 * Parse and read a way from the element.
	 *
	 * @param e		The element that contain the way.
	 * @param data	The DataSet to get segment information from.
	 * @return 		The parsed way.
	 * @throws JDOMException In case of a parsing error.
	 */
	private Way parseWay(Element e, DataSet data) throws JDOMException {
		Way way = new Way();
		parseCommon(way, e);
		for (Object o : e.getChildren("segment")) {
			Element child = (Element)o;
			long id = Long.parseLong(child.getAttributeValue("uid"));
			LineSegment ls = findLineSegment(data.lineSegments, id);
			way.segments.add(ls);
		}
		return way;
	}
	
	/**
	 * Parse the common part (properties and uid) of the element.
	 * @param data	To store the data in. 
	 * @param e		The element to extract the common information.
	 * @throws JDOMException In case of a parsing error
	 */
	private void parseCommon(OsmPrimitive data, Element e) {
		String suid = e.getAttributeValue("uid");
		if (suid != null)
			data.id = Long.parseLong(suid);
		
		String propStr = e.getAttributeValue("tags");
		if (propStr != null && !propStr.equals("")) {
			StringTokenizer st = new StringTokenizer(propStr, ";");
			while (st.hasMoreTokens()) {
				String next = st.nextToken();
				if (next.trim().equals(""))
					continue;
				int equalPos = next.indexOf('=');
				if (equalPos == -1)
					data.put(next, "");
				else {
					String keyStr = next.substring(0, equalPos);
					data.put(keyStr, next.substring(equalPos+1));
				}
			}
		}
		
		String action = e.getAttributeValue("action");
		if ("delete".equals(action))
			data.setDeleted(true);
		else if ("modify".equals(action))
			data.modified = data.modifiedProperties = true;
		else if ("modify/property".equals(action))
			data.modifiedProperties = true;
		else if ("modify/object".equals(action))
			data.modified = true;
	}

	/**
	 * Parse a property tag and assign the property to a previous found object.
	 */
	private void parseProperty(Element e, DataSet data) throws JDOMException {
		long id = Long.parseLong(e.getAttributeValue("uid"));
		OsmPrimitive osm = findObject(data, id);
		String key = e.getAttributeValue("key");
		String value = e.getAttributeValue("value");
		if (value != null)
			osm.put(key, value);
	}

	/**
	 * Search for an object in the dataset by comparing the id.
	 */
	private OsmPrimitive findObject(DataSet data, long id) throws JDOMException {
		for (OsmPrimitive osm : data.nodes)
			if (osm.id == id)
				return osm;
		for (OsmPrimitive osm : data.lineSegments)
			if (osm.id == id)
				return osm;
		for (OsmPrimitive osm : data.waies)
			if (osm.id == id)
				return osm;
		throw new JDOMException("Unknown object reference: "+id);
	}

	/**
	 * Search for a segment in a collection by comparing the id.
	 */
	private LineSegment findLineSegment(Collection<LineSegment> segments, long id) throws JDOMException {
		for (LineSegment ls : segments)
			if (ls.id == id)
				return ls;
		throw new JDOMException("Unknown line segment reference: "+id);
	}
}
