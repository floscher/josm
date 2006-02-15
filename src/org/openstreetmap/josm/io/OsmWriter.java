package org.openstreetmap.josm.io;

import java.io.IOException;
import java.io.Writer;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
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
import org.openstreetmap.josm.data.osm.visitor.Visitor;


/**
 * Save the dataset into a stream as osm intern xml format.
 * @author imi
 */
public class OsmWriter implements Visitor {

	/**
	 * The output writer to write the xml stream to.
	 */
	private final Writer out;
	/**
	 * The commands that should be uploaded on the server.
	 */
	private DataSet ds;
	/**
	 * The counter for new created objects. Starting at -1 and goes down.
	 */
	private long newIdCounter = -1;
	/**
	 * All newly created ids and their primitive that uses it. This is a back reference
	 * map to allow references to use the correnct primitives.
	 */
	private HashMap<OsmPrimitive, Long> usedNewIds = new HashMap<OsmPrimitive, Long>();

	/**
	 * Set from the visitor functions as result.
	 */
	private Element element;
	/**
	 * Filled with all generated properties tags.
	 */
	private Collection<Element> properties;
	
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
		Element root = new Element("osm");
		List<Element> list = root.getChildren();
		properties = new LinkedList<Element>();
		for (OsmPrimitive osm : ds.allPrimitives()) {
			osm.visit(this);
			list.add(element);
		}
		list.addAll(properties);

		Document d = new Document(root);
		XMLOutputter xmlOut = new XMLOutputter(Format.getPrettyFormat());
		xmlOut.output(d, out);
	}

	/**
	 * Create a properties element.
	 */
	private Element parseProperty(OsmPrimitive osm, long id, Entry<Key, String> entry) {
		Element e = new Element("property");
		Key key = entry.getKey();
		e.setAttribute("uid", ""+id);
		e.setAttribute("key", key.name);
		e.setAttribute("value", entry.getValue());
		return e;
	}
	
	/**
	 * Add the id attribute to the element and the properties to the collection.
	 */
	private void addProperties(Element e, OsmPrimitive osm) {
		long id = osm.id;
		if (id == 0) {
			id = newIdCounter--;
			usedNewIds.put(osm, id);
		}
		e.setAttribute("uid", ""+id);
		if (osm.keys != null)
			for (Entry<Key, String> entry : osm.keys.entrySet())
				properties.add(parseProperty(osm, id, entry));
		if (osm.isDeleted())
			e.setAttribute("action", "delete");
		else if (osm.modified && osm.modifiedProperties)
			e.setAttribute("action", "modify");
		else if (osm.modified && !osm.modifiedProperties)
			e.setAttribute("action", "modify/object");
		else if (!osm.modified && osm.modifiedProperties)
			e.setAttribute("action", "modify/property");
	}

	/**
	 * Create an node element. Add all properties of the node to the properties-list.
	 */
	public void visit(Node n) {
		element = new Element("node");
		addProperties(element, n);
		element.setAttribute("lat", ""+n.coor.lat);
		element.setAttribute("lon", ""+n.coor.lon);
	}

	/**
	 * Create an line segment element. Add all properties of the node to the properties-list.
	 */
	public void visit(LineSegment ls) {
		element = new Element("segment");
		addProperties(element, ls);
		element.setAttribute("from", ""+getUsedId(ls.start));
		element.setAttribute("to", ""+getUsedId(ls.end));
	}

	/**
	 * Return the id for the given osm primitive (may access the usedId map)
	 */
	private Long getUsedId(OsmPrimitive osm) {
		return osm.id == 0 ? usedNewIds.get(osm) : osm.id;
	}

	/**
	 * Create an track element. Add all properties of the node to the properties-list.
	 */
	@SuppressWarnings("unchecked")
	public void visit(Track t) {
		element = new Element("track");
		addProperties(element, t);
		for (LineSegment ls : t.segments)
			element.getChildren().add(new Element("segment").setAttribute("uid", ""+getUsedId(ls)));
	}

	public void visit(Key k) {
		element = new Element("property");
		addProperties(element, k);
	}
}
