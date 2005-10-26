package org.openstreetmap.josm.io;

import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;

import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.Namespace;
import org.jdom.input.SAXBuilder;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.GeoPoint;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Key;
import org.openstreetmap.josm.data.osm.LineSegment;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Track;

/**
 * Reads an gpx stream and construct a DataSet out of it. 
 * Some information may not be imported, but GpxReader tries its best to load
 * all data possible in the key/value structure.
 * 
 * @author imi
 */
public class GpxReader {

	/**
	 * The GPX namespace used.
	 */
	public static final Namespace GPX = Namespace.getNamespace("http://www.topografix.com/GPX/1/0");
	/**
	 * The OSM namespace used (for extensions).
	 */
	private static final Namespace OSM = Namespace.getNamespace("osm");

	/**
	 * The data source from this reader.
	 */
	public Reader source;
	
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
		Node data = new Node();
		data.coor = new GeoPoint(
			Float.parseFloat(e.getAttributeValue("lat")),
			Float.parseFloat(e.getAttributeValue("lon")));
		
		for (Object o : e.getChildren()) {
			Element child = (Element)o;
			if (child.getName().equals("extensions"))
				parseKeyValueExtensions(data, child);
			else if (child.getName().equals("link"))
				parseKeyValueLink(data, child);
			else
				parseKeyValueTag(data, child);
		}
		return data;
	}

	/**
	 * Read a data set from the element.
	 * @param e 	The element to parse
	 * @return		The DataSet read from the element
	 */
	private DataSet parseDataSet(Element e) {
		DataSet data = new DataSet();
		// read waypoints not contained in tracks or areas
		for (Object o : e.getChildren("wpt", GPX))
			addNode(data, parseWaypoint((Element)o));
	
		// read tracks
		for (Object trackElement : e.getChildren("trk", GPX))
			parseTrack((Element)trackElement, data);
	
		return data;
	}

	/**
	 * Parse and read a track from the element. Store it in the dataSet, as well
	 * as all nodes in it.
	 *
	 * @param e		The element that contain the track.
	 * @param ds	The DataSet to store the data in.
	 */
	private void parseTrack(Element e, DataSet ds) {
		Track track = new Track();
		for (Object o : e.getChildren()) {
			Element child = (Element)o;

			if (child.getName().equals("trkseg")) {
				Node start = null;
				for (Object w : child.getChildren("trkpt", GPX)) {
					Node node = parseWaypoint((Element)w);
					node = addNode(ds, node);
					if (start == null)
						start = node;
					else {
						LineSegment lineSegment = new LineSegment(start, node);
						parseKeyValueExtensions(lineSegment, ((Element)w).getChild("extensions", GPX));
						track.add(lineSegment);
						start = null;
					}
				}
			}
			
			if (child.getName().equals("extensions"))
				parseKeyValueExtensions(track, child);
			else if (child.getName().equals("link"))
				parseKeyValueLink(track, child);
			else
				parseKeyValueTag(track, child);
		}
		ds.tracks.add(track);
	}
	

	/**
	 * Adds the node to allNodes if it is not already listed. Does respect the
	 * preference setting "mergeNodes". Return the node in the list that correspond
	 * to the node in the list (either the new added or the old found).
	 * 
	 * If reading raw gps data, mergeNodes are always on (To save memory. You
	 * can't edit raw gps nodes anyway.)
	 * 
	 * @param data The DataSet to add the node to.
	 * @param node The node that should be added.
	 * @return Either the parameter node or the old node found in the dataset. 
	 */
	private Node addNode (DataSet data, Node node) {
		if (Main.pref.mergeNodes)
			for (Node n : data.nodes)
				if (node.coor.equalsLatLon(n.coor))
					return n;
		data.nodes.add(node);
		return node;
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
			if (osm.keys == null)
				osm.keys = new HashMap<Key, String>();
			for (Object o : e.getChildren("property", OSM)) {
				Element child = (Element)o;
				Key key = Key.get(child.getAttributeValue("name"));
				osm.keys.put(key, child.getAttributeValue("value"));
			}
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
		if (e != null) {
			if (osm.keys == null)
				osm.keys = new HashMap<Key, String>();
			osm.keys.put(Key.get(e.getName()), e.getValue());
		}
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
			if (osm.keys == null)
				osm.keys = new HashMap<Key, String>();
			String link = e.getChildText("type") + ";" + e.getChildText("text");
			osm.keys.put(Key.get("link"), link);
		}
	}
}
