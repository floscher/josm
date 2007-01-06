package org.openstreetmap.josm.io;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Stack;

import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.gui.layer.MarkerLayer.Marker;
import org.openstreetmap.josm.gui.layer.RawGpsLayer.GpsPoint;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import uk.co.wilson.xml.MinML2;

/**
 * Read raw gps data from a gpx file. Only way points with their ways segments
 * and waypoints are imported.
 * @author imi
 */
public class RawGpsReader {

	/**
	 * Hold the resulting gps data (tracks and their track points)
	 */
	public Collection<Collection<GpsPoint>> trackData = new LinkedList<Collection<GpsPoint>>();

	/**
	 * Hold the waypoints of the gps data.
	 */
	public Collection<Marker> markerData = new ArrayList<Marker>();

	private class Parser extends MinML2 {
		/**
		 * Current track to be read. The last entry is the current trkpt.
		 * If in wpt-mode, it contain only one GpsPoint.
		 */
		private Collection<GpsPoint> current = new LinkedList<GpsPoint>();
		private LatLon currentLatLon;
		private String currentTime = "";
		private String currentName = "";
		private Stack<String> tags = new Stack<String>();

		@Override public void startElement(String namespaceURI, String localName, String qName, Attributes atts) throws SAXException {
			if (qName.equals("wpt") || qName.equals("trkpt")) {
				try {
	                double lat = Double.parseDouble(atts.getValue("lat"));
	                double lon = Double.parseDouble(atts.getValue("lon"));
	        		if (Math.abs(lat) > 90)
	        			throw new SAXException(tr("Data error: lat value \"{0}\" is out of bound.", lat));
	        		if (Math.abs(lon) > 180)
	        			throw new SAXException(tr("Data error: lon value \"{0}\" is out of bound.", lon));
	                currentLatLon = new LatLon(lat, lon);
                } catch (NumberFormatException e) {
                	e.printStackTrace();
	                throw new SAXException(e);
                }
                currentTime = "";
                currentName = "";
			}
			tags.push(qName);
		}

		@Override public void characters(char[] ch, int start, int length) {
			String peek = tags.peek();
			if (peek.equals("time") || peek.equals("name")) {
				String tag = tags.pop();
				if (tags.empty() || (!tags.peek().equals("wpt") && !tags.peek().equals("trkpt"))) {
					tags.push(tag);
					return;
				}
				String contents = new String(ch, start, length);
				if (peek.equals("time")) currentTime += contents; else currentName += contents;
				tags.push(tag);
			}
		}

		@Override public void endElement(String namespaceURI, String localName, String qName) {
			if (qName.equals("trkpt")) {
				current.add(new GpsPoint(currentLatLon, currentTime));
				currentTime = "";
				currentName = "";
			} else if (qName.equals("wpt")) {
				markerData.add(new Marker(currentLatLon, currentName, null));
				currentTime = "";
				currentName = "";
			} else if (qName.equals("trkseg") || qName.equals("trk") || qName.equals("gpx")) {
				newTrack();
				currentTime = "";
				currentName = "";
			}
			tags.pop();
        }

		private void newTrack() {
			if (!current.isEmpty()) {
				trackData.add(current);
				current = new LinkedList<GpsPoint>();
			}
		}
	}


	/**
	 * Parse the input stream and store the result in trackData and markerData 
	 */
	public RawGpsReader(InputStream source) throws SAXException, IOException {
		Parser parser = new Parser();
		parser.parse(new InputStreamReader(source, "UTF-8"));
	}
}
