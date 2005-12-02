package org.openstreetmap.josm.io;

import java.io.IOException;
import java.io.Reader;
import java.util.Collection;
import java.util.HashMap;
import java.util.StringTokenizer;

import org.jdom.Element;
import org.jdom.JDOMException;
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
 * Reads an osm xml stream and construct a DataSet out of it. 
 * 
 * @author imi
 */
public class OsmReader {

	/**
	 * The data source from this reader.
	 */
	public Reader source;

	/**
	 * Construct a parser from a specific data source.
	 * @param source The data source, as example a FileReader to read from a file.
	 */
	public OsmReader(Reader source) {
		this.source = source;
	}
	static int i;
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
		Node data = new Node();
		data.coor = new GeoPoint(
			Float.parseFloat(e.getAttributeValue("lat")),
			Float.parseFloat(e.getAttributeValue("lon")));
		if (Double.isNaN(data.coor.lat) || 
				data.coor.lat < -90 || data.coor.lat > 90 ||
				data.coor.lon < -180 || data.coor.lon > 180)
			throw new JDOMException("Illegal lat or lon value: "+data.coor.lat+"/"+data.coor.lon);
		parseCommon(data, e);
		return data;
	}

	/**
	 * Parse the common part (properties and uid) of the element.
	 * @param data	To store the data in. 
	 * @param e		The element to extract the common information.
	 * @throws JDOMException In case of a parsing error
	 */
	private void parseCommon(OsmPrimitive data, Element e) throws JDOMException {
		data.id = Long.parseLong(e.getAttributeValue("uid"));
		if (data.id == 0)
			throw new JDOMException("Object has illegal or no id.");
		
		String propStr = e.getAttributeValue("tags");
		if (propStr != null && !propStr.equals("")) {
			data.keys = new HashMap<Key, String>();
			StringTokenizer st = new StringTokenizer(propStr, ";");
			while (st.hasMoreTokens()) {
				StringTokenizer t = new StringTokenizer(st.nextToken(), "=");
				if (t.countTokens() > 1)
					data.keys.put(Key.get(t.nextToken()), t.nextToken());
				else {
					String token = t.nextToken();
					if (!" ".equals(token))
						data.keys.put(Key.get(token), "");
				}
			}
		}
	}

	/**
	 * Read a data set from the element.
	 * @param e 	The element to parse
	 * @return		The DataSet read from the element
	 * @throws JDOMException In case of a parsing error.
	 */
	private DataSet parseDataSet(Element e) throws JDOMException {
		DataSet data = new DataSet();
		for (Object o : e.getChildren()) {
			Element child = (Element)o;
			if (child.getName().equals("node"))
				addNode(data, parseNode(child));
			else if (child.getName().equals("segment")) {
				LineSegment ls = parseLineSegment(child, data);
				if (data.pendingLineSegments.contains(ls))
					throw new JDOMException("Double segment definition "+ls.id);
				for (Track t : data.tracks)
					if (t.segments.contains(ls))
						throw new JDOMException("Double segment definition "+ls.id);
				data.pendingLineSegments.add(ls);
			} else if (child.getName().equals("track")) {
				Track track = parseTrack(child, data);
				if (data.tracks.contains(track))
					throw new JDOMException("Double track definition "+track.id);
				data.tracks.add(track);
			}
		}

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
	 * Parse and read a track from the element.
	 *
	 * @param e		The element that contain the track.
	 * @param data	The DataSet to get segment information from.
	 * @return 		The parsed track.
	 * @throws JDOMException In case of a parsing error.
	 */
	private Track parseTrack(Element e, DataSet data) throws JDOMException {
		Track track = new Track();
		parseCommon(track, e);
		for (Object o : e.getChildren("segment")) {
			Element child = (Element)o;
			long id = Long.parseLong(child.getAttributeValue("uid"));
			LineSegment ls = findLineSegment(data.pendingLineSegments, id);
			if (ls != null) {
				track.segments.add(ls);
				data.pendingLineSegments.remove(ls);
				continue;
			}
			for (Track t : data.tracks) {
				ls = findLineSegment(t.segments, id);
				if (ls != null) {
					track.segments.add(ls);
					break;
				}
			}
		}
		return track;
	}
	
	/**
	 * Search for a segment in a collection by comparing the id.
	 */
	private LineSegment findLineSegment(Collection<LineSegment> segments, long id) {
		for (LineSegment ls : segments)
			if (ls.id == id)
				return ls;
		return null;
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
	private Node addNode(DataSet data, Node node) {
		if (Main.pref.mergeNodes)
			for (Node n : data.nodes)
				if (node.coor.equalsLatLon(n.coor))
					return n;
		data.nodes.add(node);
		return node;
	}
}
