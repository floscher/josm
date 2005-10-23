package org.openstreetmap.josm.io;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;

import org.openstreetmap.josm.command.DataSet;

/**
 * This DataReader read directly from the REST API of the osm server.
 * 
 * @author imi
 */
public class OsmReader extends OsmConnection implements DataReader {

	/**
	 * The url string of the desired map data.
	 */
	private String urlStr;
	/**
	 * Whether importing the raw trackpoints or the regular osm map information
	 */
	private boolean rawGps;
	
	/**
	 * Construct the reader and store the information for attaching
	 */
	public OsmReader(String server, boolean rawGps, 
			double lat1, double lon1, double lat2, double lon2) {
		this.rawGps = rawGps;
		urlStr = server.endsWith("/") ? server : server+"/";
		if (rawGps)
			urlStr += "trackpoints?bbox="+lat1+","+lon1+","+lat2+","+lon2+"&page=";
		else
			urlStr += "map?bbox="+lon1+","+lat1+","+lon2+","+lat2;
	}


	public DataSet parse() throws ParseException, ConnectionException {
		Reader in;
		initAuthentication();
		try {
			if (rawGps) {
				DataSet ds = new DataSet();
				for (int i = 0;;++i) {
					URL url = new URL(urlStr+i);
					System.out.println(url);
					HttpURLConnection con = (HttpURLConnection)url.openConnection();
					con.setConnectTimeout(20000);
					if (con.getResponseCode() == 401 && isCancelled())
						return null;
					in = new InputStreamReader(con.getInputStream());
					DataSet currentData = new GpxReader(in, true).parse();
					if (currentData.nodes.isEmpty())
						return ds;
					ds.mergeFrom(currentData, true);
				}
			}
			URL url = new URL(urlStr);
			HttpURLConnection con = (HttpURLConnection)url.openConnection();
			con.setConnectTimeout(20000);
			if (con.getResponseCode() == 401 && isCancelled())
				return null;
			in = new InputStreamReader(con.getInputStream());
			return new GpxReader(in, false).parse();
		} catch (IOException e) {
			throw new ConnectionException("Failed to open server connection\n"+e.getMessage(), e);
		}
	}
}
