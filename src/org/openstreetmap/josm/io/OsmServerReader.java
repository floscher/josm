package org.openstreetmap.josm.io;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collection;
import java.util.LinkedList;

import org.jdom.JDOMException;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.GeoPoint;
import org.openstreetmap.josm.data.osm.DataSet;

/**
 * This DataReader read directly from the REST API of the osm server.
 * 
 * @author imi
 */
public class OsmServerReader extends OsmConnection {

	/**
	 * The boundings of the desired map data.
	 */
	private final double lat1;
	private final double lon1;
	private final double lat2;
	private final double lon2;
	
	/**
	 * Construct the reader and store the information for attaching
	 */
	public OsmServerReader(double lat1, double lon1, double lat2, double lon2) {
		this.lon2 = lon2;
		this.lat2 = lat2;
		this.lon1 = lon1;
		this.lat1 = lat1;
	}


	/**
	 * Retrieve raw gps trackpoints from the server API.
	 * @return A list of all primitives retrieved. Currently, the list of lists
	 * 		contain only one list, since the server cannot distinguish between
	 * 		tracks.
	 */
	public Collection<Collection<GeoPoint>> parseRawGps() throws IOException, JDOMException {
		String url = Main.pref.osmDataServer+"/trackpoints?bbox="+lon1+","+lat1+","+lon2+","+lat2+"&page=";
		Collection<Collection<GeoPoint>> data = new LinkedList<Collection<GeoPoint>>();
		Collection<GeoPoint> list = new LinkedList<GeoPoint>();
		
		for (int i = 0;;++i) {
			Reader r = getReader(url+i);
			if (r == null)
				break;
			RawGpsReader gpsReader = new RawGpsReader(r);
			Collection<Collection<GeoPoint>> allTracks = gpsReader.parse();
			boolean foundSomething = false;
			for (Collection<GeoPoint> t : allTracks) {
				if (!t.isEmpty()) {
					foundSomething = true;
					list.addAll(t);
				}
			}
			if (!foundSomething)
				break;
		}

		data.add(list);
		return data;
	}


	/**
	 * Read the data from the osm server address.
	 * @return A data set containing all data retrieved from that url
	 */
	public DataSet parseOsm() throws JDOMException, IOException {
		Reader r = getReader(Main.pref.osmDataServer+"/map?bbox="+lon1+","+lat1+","+lon2+","+lat2);
		if (r == null)
			return null;
		return new OsmReader(r).parse();
	}


	/**
	 * Open a connection to the given url and return a reader on the input stream
	 * from that connection. In case of user cancel, return <code>null</code>.
	 * @param url The exact url to connect to.
	 * @return An reader reading the input stream (servers answer) or <code>null</code>.
	 */
	private Reader getReader(String urlStr) throws IOException {
		initAuthentication();
		System.out.println(urlStr);
		URL url = new URL(urlStr);
		HttpURLConnection con = (HttpURLConnection)url.openConnection();
		con.setConnectTimeout(20000);
		if (con.getResponseCode() == 401 && isCancelled())
			return null;
		return new InputStreamReader(con.getInputStream());
	}
}
