package org.openstreetmap.josm.io;

import static org.openstreetmap.josm.io.GpxWriter.GPX;
import static org.openstreetmap.josm.io.GpxWriter.JOSM;
import static org.openstreetmap.josm.io.GpxWriter.OSM;

import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;

import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.openstreetmap.josm.data.GeoPoint;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.LineSegment;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Way;

/**
 * Reads an gpx stream and construct a DataSet out of it. 
 * Some information may not be imported, but GpxReader tries its best to load
 * all data possible in the key/value structure.
 * 
 * @author imi
 */
public class GpxReader {

	/**
	 * The data source from this reader.
	 */
	public Reader source;
	/**
	 * Mapper to find new created objects that occoure more than once.
	 */
	private HashMap<Long, OsmPrimitive> newCreatedPrimitives = new HashMap<Long, OsmPrimitive>();
	/**
	 * Either set to true or false, depending whether the JOSM namespace declaration 
	 * was found.
	 */
	private boolean mergeNodes = false;

	/**
	 * Construct a parser from a specific data source.
	 * @param source The data source, as example a FileReader to read from a file.
	 */
	public GpxReader(Reader source) {
		this.source = source;
	}
	
	/**
	 * Read the input stream and return a DataSet from the stream.
	 */
	public DataSet parse() throws JDOMException, IOException {
		try {
			final SAXBuilder builder = new SAXBuilder();
			Element root = builder.build(source).getRootElement();
			mergeNodes = !root.getAdditionalNamespaces().contains(JOSM);
			return parseDataSet(root);
		} catch (NullPointerException npe) {
			throw new JDOMException("NullPointerException. Probably a tag name mismatch.", npe);
		} catch (ClassCastException cce) {
			throw new JDOMException("ClassCastException. Probably a tag does not contain the correct type.", cce);
		}
	}


	/**
	 * Read one node (waypoint).
	 * @param e 	The element to parse
	 * @return		The Waypoint read from the element
	 */
	private Node parseWaypoint(Element e) {
		Node data = new Node(new GeoPoint(
			Double.parseDouble(e.getAttributeValue("lat")),
			Double.parseDouble(e.getAttributeValue("lon"))));
		
		for (Object o : e.getChildren()) {
			Element child = (Element)o;
			if (child.getName().equals("extensions"))
				parseKeyValueExtensions(data, child);
			else if (child.getName().equals("link"))
				parseKeyValueLink(data, child);
			else
				parseKeyValueTag(data, child);
		}
		data = (Node)getNewIfSeenBefore(data);
		return data;
	}

	/**
	 * Read a data set from the element.
	 * @param e 	The element to parse
	 * @return		The DataSet read from the element
	 */
	private DataSet parseDataSet(Element e) {
		DataSet data = new DataSet();
		// read waypoints not contained in waies or areas
		for (Object o : e.getChildren("wpt", GPX)) {
			Node node = parseWaypoint((Element)o);
			addNode(data, node);
		}
	
		// read waies (and line segments)
		for (Object wayElement : e.getChildren("trk", GPX))
			parseWay((Element)wayElement, data);

		// reset new created ids to zero
		for (OsmPrimitive osm : data.allPrimitives())
			if (osm.id < 0)
				osm.id = 0;
		
		return data;
	}

	/**
	 * Parse and read a way from the element. Store it in the dataSet, as well
	 * as all nodes in it.
	 *
	 * @param e		The element that contain the way.
	 * @param ds	The DataSet to store the data in.
	 */
	private void parseWay(Element e, DataSet ds) {
		Way way = new Way();
		boolean realLineSegment = false; // is this way just a fake?

		for (Object o : e.getChildren()) {
			Element child = (Element)o;

			if (child.getName().equals("trkseg")) {
				Node start = null;
				for (Object w : child.getChildren("trkpt", GPX)) {
					Node node = addNode(ds, parseWaypoint((Element)w));
					if (start == null)
						start = node;
					else {
						LineSegment lineSegment = new LineSegment(start, node);
						parseKeyValueExtensions(lineSegment, child.getChild("extensions", GPX));
						lineSegment = (LineSegment)getNewIfSeenBefore(lineSegment);
						way.segments.add(lineSegment);
						start = node;
					}
				}
			} else if (child.getName().equals("extensions")) {
				parseKeyValueExtensions(way, child);
				if (child.getChild("segment", JOSM) != null)
					realLineSegment = true;
			} else if (child.getName().equals("link"))
				parseKeyValueLink(way, child);
			else
				parseKeyValueTag(way, child);
		}
		way = (Way)getNewIfSeenBefore(way);
		ds.lineSegments.addAll(way.segments);
		if (!realLineSegment)
			ds.waies.add(way);
	}

	/**
	 * Adds the node to allNodes if it is not already listed. Does respect the
	 * setting "mergeNodes". Return the node in the list that correspond
	 * to the node in the list (either the new added or the old found).
	 * 
	 * @param data The DataSet to add the node to.
	 * @param node The node that should be added.
	 * @return Either the parameter node or the old node found in the dataset. 
	 */
	private Node addNode(DataSet data, Node node) {
		if (mergeNodes)
			for (Node n : data.nodes)
				if (node.coor.equalsLatLon(n.coor))
					return n;
		data.nodes.add(node);
		return node;
	}
	

	/**
	 * @return Either the parameter or an index from the newCreatedPrimitives map
	 * 		with the id seen before.
	 */
	private OsmPrimitive getNewIfSeenBefore(OsmPrimitive osm) {
		if (newCreatedPrimitives.containsKey(osm.id))
			return newCreatedPrimitives.get(osm.id);
		return osm;
	}

	/**
	 * Parse the extensions tag and add all properties found as key/value. 
	 * <code>osm.keys</code> may be <code>null</code>, in which case it is 
	 * created first. If <code>e</code> is <code>null</code>, nothing
	 * happens.
	 * 
	 * @param osm	The primitive to store the properties.
	 * @param e		The extensions element to read the properties from.
	 */
	private void parseKeyValueExtensions(OsmPrimitive osm, Element e) {
		if (e != null) {
			for (Object o : e.getChildren("property", OSM)) {
				Element child = (Element)o;
				String keyname = child.getAttributeValue("key");
				if (keyname != null) {
					String value = child.getAttributeValue("value");
					if (value == null)
						value = "";
					osm.put(keyname, value);
				}
			}
			Element idElement = e.getChild("uid", JOSM);
			if (idElement != null)
				osm.id = Long.parseLong(idElement.getText());
			if (osm.id < 0 && !newCreatedPrimitives.containsKey(osm.id))
				newCreatedPrimitives.put(osm.id, osm);
			osm.modified = e.getChild("modified", JOSM) != null;
			osm.setDeleted(e.getChild("deleted", JOSM) != null);
			osm.modifiedProperties = e.getChild("modifiedProperties", JOSM) != null;
		}
	}

	/**
	 * If the element is not <code>null</code>, read the data from it and put
	 * it as the key with the name of the elements name in the given object.
	 * 
	 * The <code>keys</code> - field of the element could be <code>null</code>,
	 * in which case it is created first.
	 * 
	 * @param osm     The osm primitive to put the key into.
	 * @param e		  The element to look for data.
	 */
	private void parseKeyValueTag(OsmPrimitive osm, Element e) {
		if (e != null)
			osm.put(e.getName(), e.getValue());
	}

	/**
	 * Parse the GPX linkType data information and store it as value in the 
	 * primitives <i>link</i> key. <code>osm.keys</code> may be 
	 * <code>null</code>, in which case it is created first. If 
	 * <code>e</code> is <code>null</code>, nothing happens.
	 * 
	 * The format stored is: mimetype;url
	 * Example: text/html;http://www.openstreetmap.org
	 * 
	 * @param osm	The osm primitive to store the data in.
	 * @param e		The element in gpx:linkType - format.
	 */
	private void parseKeyValueLink(OsmPrimitive osm, Element e) {
		if (e != null) {
			String link = e.getChildText("type") + ";" + e.getChildText("text");
			osm.put("link", link);
		}
	}
}
