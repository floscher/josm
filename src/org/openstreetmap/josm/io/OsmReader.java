package org.openstreetmap.josm.io;

import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;

import org.openstreetmap.josm.data.GeoPoint;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.LineSegment;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.visitor.AddVisitor;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import uk.co.wilson.xml.MinML2;

/**
 * Parser for the Osm Api. Read from an input stream and construct a dataset out of it.
 * 
 * @author Imi
 */
public class OsmReader extends MinML2 {

	/**
	 * The dataset to add parsed objects to.
	 */
	private DataSet ds = new DataSet();

	/**
	 * The visitor to use to add the data to the set.
	 */
	private AddVisitor adder = new AddVisitor(ds);
	
	/**
	 * The current processed primitive.
	 */
	private OsmPrimitive current;

	/**
	 * All read nodes so far.
	 */
	private Map<Long, Node> nodes = new HashMap<Long, Node>();
	/**
	 * All read segents so far.
	 */
	private Map<Long, LineSegment> lineSegments = new HashMap<Long, LineSegment>();
	
	/**
	 * Parse the given input source and return the dataset.
	 */
	public static DataSet parseDataSet(Reader source) throws SAXException, IOException {
		OsmReader osm = new OsmReader(source);

		// clear all negative ids (new to this file)
		for (OsmPrimitive o : osm.ds.allPrimitives())
			if (o.id < 0)
				o.id = 0;
		
		return osm.ds;
	}

	private OsmReader(Reader source) throws SAXException, IOException {
		parse(source);
	}

	@Override
	public void startElement(String namespaceURI, String localName, String qName, Attributes atts) throws SAXException {
		try {
			if (qName.equals("osm")) {
				if (atts == null)
					throw new SAXException("Unknown version.");
				if (!"0.3".equals(atts.getValue("version")))
					throw new SAXException("Unknown version: "+atts.getValue("version"));
			} else if (qName.equals("node")) {
				Node n = new Node();
				n.coor = new GeoPoint(getDouble(atts, "lat"), getDouble(atts, "lon"));
				current = n;
				readCommon(atts);
				current.id = getLong(atts, "id");
				nodes.put(n.id, n);
			} else if (qName.equals("segment")) {
				current = new LineSegment(
						nodes.get(getLong(atts, "from")), 
						nodes.get(getLong(atts, "to")));
				readCommon(atts);
				lineSegments.put(current.id, (LineSegment)current);
			} else if (qName.equals("way")) {
				current = new Way();
				readCommon(atts);
			} else if (qName.equals("seg")) {
				if (current instanceof Way) {
					LineSegment ls = lineSegments.get(getLong(atts, "id"));
					if (ls == null)
						fatalError(new SAXParseException("Line segment "+getLong(atts, "id")+" has not been transfered before.", null));
					((Way)current).segments.add(ls);
				}
			} else if (qName.equals("tag")) {
				current.put(atts.getValue("k"), atts.getValue("v"));
			}
		} catch (NumberFormatException x) {
			throw new SAXException(x.getMessage(), x);
		} catch (NullPointerException x) {
			throw new SAXException("NullPointerException. Possible some missing tags.", x);
		}
	}

	
	@Override
	public void endElement(String namespaceURI, String localName, String qName) throws SAXException {
		if (qName.equals("node") || qName.equals("segment") || qName.equals("way") || qName.equals("area")) {
			current.visit(adder);
		}
	}

	/**
	 * Read out the common attributes from atts and put them into this.current.
	 */
	private void readCommon(Attributes atts) {
		current.id = getLong(atts, "id");
		String action = atts.getValue("action");
		if ("delete".equals(action))
			current.setDeleted(true);
		else if ("modify".equals(action)) {
			current.modified = true;
			current.modifiedProperties = true;
		} else if ("modify/object".equals(action))
			current.modified = true;
		else if ("modify/property".equals(action))
			current.modifiedProperties = true;
	}

	private double getDouble(Attributes atts, String value) {
		return Double.parseDouble(atts.getValue(value));
	}
	private long getLong(Attributes atts, String value) {
		return Long.parseLong(atts.getValue(value));
	}
}
