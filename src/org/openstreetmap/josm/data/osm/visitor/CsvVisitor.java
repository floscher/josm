package org.openstreetmap.josm.data.osm.visitor;

import java.io.PrintWriter;
import java.io.Writer;
import java.util.Map.Entry;

import org.openstreetmap.josm.data.osm.Key;
import org.openstreetmap.josm.data.osm.LineSegment;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Track;

/**
 * Outputs the visited primitive as comma seperated value.
 * @author imi
 */
public class CsvVisitor implements Visitor {

	private final PrintWriter out;

	/**
	 * Construct the visitor.
	 * @param out The stream, where the output should go to. 
	 */
	public CsvVisitor(Writer out) {
		this.out = new PrintWriter(out);
	}
	
	public void visit(Node n) {
		out.print("n,"+common(n)+","+n.coor.lat+","+n.coor.lon+","+n.coor.x+","+n.coor.y);
	}

	public void visit(LineSegment ls) {
		out.print("ls,"+common(ls)+",");
		visit(ls.start);
		out.print(',');
		visit(ls.end);
	}

	public void visit(Track t) {
		out.print("t,"+common(t)+","+t.segments.size());
		for (LineSegment ls : t.segments) {
			out.print(',');
			visit(ls);
		}
	}

	public void visit(Key k) {
		//TODO
	}

	/**
	 * Create a string for all common fields in the primitive
	 * @param osm The primitive
	 * @return A string containing the fields as csv.
	 */
	private String common(OsmPrimitive osm) {
		StringBuilder b = new StringBuilder();
		b.append(osm.id);
		if (osm.keys != null) {
			b.append(","+osm.keys.size());
			for (Entry<Key, String> e : osm.keys.entrySet())
				b.append(e.getKey().name+","+encode(e.getValue()));
		} else
			b.append(",0");
		return b.toString();
	}

	/**
	 * Encodes the string to be inserted as csv compatible value.
	 * @param s The string to be inserted
	 * @return The encoded string
	 */
	private String encode(String s) {
		s = s.replace(",", "\\,");
		s = s.replace("\n", "\\\n");
		return s;
	}
}
