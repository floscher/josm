package org.openstreetmap.josm.io;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Stack;

import org.openstreetmap.josm.data.coor.LatLon;
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

	private static class Parser extends MinML2 {
		/**
		 * Current track to be read. The last entry is the current trkpt.
		 * If in wpt-mode, it contain only one GpsPoint.
		 */
		private Collection<GpsPoint> current = new LinkedList<GpsPoint>();
		public Collection<Collection<GpsPoint>> data = new LinkedList<Collection<GpsPoint>>();
		private LatLon currentLatLon;
		private String currentTime = null;
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
			}
			tags.push(qName);
		}

		@Override public void characters(char[] ch, int start, int length) {
			if (tags.peek().equals("time")) {
				String time = tags.pop();
				if (tags.empty() || (!tags.peek().equals("wpt") && !tags.peek().equals("trkpt"))) {
					tags.push(time);
					return;
				}
				String ct = new String(ch, start, length);
				currentTime += ct;
				tags.push(time);
			}
		}

		@Override public void endElement(String namespaceURI, String localName, String qName) {
			if (qName.equals("wpt") || qName.equals("trkpt")) {
				current.add(new GpsPoint(currentLatLon, currentTime));
			} else if (qName.equals("trkseg") || qName.equals("trk") || qName.equals("gpx"))
				newTrack();
			
			if (!qName.equals("time"))
				currentTime = "";
			tags.pop();
        }

		private void newTrack() {
			if (!current.isEmpty()) {
				data.add(current);
				current = new LinkedList<GpsPoint>();
			}
		}
	}

	/**
	 * Parse and return the read data
	 */
	public static Collection<Collection<GpsPoint>> parse(InputStream source) throws SAXException, IOException {
		Parser parser = new Parser();
		parser.parse(new InputStreamReader(source, "UTF-8"));
		return parser.data;
	}
}
