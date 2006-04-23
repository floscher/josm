package org.openstreetmap.josm.io;

import java.io.IOException;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collection;
import java.util.LinkedList;

import org.jdom.JDOMException;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.gui.layer.RawGpsDataLayer.GpsPoint;
import org.xml.sax.SAXException;

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
	 * Retrieve raw gps waypoints from the server API.
	 * @return A list of all primitives retrieved. Currently, the list of lists
	 * 		contain only one list, since the server cannot distinguish between
	 * 		ways.
	 */
	public Collection<Collection<GpsPoint>> parseRawGps() throws IOException, JDOMException {
		try {
			String url = Main.pref.get("osm-server.url")+"/0.3/trackpoints?bbox="+lon1+","+lat1+","+lon2+","+lat2+"&page=";
			Collection<Collection<GpsPoint>> data = new LinkedList<Collection<GpsPoint>>();
			Collection<GpsPoint> list = new LinkedList<GpsPoint>();

			for (int i = 0;;++i) {
				currentAction.setText("Downloading points "+(i*5000)+" to "+((i+1)*5000)+"...");
				Reader r = getReader(url+i);
				if (r == null)
					break;
				RawGpsReader gpsReader = new RawGpsReader(r);
				Collection<Collection<GpsPoint>> allWays = gpsReader.parse();
				boolean foundSomething = false;
				for (Collection<GpsPoint> t : allWays) {
					if (!t.isEmpty()) {
						foundSomething = true;
						list.addAll(t);
					}
				}
				if (!foundSomething)
					break;
				r.close();
				activeConnection = null;
			}

			data.add(list);
			return data;
		} catch (IOException e) {
			if (cancel)
				return null;
			throw e;
		} catch (JDOMException e) {
			throw e;
		} catch (Exception e) {
			if (cancel)
				return null;
			if (e instanceof RuntimeException)
				throw (RuntimeException)e;
			throw new RuntimeException(e);
		}
	}


	/**
	 * Read the data from the osm server address.
	 * @return A data set containing all data retrieved from that url
	 */
	public DataSet parseOsm() throws SAXException, IOException {
		try {
			final Reader r = getReader(Main.pref.get("osm-server.url")+"/0.3/map?bbox="+lon1+","+lat1+","+lon2+","+lat2);
			if (r == null)
				return null;
			currentAction.setText("Downloading OSM data...");
			final DataSet data = OsmReader.parseDataSet(r);
			r.close();
			activeConnection = null;
			return data;
		} catch (IOException e) {
			if (cancel)
				return null;
			throw e;
		} catch (SAXException e) {
			throw e;
		} catch (Exception e) {
			if (cancel)
				return null;
			if (e instanceof RuntimeException)
				throw (RuntimeException)e;
			throw new RuntimeException(e);
		}
	}


	/**
	 * Open a connection to the given url and return a reader on the input stream
	 * from that connection. In case of user cancel, return <code>null</code>.
	 * @param url The exact url to connect to.
	 * @return An reader reading the input stream (servers answer) or <code>null</code>.
	 */
	private Reader getReader(String urlStr) throws IOException {
		System.out.println("download: "+urlStr);
		initAuthentication();
		URL url = new URL(urlStr);
		activeConnection = (HttpURLConnection)url.openConnection();
		activeConnection.setConnectTimeout(15000);
		if (isAuthCancelled() && activeConnection.getResponseCode() == 401)
			return null;
		return new ProgressReader(activeConnection, progress);
	}
}
