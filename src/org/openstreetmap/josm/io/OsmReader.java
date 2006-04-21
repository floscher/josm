package org.openstreetmap.josm.io;

import java.io.IOException;
import java.io.Reader;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;

import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Segment;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.visitor.AddVisitor;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

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
	private Map<Long, Segment> segments = new HashMap<Long, Segment>();
	
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

	@Override public void startElement(String namespaceURI, String localName, String qName, Attributes atts) throws SAXException {
		try {
			if (qName.equals("osm")) {
				if (atts == null)
					throw new SAXException("Unknown version.");
				if (!"0.3".equals(atts.getValue("version")))
					throw new SAXException("Unknown version "+atts.getValue("version"));
			} else if (qName.equals("node")) {
				Node n = new Node(new LatLon(getDouble(atts, "lat"), getDouble(atts, "lon")));
				current = n;
				readCommon(atts);
				current.id = getLong(atts, "id");
				nodes.put(n.id, n);
			} else if (qName.equals("segment")) {
				Node from = nodes.get(getLong(atts, "from"));
				Node to = nodes.get(getLong(atts, "to"));
				if (from == null || to == null)
					throw new SAXException("Segment "+atts.getValue("id")+" is missing its nodes.");
				current = new Segment(from, to);
				readCommon(atts);
				segments.put(current.id, (Segment)current);
			} else if (qName.equals("way")) {
				current = new Way();
				readCommon(atts);
			} else if (qName.equals("seg")) {
				if (current instanceof Way) {
					long id = getLong(atts, "id");
					if (id == 0)
						throw new SAXException("Incomplete segment with id=0");
					Segment ls = segments.get(id);
					if (ls == null) {
						ls = new Segment(id); // incomplete segment
						segments.put(id, ls);
						adder.visit(ls);
					}
					((Way)current).segments.add(ls);
				}
			} else if (qName.equals("tag"))
				current.put(atts.getValue("k"), atts.getValue("v"));
		} catch (NumberFormatException x) {
            x.printStackTrace(); // SAXException does not chain correctly
			throw new SAXException(x.getMessage(), x);
		} catch (NullPointerException x) {
			x.printStackTrace(); // SAXException does not chain correctly
			throw new SAXException("NullPointerException. Possible some missing tags.", x);
		}
	}

	
	@Override public void endElement(String namespaceURI, String localName, String qName) {
		if (qName.equals("node") || qName.equals("segment") || qName.equals("way") || qName.equals("area")) {
			current.visit(adder);
		}
	}

	/**
	 * Read out the common attributes from atts and put them into this.current.
	 */
	private void readCommon(Attributes atts) throws SAXException {
		current.id = getLong(atts, "id");
		if (current.id == 0)
			throw new SAXException("Illegal object with id=0");
		
		String time = atts.getValue("timestamp");
		if (time != null && time.length() != 0) {
			try {
				DateFormat df = new SimpleDateFormat("y-M-d H:m:s");
	            current.timestamp = df.parse(time);
            } catch (ParseException e) {
	            e.printStackTrace();
	            throw new SAXException("Couldn't read time format '"+time+"'.");
            }
		}
		
		String action = atts.getValue("action");
		if (action == null)
			return;
		if (action.equals("delete"))
			current.delete(true);
		else if (action.startsWith("modify"))
			current.modified = true;
	}

	private double getDouble(Attributes atts, String value) {
		return Double.parseDouble(atts.getValue(value));
	}
	private long getLong(Attributes atts, String value) {
		return Long.parseLong(atts.getValue(value));
	}
}
