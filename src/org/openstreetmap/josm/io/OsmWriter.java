package org.openstreetmap.josm.io;

import java.io.PrintWriter;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map.Entry;

import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.LineSegment;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
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
	public static void output(Writer out, DataSet ds, boolean osmConform) {
		OsmWriter writer = new OsmWriter(out, osmConform);
		writer.out.println("<?xml version='1.0' encoding='UTF-8'?>");
		writer.out.println("<osm version='0.3' generator='JOSM'>");
		for (Node n : ds.nodes)
			writer.visit(n);
		for (LineSegment ls : ds.lineSegments)
			writer.visit(ls);
		for (Way w : ds.ways)
			writer.visit(w);
		writer.out.println("</osm>");
	}

	public static void outputSingle(Writer out, OsmPrimitive osm, boolean osmConform) {
		OsmWriter writer = new OsmWriter(out, osmConform);
		writer.out.println(XmlWriter.header());
		writer.out.println("<osm version='0.3' generator='JOSM'>");
		osm.visit(writer);
		writer.out.println("</osm>");
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
		out.print(" lat='"+n.coor.lat()+"' lon='"+n.coor.lon()+"'");
		addTags(n, "node", true);
	}

	public void visit(LineSegment ls) {
		if (ls.incomplete)
			return; // Do not write an incomplete line segment
		addCommon(ls, "segment");
		out.print(" from='"+getUsedId(ls.from)+"' to='"+getUsedId(ls.to)+"'");
		addTags(ls, "segment", true);
	}

	public void visit(Way w) {
		addCommon(w, "way");
		out.println(">");
		for (LineSegment ls : w.segments)
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
		if (osm.lastModified != null) {
			String time = new SimpleDateFormat("y-M-d H:m:s").format(osm.lastModified);
			out.print(" timestamp='"+time+"'");
		}
	}
}
