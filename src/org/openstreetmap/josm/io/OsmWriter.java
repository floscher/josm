package org.openstreetmap.josm.io;

import java.io.IOException;
import java.io.Writer;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.Map.Entry;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Key;
import org.openstreetmap.josm.data.osm.LineSegment;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Track;


/**
 * Save the dataset into a stream as osm intern xml format.
 * @author imi
 */
public class OsmWriter extends OsmConnection {

	/**
	 * The output writer to write the xml stream to.
	 */
	private final Writer out;
	/**
	 * The commands that should be uploaded on the server.
	 */
	private DataSet ds;
	/**
	 * ID generator start for all nodes with id==0
	 */
	private long id = 0;
	/**
	 * A collection of all ids used so far to look up and jump over used ids.
	 */
	private Set<Long> ids;
	
	public OsmWriter(Writer out, DataSet dataSet) {
		this.out = out;
		ds = dataSet;
	}

	/**
	 * Output the data to the stream
	 * @throws IOException In case of stream IO errors.
	 */
	@SuppressWarnings("unchecked")
	public void output() throws IOException {
		ids = allUsedIds();
		Element root = new Element("osm");
		List<Element> list = root.getChildren();
		Collection<Element> properties = new LinkedList<Element>();
		for (Node n : ds.nodes)
			list.add(parseNode(n, properties));
		for (LineSegment ls : ds.lineSegments)
			list.add(parseLineSegment(ls, properties));
		// all other line segments
		Collection<LineSegment> lineSegments = new HashSet<LineSegment>();
		for (Track t : ds.tracks)
			lineSegments.addAll(t.segments);
		for (LineSegment ls : lineSegments)
			list.add(parseLineSegment(ls, properties));
		for (Track t : ds.tracks)
			list.add(parseTrack(t, properties));
		list.addAll(properties);

		Document d = new Document(root);
		XMLOutputter xmlOut = new XMLOutputter(Format.getPrettyFormat());
		xmlOut.output(d, out);
	}

	/**
	 * Create an track element. Add all properties of the node to the properties-list.
	 */
	@SuppressWarnings("unchecked")
	private Element parseTrack(Track t, Collection<Element> properties) {
		Element e = new Element("track");
		addProperties(e, t, properties);
		for (LineSegment ls : t.segments)
			e.getChildren().add(new Element("segment").setAttribute("uid", ""+ls.id));
		return e;
	}

	/**
	 * Create an node element. Add all properties of the node to the properties-list.
	 */
	private Element parseNode(Node n, Collection<Element> properties) {
		Element e = new Element("node");
		addProperties(e, n, properties);
		e.setAttribute("lat", ""+n.coor.lat);
		e.setAttribute("lon", ""+n.coor.lon);
		return e;
	}

	

	/**
	 * Create an line segment element. Add all properties of the node to the properties-list.
	 */
	private Element parseLineSegment(LineSegment ls, Collection<Element> properties) {
		Element e = new Element("segment");
		addProperties(e, ls, properties);
		e.setAttribute("from", ""+ls.start.id);
		e.setAttribute("to", ""+ls.end.id);
		return e;
	}
	
	/**
	 * Create a properties element.
	 */
	private Element parseProperty(OsmPrimitive osm, Entry<Key, String> entry) {
		Element e = new Element("property");
		Key key = entry.getKey();
		if (key.id == 0)
			key.id = generateId();
		e.setAttribute("uid", ""+key.id);
		e.setAttribute("object", ""+osm.id);
		e.setAttribute("key", key.name);
		e.setAttribute("value", entry.getValue());
		return e;
	}
	
	/**
	 * Add the id attribute to the element and the properties to the collection.
	 */
	private void addProperties(Element e, OsmPrimitive osm, Collection<Element> properties) {
		if (osm.id == 0)
			osm.id = generateId();
		e.setAttribute("uid", ""+osm.id);
		if (osm.keys != null)
			for (Entry<Key, String> entry : osm.keys.entrySet())
				properties.add(parseProperty(osm, entry));
	}

	/**
	 * Generate an new unused id.
	 */
	private long generateId() {
		while (ids.contains(Long.valueOf(id)))
			id++;
		ids.add(id);
		return id;
	}

	/**
	 * Return all used ids in a set. 
	 */
	private Set<Long> allUsedIds() {
		HashSet<Long> ids = new HashSet<Long>();
		for (OsmPrimitive osm : ds.nodes)
			addIdAndKeyIds(osm, ids);
		for (OsmPrimitive osm : ds.lineSegments)
			addIdAndKeyIds(osm, ids);
		for (Track t : ds.tracks) {
			addIdAndKeyIds(t, ids);
			for (LineSegment ls : t.segments) {
				addIdAndKeyIds(ls, ids);
				addIdAndKeyIds(ls.start, ids);
				addIdAndKeyIds(ls.end, ids);
			}
		}
		return ids;
	}

	/**
	 * Return all used ids in the given osm primitive.
	 */
	private void addIdAndKeyIds(OsmPrimitive osm, Collection<Long> ids) {
		ids.add(osm.id);
		if (osm.keys != null)
			for (Key key : osm.keys.keySet())
				ids.add(key.id);
	}
}

