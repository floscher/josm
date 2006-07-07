package org.openstreetmap.josm.io;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;

import javax.swing.BoundedRangeModel;
import javax.swing.JLabel;

import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Segment;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.visitor.AddVisitor;
import org.openstreetmap.josm.data.osm.visitor.Visitor;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import uk.co.wilson.xml.MinML2;

/**
 * Parser for the Osm Api. Read from an input stream and construct a dataset out of it.
 * 
 * Reading process takes place in three phases. During the first phase (including xml parse),
 * all nodes are read and stored. Other information than nodes are stored in a raw list
 * 
 * The second phase reads from the raw list all segments and create Segment objects.
 * 
 * The third phase read all ways out of the remaining objects in the raw list.
 * 
 * @author Imi
 */
public class OsmReader {

	/**
	 * The dataset to add parsed objects to.
	 */
	private DataSet ds = new DataSet();

	/**
	 * The visitor to use to add the data to the set.
	 */
	private AddVisitor adder = new AddVisitor(ds);

	/**
	 * All read nodes after phase 1.
	 */
	private Map<Long, Node> nodes = new HashMap<Long, Node>();

	private static class OsmPrimitiveData extends OsmPrimitive {
		@Override public void visit(Visitor visitor) {}
		public int compareTo(OsmPrimitive o) {return 0;}

		public void copyTo(OsmPrimitive osm) {
			osm.id = id;
			osm.keys = keys;
			osm.modified = modified;
			osm.selected = selected;
			osm.deleted = deleted;
			osm.timestamp = timestamp;
		}
	}

	/**
	 * Data structure for the remaining segment objects
	 * Maps the raw attributes to key/value pairs.
	 */
	private Map<OsmPrimitiveData, long[]> segs = new HashMap<OsmPrimitiveData, long[]>();
	/**
	 * Data structure for the remaining way objects
	 */
	private Map<OsmPrimitiveData, Collection<Long>> ways = new HashMap<OsmPrimitiveData, Collection<Long>>();


	private class Parser extends MinML2 {
		/**
		 * The current osm primitive to be read.
		 */
		private OsmPrimitive current;

		@Override public void startElement(String namespaceURI, String localName, String qName, Attributes atts) throws SAXException {
			try {
				if (qName.equals("osm")) {
					if (atts == null)
						throw new SAXException(tr("Unknown version."));
					if (!"0.3".equals(atts.getValue("version")))
						throw new SAXException(tr("Unknown version {0}",atts.getValue("version")));
				} else if (qName.equals("node")) {
					current = new Node(new LatLon(getDouble(atts, "lat"), getDouble(atts, "lon")));
					readCommon(atts, current);
					nodes.put(current.id, (Node)current);
				} else if (qName.equals("segment")) {
					current = new OsmPrimitiveData();
					readCommon(atts, current);
					segs.put((OsmPrimitiveData)current, new long[]{getLong(atts, "from"), getLong(atts, "to")});
				} else if (qName.equals("way")) {
					current = new OsmPrimitiveData();
					readCommon(atts, current);
					ways.put((OsmPrimitiveData)current, new LinkedList<Long>());
				} else if (qName.equals("seg")) {
					Collection<Long> list = ways.get(current);
					if (list == null)
						throw new SAXException(tr("Found <seg> tag on non-way."));
					long id = getLong(atts, "id");
					if (id == 0)
						throw new SAXException(tr("Incomplete segment with id=0"));
					list.add(id);
				} else if (qName.equals("tag"))
					current.put(atts.getValue("k"), atts.getValue("v"));
			} catch (NumberFormatException x) {
				x.printStackTrace(); // SAXException does not chain correctly
				throw new SAXException(x.getMessage(), x);
			} catch (NullPointerException x) {
				x.printStackTrace(); // SAXException does not chain correctly
				throw new SAXException(tr("NullPointerException. Possible some missing tags."), x);
			}
		}

		private double getDouble(Attributes atts, String value) {
			return Double.parseDouble(atts.getValue(value));
		}
	}

	/**
	 * Read out the common attributes from atts and put them into this.current.
	 */
	void readCommon(Attributes atts, OsmPrimitive current) throws SAXException {
		current.id = getLong(atts, "id");
		if (current.id == 0)
			throw new SAXException(tr("Illegal object with id=0"));

		String time = atts.getValue("timestamp");
		if (time != null && time.length() != 0) {
			try {
				DateFormat df = new SimpleDateFormat("y-M-d H:m:s");
				current.timestamp = df.parse(time);
			} catch (ParseException e) {
				e.printStackTrace();
				throw new SAXException(tr("Couldn't read time format '{0}'.",time));
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
	private long getLong(Attributes atts, String value) throws SAXException {
		String s = atts.getValue(value);
		if (s == null)
			throw new SAXException(tr("Missing required attirbute '{0}'.",value));
		return Long.parseLong(s);
	}

	private void createSegments() {
		for (Entry<OsmPrimitiveData, long[]> e : segs.entrySet()) {
			Node from = nodes.get(e.getValue()[0]);
			Node to = nodes.get(e.getValue()[1]);
			if (from == null || to == null)
				continue; //TODO: implement support for incomplete nodes.
			Segment s = new Segment(from, to);
			e.getKey().copyTo(s);
			segments.put(s.id, s);
			adder.visit(s);
		}
	}

	private void createWays() {
		for (Entry<OsmPrimitiveData, Collection<Long>> e : ways.entrySet()) {
			Way w = new Way();
			for (long id : e.getValue()) {
				Segment s = segments.get(id);
				if (s == null) {
					s = new Segment(id); // incomplete line segment
					adder.visit(s);
				}
				w.segments.add(s);
			}
			e.getKey().copyTo(w);
			adder.visit(w);
		}
	}

	/**
	 * All read segments after phase 2.
	 */
	private Map<Long, Segment> segments = new HashMap<Long, Segment>();

	/**
	 * Parse the given input source and return the dataset.
	 */
	public static DataSet parseDataSet(InputStream source, JLabel currentAction, BoundedRangeModel progress) throws SAXException, IOException {
		OsmReader osm = new OsmReader();

		// phase 1: Parse nodes and read in raw segments and ways
		osm.new Parser().parse(new InputStreamReader(source, "UTF-8"));
		if (progress != null)
			progress.setValue(0);
		if (currentAction != null)
			currentAction.setText(tr("Preparing data..."));
		for (Node n : osm.nodes.values())
			osm.adder.visit(n);

		try {
			osm.createSegments();
			osm.createWays();
		} catch (NumberFormatException e) {
			e.printStackTrace();
			throw new SAXException(tr("Illformed Node id"));
		}

		// clear all negative ids (new to this file)
		for (OsmPrimitive o : osm.ds.allPrimitives())
			if (o.id < 0)
				o.id = 0;

		return osm.ds;
	}
}
