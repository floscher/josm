package org.openstreetmap.josm.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.LinkedList;

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
		private String currentTime;

		@Override public void startElement(String namespaceURI, String localName, String qName, Attributes atts) throws SAXException {
			if (qName.equals("wpt") || qName.equals("trkpt")) {
				try {
	                double lat = Double.parseDouble(atts.getValue("lat"));
	                double lon = Double.parseDouble(atts.getValue("lon"));
	        		if (Math.abs(lat) > 90)
	        			throw new SAXException("Data error: lat value '"+lat+"' is out of bound.");
	        		if (Math.abs(lon) > 180)
	        			throw new SAXException("Data error: lon value '"+lon+"' is out of bound.");
	                currentLatLon = new LatLon(lat, lon);
                } catch (NumberFormatException e) {
                	e.printStackTrace();
	                throw new SAXException(e);
                }
			} else if (qName.equals("time")) {
				currentTime = "";
			}
		}

		@Override public void characters(char[] ch, int start, int length) {
			if (currentTime != null && currentTime.equals(""))
				currentTime = new String(ch, start, length);
		}

		@Override public void endElement(String namespaceURI, String localName, String qName) {
			if (qName.equals("wpt") || qName.equals("trkpt")) {
				current.add(new GpsPoint(currentLatLon, currentTime));
			} else if (qName.equals("trkseg") || qName.equals("trk") || qName.equals("gpx"))
				newTrack();
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
