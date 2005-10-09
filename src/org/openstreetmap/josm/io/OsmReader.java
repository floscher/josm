package org.openstreetmap.josm.io;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.PasswordAuthentication;
import java.net.URL;

import org.openstreetmap.josm.data.osm.DataSet;

/**
 * This DataReader read directly from the REST API of the osm server.
 * 
 * @author imi
 */
public class OsmReader implements DataReader {

	/**
	 * The url string of the desired map data.
	 */
	private String urlStr;

	/**
	 * Construct the reader and store the information for attaching
	 */
	public OsmReader(String server, final String username, final String password, 
			double lat1, double lon1, double lat2, double lon2) {
		urlStr = server.endsWith("/") ? server : server+"/";
		urlStr += "map?bbox="+lon1+","+lat1+","+lon2+","+lat2;
		
		HttpURLConnection.setFollowRedirects(true);
		Authenticator.setDefault(new Authenticator(){
			@Override
			protected PasswordAuthentication getPasswordAuthentication() {
				return new PasswordAuthentication(username, password.toCharArray());
			}
		});
	}


	public DataSet parse() throws ParseException, ConnectionException {
		Reader in;
		try {
			URL url = new URL(urlStr);
			HttpURLConnection con = (HttpURLConnection)url.openConnection();
			con.setDoInput(true);
			con.setConnectTimeout(20000);
			con.setRequestMethod("GET");
			con.connect();
			in = new InputStreamReader(con.getInputStream());
		} catch (IOException e) {
			throw new ConnectionException("Failed to open server connection\n"+e.getMessage(), e);
		}
		GpxReader reader = new GpxReader(in, false);
		return reader.parse();
	}
}
