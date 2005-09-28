package org.openstreetmap.josm.io;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.LinkedList;

import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.Namespace;
import org.jdom.input.SAXBuilder;
import org.openstreetmap.josm.data.GeoPoint;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.Track;
import org.openstreetmap.josm.data.osm.LineSegment;

/**
 * Reads an gpx stream and construct a DataSet out of it. Some information may not be 
 * imported, since JOSM does not fully support GPX.
 * 
 * @author imi
 */
public class GpxReader {

	public static final Namespace XSD = Namespace.getNamespace("http://www.w3.org/2001/XMLSchema");
	public static final Namespace GPX = Namespace.getNamespace("http://www.topografix.com/GPX/1/0");
	
	/**
	 * Read the input stream and return a DataSet from the stream.
	 * 
	 * @param in
	 * @throws IOException 		An error with the provided stream occoured.
	 * @throws JDOMException 	An parse error occoured.
	 */
	public DataSet parse(Reader in) throws JDOMException, IOException {
		try {
			final SAXBuilder builder = new SAXBuilder();
			Element root = builder.build(in).getRootElement();
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
		data.allNodes = new LinkedList<Node>(); 
		for (Object o : e.getChildren("wpt", GPX))
			data.allNodes.add(parseWaypoint((Element)o));

		// read tracks
		for (Object trackElement : e.getChildren("trk", GPX)) {
			Track track = new Track();
			for (Object trackSegmentElement : ((Element)trackElement).getChildren("trkseg", GPX)) {
				Node start = null;
				for (Object w : ((Element)trackSegmentElement).getChildren("trkpt", GPX)) {
					Node node = parseWaypoint((Element)w);
					data.allNodes.add(node);
					if (start == null)
						start = node;
					else {
						LineSegment lineSegment = new LineSegment(start, node);
						track.segments.add(lineSegment);
						start = null;
					}
				}
			}
			if (data.tracks == null)
				data.tracks = new ArrayList<Track>();
			data.tracks.add(track);
		}

		return data;
	}
}
