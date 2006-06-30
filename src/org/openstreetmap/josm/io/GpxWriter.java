package org.openstreetmap.josm.io;

import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.Collection;
import java.util.LinkedList;

import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Segment;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.layer.RawGpsLayer.GpsPoint;
import org.openstreetmap.josm.tools.XmlWriter;

/**
 * Exports a dataset to GPX data. All information available are tried to store in
 * the gpx. If no corresponding tag is available in GPX, use 
 * <code>&lt;extensions&gt;</code> instead.
 * 
 * GPX-Way segments are stored as 2-node-pairs, so no &lt;trkseg&gt; with more
 * or less than 2 &lt;trkpt&gt; are exported.
 * 
 * @author imi
 */
public class GpxWriter {

	/**
	 * This is the output writer to store the resulting data in.
	 */
	private PrintWriter out;

	public GpxWriter(OutputStream os, String name, String desc, String author, String email, String copyright, String year, String keywords) {
		try {
			out = new PrintWriter(new OutputStreamWriter(os, "UTF-8"));
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
		out.println(XmlWriter.header());
		out.println("<gpx version='1.1' creator='JOSM' xmlns='http://www.topografix.com/GPX/1/1'>");
		out.println("  <metadata>");
		if (!name.equals(""))
			out.println("    <name>"+XmlWriter.encode(name)+"</name>");
		if (!desc.equals(""))
			out.println("    <desc>"+XmlWriter.encode(desc)+"</desc>");
		if (!author.equals("")) {
			out.println("    <author>");
			out.println("      <name>"+XmlWriter.encode(author)+"</name>");
			if (!email.equals(""))
				out.println("      <email>"+XmlWriter.encode(email)+"</email>");
			out.println("    </author>");
			if (!copyright.equals("")) {
				out.println("    <copyright author='"+XmlWriter.encode(author)+"'>");
				if (!year.equals(""))
					out.println("      <year>"+XmlWriter.encode(year)+"</year>");
				out.println("      <license>"+XmlWriter.encode(copyright)+"</license>");
				out.println("    </copyright>");
			}
		}
		if (!keywords.equals("")) {
			out.println("    <keywords>"+XmlWriter.encode(keywords)+"</keywords>");
		}
		// don't finish here, to give output functions the chance to add <bounds>
	}

	/**
	 * Export the dataset to gpx. Only the physical segment structure is
	 * exported. To do this, the list of ways is processed. If a way span a 
	 * sequence of segments, this is added as one trkseg.
	 * Then, all remaining segments are added in one extra trk. Finally,
	 * all remaining nodes are added as wpt.
	 */
	public void output(DataSet data) {
		Collection<OsmPrimitive> all = data.allNonDeletedPrimitives();
		if (all.isEmpty()) {
			out.println("  </metadata>");
			out.println("</gpx>");
			return;
		}
		// calculate bounds
		Bounds b = new Bounds(new LatLon(Double.MAX_VALUE, Double.MAX_VALUE), new LatLon(Double.MIN_VALUE, Double.MIN_VALUE));
		for (Node n : data.nodes)
			if (!n.deleted)
				b.extend(n.coor);
		out.println("    <bounds minlat='"+b.min.lat()+"' minlon='"+b.min.lon()+"' maxlat='"+b.max.lat()+"' maxlon='"+b.max.lon()+"' />");
		out.println("  </metadata>");

		// add ways
		for (Way w : data.ways) {
			if (w.deleted)
				continue;
			out.println("  <trk>");
			Segment oldLs = null;
			for (Segment ls : w.segments) {
				if (ls.incomplete)
					continue;
				// end old segemnt, if no longer match a chain
				if (oldLs != null && !oldLs.to.coor.equals(ls.from.coor)) {
					out.println("    </trkseg>");
					outputNode(oldLs.to, false);
					all.remove(oldLs.to);
					oldLs = null;
				}
				// start new segment if necessary
				if (oldLs == null)
					out.println("    <trkseg>");
				outputNode(ls.from, false);
				all.remove(ls.from);
				oldLs = ls;
				all.remove(ls);
			}
			// write last node if there
			if (oldLs != null) {
				outputNode(oldLs.to, false);
				all.remove(oldLs.to);
				out.println("    </trkseg>");
			}
			out.println("  </trk>");
			all.remove(w);
		}

		// add remaining segments
		Collection<Segment> segments = new LinkedList<Segment>();
		for (OsmPrimitive osm : all)
			if (osm instanceof Segment && !((Segment)osm).incomplete)
				segments.add((Segment)osm);
		if (!segments.isEmpty()) {
			out.println("  <trk>");
			for (Segment ls : segments) {
				out.println("    <trkseg>");
				outputNode(ls.from, false);
				all.remove(ls.from);
				outputNode(ls.to, false);
				all.remove(ls.to);
				out.println("    </trkseg>");
				all.remove(ls);
			}
			out.println("  </trk>");
		}

		// finally add the remaining nodes
		for (OsmPrimitive osm : all)
			if (osm instanceof Node)
				outputNode((Node)osm, true);

		out.println("</gpx>");
	}


	/**
	 * Export the collection structure to gpx. The gpx will consists of only one
	 * trk with as many trkseg as there are collections in the outer collection.
	 */
	public void output(Collection<Collection<GpsPoint>> data) {
		if (data.size() == 0) {
			out.println("  </metadata>");
			out.println("</gpx>");
			return;
		}
		// calculate bounds
		Bounds b = new Bounds(new LatLon(Double.MAX_VALUE, Double.MAX_VALUE), new LatLon(Double.MIN_VALUE, Double.MIN_VALUE));
		for (Collection<GpsPoint> c : data)
			for (GpsPoint p : c)
				b.extend(p.latlon);
		out.println("    <bounds minlat='"+b.min.lat()+"' minlon='"+b.min.lon()+"' maxlat='"+b.max.lat()+"' maxlon='"+b.max.lon()+"' />");
		out.println("  </metadata>");

		out.println("  <trk>");
		for (Collection<GpsPoint> c : data) {
			out.println("    <trkseg>");
			LatLon last = null;
			for (GpsPoint p : c) {
				// skip double entries
				if (p.latlon.equals(last))
					continue;
				last =  p.latlon;
				LatLon ll = p.latlon;
				out.print("      <trkpt lat='"+ll.lat()+"' lon='"+ll.lon()+"'");
				if (p.time != null && p.time.length()!=0) {
					out.println(">");
					out.println("        <time>"+p.time+"</time>");
					out.println("      </trkpt>");
				} else
					out.println(" />");
			}
			out.println("    </trkseg>");
		}
		out.println("  </trk>");
		out.println("</gpx>");
	}

	private void outputNode(Node n, boolean wpt) {
		out.print((wpt?"  <wpt":"      <trkpt")+" lat='"+n.coor.lat()+"' lon='"+n.coor.lon()+"'");
		if (n.keys == null) {
			out.println(" />");
			return;
		}
		boolean found = false;
		String[] possibleKeys = {"ele", "time", "magvar", "geoidheight", "name",
				"cmt", "desc", "src", "link", "sym", "type", "fix", "sat",
				"hdop", "vdop", "pdop", "ageofgpsdata", "dgpsid"};
		Collection<String> keys = n.keySet();
		for (String k : possibleKeys) {
			if (keys.contains(k)) {
				if (!found) {
					found = true;
					out.println(">");
				}
				if (k.equals("link")) {
					out.println("        <link>");
					out.println("          <text>"+XmlWriter.encode(n.get(k))+"</text>");
					out.println("        </link>");
				} else
					out.println("        <"+k+">"+XmlWriter.encode(n.get(k))+"</"+k+">");
			}
		}
		if (found)
			out.println(wpt?"  </wpt>":"      </trkpt>");
		else
			out.println(" />");
	}

	public void close() {
		out.flush();
		out.close();
	}
}
