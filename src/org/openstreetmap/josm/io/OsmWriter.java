package org.openstreetmap.josm.io;

import java.io.PrintWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map.Entry;

import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Key;
import org.openstreetmap.josm.data.osm.LineSegment;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Track;
import org.openstreetmap.josm.data.osm.visitor.Visitor;
import org.xml.sax.SAXException;

/**
 * Save the dataset into a stream as osm intern xml format. This is not using any
 * xml library for storing.
 * @author imi
 */
public class OsmWriter implements Visitor {

	private class RuntimeEncodingException extends RuntimeException {
		public RuntimeEncodingException(Throwable t) {
			super(t);
		}
		public RuntimeEncodingException() {
		}
	}

	/**
	 * The output writer to save the values to.
	 */
	private PrintWriter out;

	/**
	 * The counter for new created objects. Starting at -1 and goes down.
	 */
	private long newIdCounter = -1;
	/**
	 * All newly created ids and their primitive that uses it. This is a back reference
	 * map to allow references to use the correnct primitives.
	 */
	private HashMap<OsmPrimitive, Long> usedNewIds = new HashMap<OsmPrimitive, Long>();

	private final boolean osmConform;

	private final static HashMap<Character, String> encoding = new HashMap<Character, String>();
	static {
		encoding.put('<', "&lt;");
		encoding.put('>', "&gt;");
		encoding.put('"', "&quot;");
		encoding.put('\'', "&apos;");
		encoding.put('&', "&amp;");
		encoding.put('\n', "&#xA;");
		encoding.put('\r', "&#xD;");
		encoding.put('\t', "&#x9;");
	}
	
	/**
	 * Output the data to the stream
	 * @param osmConform <code>true</code>, if the xml should be 100% osm conform. In this
	 * 		case, not all information can be retrieved later (as example, modified state
	 * 		is lost and id's remain 0 instead of decrementing from -1)
	 */
	public static void output(Writer out, DataSet ds, boolean osmConform) {
		OsmWriter writer = new OsmWriter(out, osmConform);
		writer.out.println("<?xml version='1.0' encoding='UTF-8'?>");
		writer.out.println("<osm version='0.3' generator='JOSM'>");
		for (Node n : ds.nodes)
			writer.visit(n);
		for (LineSegment ls : ds.lineSegments)
			writer.visit(ls);
		for (Track w : ds.tracks)
			writer.visit(w);
		writer.out.println("</osm>");
	}

	public static void outputSingle(Writer out, OsmPrimitive osm, boolean osmConform) throws SAXException {
		try {
			OsmWriter writer = new OsmWriter(out, osmConform);
			writer.out.println("<?xml version='1.0' encoding='UTF-8'?>");
			writer.out.println("<osm version='0.3' generator='JOSM'>");
			osm.visit(writer);
			writer.out.println("</osm>");
		} catch (RuntimeEncodingException e) {
			throw new SAXException("Your Java installation does not support the required UTF-8 encoding", (Exception)e.getCause());
		}		
	}

	private OsmWriter(Writer out, boolean osmConform) {
		if (out instanceof PrintWriter)
			this.out = (PrintWriter)out;
		else
			this.out = new PrintWriter(out);
		this.osmConform = osmConform;
	}

	public void visit(Node n) {
		addCommon(n, "node");
		out.print(" lat='"+n.coor.lat+"' lon='"+n.coor.lon+"'");
		addTags(n, "node", true);
	}

	public void visit(LineSegment ls) {
		addCommon(ls, "segment");
		out.print(" from='"+getUsedId(ls.start)+"' to='"+getUsedId(ls.end)+"'");
		addTags(ls, "segment", true);
	}

	public void visit(Track w) {
		addCommon(w, "way");
		out.println(">");
		for (LineSegment ls : w.segments)
			out.println("    <seg id='"+getUsedId(ls)+"' />");
		addTags(w, "way", false);
	}

	public void visit(Key k) {
	}

	/**
	 * Return the id for the given osm primitive (may access the usedId map)
	 */
	private long getUsedId(OsmPrimitive osm) {
		if (osm.id != 0)
			return osm.id;
		if (usedNewIds.containsKey(osm))
			return usedNewIds.get(osm);
		usedNewIds.put(osm, newIdCounter);
		return osmConform ? 0 : newIdCounter--;
	}


	private void addTags(OsmPrimitive osm, String tagname, boolean tagOpen) {
		if (osm.keys != null) {
			if (tagOpen)
				out.println(">");
			for (Entry<Key, String> e : osm.keys.entrySet())
				out.println("    <tag k='"+ encode(e.getKey().name) +
						"' v='"+encode(e.getValue())+ "' />");
			out.println("  </" + tagname + ">");
		} else if (tagOpen)
			out.println(" />");
		else
			out.println("  </" + tagname + ">");
	}

	/**
	 * Encode the given string in XML1.0 format.
	 * Optimized to fast pass strings that don't need encoding (normal case).
	 */
	public String encode(String unencoded) {
		StringBuilder buffer = null;
		for (int i = 0; i < unencoded.length(); ++i) {
			String encS = encoding.get(unencoded.charAt(i));
			if (encS != null) {
				if (buffer == null)
					buffer = new StringBuilder(unencoded.substring(0,i));
				buffer.append(encS);
			} else if (buffer != null)
				buffer.append(unencoded.charAt(i));
		}
		return (buffer == null) ? unencoded : buffer.toString();
	}


	/**
	 * Add the common part as the start of the tag as well as the id or the action tag.
	 */
	private void addCommon(OsmPrimitive osm, String tagname) {
		out.print("  <"+tagname+" id='"+getUsedId(osm)+"'");
		if (!osmConform) {
			String action = null;
			if (osm.isDeleted())
				action = "delete";
			else if (osm.modified && osm.modifiedProperties)
				action = "modify";
			else if (osm.modified && !osm.modifiedProperties)
				action = "modify/object";
			else if (!osm.modified && osm.modifiedProperties)
				action = "modify/property";
			if (action != null)
				out.print(" action='"+action+"'");
		}
	}
}
