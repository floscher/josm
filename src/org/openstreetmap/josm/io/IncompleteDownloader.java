package org.openstreetmap.josm.io;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.Collection;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.Segment;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.visitor.MergeVisitor;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import uk.co.wilson.xml.MinML2;

/**
 * Capable of downloading ways without having to fully parse their segments.
 * 
 * @author Imi
 */
public class IncompleteDownloader extends OsmServerReader {

	/**
	 * The list of incomplete Ways to download. The ways will be filled and are complete after download.
	 */
	private final Collection<Way> toDownload;
	private MergeVisitor merger = new MergeVisitor(Main.ds);
	
	public IncompleteDownloader(Collection<Way> toDownload) {
		this.toDownload = toDownload;
	}

	public void parse() throws SAXException, IOException {
		Main.pleaseWaitDlg.progress.setMaximum(toDownload.size());
		Main.pleaseWaitDlg.progress.setValue(0);
		int i = 0;
		try {
			for (Way w : toDownload) {
				download(w);
				Main.pleaseWaitDlg.progress.setValue(++i);
			}
		} catch (IOException e) {
			if (!cancel)
				throw e;
		} catch (SAXException e) {
			throw e;
		} catch (Exception e) {
			if (!cancel)
				throw (e instanceof RuntimeException) ? (RuntimeException)e : new RuntimeException(e);
		}
	}

	private static class SegmentParser extends MinML2 {
		public long from, to;
		@Override public void startElement(String ns, String lname, String qname, Attributes a) {
			if (qname.equals("segment")) {
				from = Long.parseLong(a.getValue("from"));
				to = Long.parseLong(a.getValue("to"));
			}
		}
	}

	private void download(Way w) throws IOException, SAXException {
		// get all the segments
		for (Segment s : w.segments) {
			if (!s.incomplete)
				continue;
			BufferedReader segReader;
		    try {
		    	segReader = new BufferedReader(new InputStreamReader(getInputStream("segment/"+s.id, null), "UTF-8"));
	        } catch (FileNotFoundException e) {
		        e.printStackTrace();
		        throw new IOException(tr("Data error: Segment {0} is deleted but part of Way {1}", s.id, w.id));
	        }
			StringBuilder segBuilder = new StringBuilder();
			for (String line = segReader.readLine(); line != null; line = segReader.readLine())
				segBuilder.append(line+"\n");
			SegmentParser segmentParser = new SegmentParser();
			segmentParser.parse(new StringReader(segBuilder.toString()));
			if (segmentParser.from == 0 || segmentParser.to == 0) {
				System.out.println(segBuilder.toString());
				throw new SAXException("Invalid segment response.");
			}
			if (!hasNode(segmentParser.from))
				readNode(segmentParser.from, s.id).visit(merger);
			if (!hasNode(segmentParser.to))
				readNode(segmentParser.to, s.id).visit(merger);
			readSegment(segBuilder.toString()).visit(merger);
		}
	}

	private boolean hasNode(long id) {
	    for (Node n : Main.ds.nodes)
	    	if (n.id == id)
	    		return true;
	    return false;
    }

	private Segment readSegment(String seg) throws SAXException, IOException {
        return OsmReader.parseDataSet(new ByteArrayInputStream(seg.getBytes("UTF-8")), Main.ds, null).segments.iterator().next();
    }

	private Node readNode(long id, long segId) throws SAXException, IOException {
		try {
	        return OsmReader.parseDataSet(getInputStream("node/"+id, null), Main.ds, null).nodes.iterator().next();
        } catch (FileNotFoundException e) {
	        e.printStackTrace();
	        throw new IOException(tr("Data error: Node {0} is deleted but part of Segment {1}", id, segId));
        }
    }
}
