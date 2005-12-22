package org.openstreetmap.josm.data.osm.visitor;

import java.util.Map.Entry;

import org.jdom.Element;
import org.openstreetmap.josm.data.osm.Key;
import org.openstreetmap.josm.data.osm.LineSegment;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Track;


/**
 * An visitor which is capable of convert the visited objects to osm xml syntax
 * using JDOM.
 *
 * @author imi
 */
public class OsmXmlVisitor implements Visitor {

	private final boolean reference;
	
	public Element element;

	/**
	 * The counter for ids for new objects. 
	 */
	private static int newCounter = -1;

	/**
	 * Specify, which output do you like to have.
	 * 
	 * @param reference <code>true</code> means, the xml output is only a reference
	 * 		containing the id (for deletion and referencing etc).
	 */
	public OsmXmlVisitor(boolean reference) {
		this.reference = reference;
	}

	
	public void visit(Node n) {
		element = new Element("node");
		addCommon(n);
		if (!reference) {
			element.setAttribute("lat", ""+n.coor.lat);
			element.setAttribute("lon", ""+n.coor.lon);
		}
	}

	public void visit(LineSegment ls) {
		element = new Element("segment");
		addCommon(ls);
		if (!reference) {
			element.setAttribute("from", ""+ls.start.id);
			element.setAttribute("to", ""+ls.end.id);
		}
	}

	@SuppressWarnings("unchecked")
	public void visit(Track t) {
		element = new Element("track");
		addCommon(t);
		if (!reference) {
			for (LineSegment ls : t.segments) {
				OsmXmlVisitor v = new OsmXmlVisitor(true);
				v.visit(ls);
				element.getChildren().add(v.element);
			}
		}
	}

	public void visit(Key k) {
		//TODO
		throw new RuntimeException("cannot add keys yet.");
	}

	/**
	 * Add the common parts of the object.
	 */
	private void addCommon(OsmPrimitive osm) {
		element.setAttribute("uid", ""+(osm.id == 0 ? newCounter-- : osm.id));
		if (!reference && osm.keys != null) {
			StringBuilder tags = new StringBuilder();
			for (Entry<Key, String> e : osm.keys.entrySet()) {
				tags.append(e.getKey().name);
				tags.append("=");
				tags.append(e.getValue());
				tags.append(";");
			}
			element.setAttribute("tags", tags.toString());
		}
	}
}
