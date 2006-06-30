package org.openstreetmap.josm.io;

import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map.Entry;

import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Segment;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.visitor.Visitor;
import org.openstreetmap.josm.tools.XmlWriter;

/**
 * Save the dataset into a stream as osm intern xml format. This is not using any
 * xml library for storing.
 * @author imi
 */
public class OsmWriter implements Visitor {

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

	/**
	 * Output the data to the stream
	 * @param osmConform <code>true</code>, if the xml should be 100% osm conform. In this
	 * 		case, not all information can be retrieved later (as example, modified state
	 * 		is lost and id's remain 0 instead of decrementing from -1)
	 */
	public static void output(OutputStream out, DataSet ds, boolean osmConform) {
		OsmWriter writer = new OsmWriter(out, osmConform);
		writer.out.println("<?xml version='1.0' encoding='UTF-8'?>");
		writer.out.println("<osm version='0.3' generator='JOSM'>");
		for (Node n : ds.nodes)
			writer.visit(n);
		for (Segment ls : ds.segments)
			writer.visit(ls);
		for (Way w : ds.ways)
			writer.visit(w);
		writer.out.println("</osm>");
		writer.close();
	}

	public static void outputSingle(OutputStream out, OsmPrimitive osm, boolean osmConform) {
		OsmWriter writer = new OsmWriter(out, osmConform);
		writer.out.println(XmlWriter.header());
		writer.out.println("<osm version='0.3' generator='JOSM'>");
		osm.visit(writer);
		writer.out.println("</osm>");
		writer.close();
	}

	private OsmWriter(OutputStream out, boolean osmConform) {
		try {
	        this.out = new PrintWriter(new OutputStreamWriter(out, "UTF-8"));
        } catch (UnsupportedEncodingException e) {
        	throw new RuntimeException(e);
        }
		this.osmConform = osmConform;
	}

	public void visit(Node n) {
		addCommon(n, "node");
		out.print(" lat='"+n.coor.lat()+"' lon='"+n.coor.lon()+"'");
		addTags(n, "node", true);
	}

	public void visit(Segment ls) {
		if (ls.incomplete)
			return; // Do not write an incomplete segment
		addCommon(ls, "segment");
		out.print(" from='"+getUsedId(ls.from)+"' to='"+getUsedId(ls.to)+"'");
		addTags(ls, "segment", true);
	}

	public void visit(Way w) {
		addCommon(w, "way");
		out.println(">");
		for (Segment ls : w.segments)
			out.println("    <seg id='"+getUsedId(ls)+"' />");
		addTags(w, "way", false);
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
			for (Entry<String, String> e : osm.keys.entrySet())
				out.println("    <tag k='"+ XmlWriter.encode(e.getKey()) +
						"' v='"+XmlWriter.encode(e.getValue())+ "' />");
			out.println("  </" + tagname + ">");
		} else if (tagOpen)
			out.println(" />");
		else
			out.println("  </" + tagname + ">");
	}

	/**
	 * Add the common part as the from of the tag as well as the id or the action tag.
	 */
	private void addCommon(OsmPrimitive osm, String tagname) {
		out.print("  <"+tagname+" id='"+getUsedId(osm)+"'");
		if (!osmConform) {
			String action = null;
			if (osm.deleted)
				action = "delete";
			else if (osm.modified)
				action = "modify";
			if (action != null)
				out.print(" action='"+action+"'");
		}
		if (osm.timestamp != null) {
			String time = osm.getTimeStr();
			out.print(" timestamp='"+time+"'");
		}
	}

	public void close() {
	    out.close();
    }
}
