package org.openstreetmap.josm.io;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import org.openstreetmap.josm.Main;

/**
 * This DataReader read directly from the REST API of the osm server.
 *
 * @author imi
 */
abstract class OsmServerReader extends OsmConnection {
	/**
	 * Open a connection to the given url and return a reader on the input stream
	 * from that connection. In case of user cancel, return <code>null</code>.
	 * @param url The exact url to connect to.
	 * @return An reader reading the input stream (servers answer) or <code>null</code>.
	 */
	protected InputStream getInputStream(String urlStr) throws IOException {
		urlStr = Main.pref.get("osm-server.url")+"/0.3/" + urlStr;
		System.out.println("download: "+urlStr);
		initAuthentication();
		URL url = new URL(urlStr);
		activeConnection = (HttpURLConnection)url.openConnection();
		System.out.println("got return: "+activeConnection.getResponseCode());
		activeConnection.setConnectTimeout(15000);
		if (isAuthCancelled() && activeConnection.getResponseCode() == 401)
			return null;
		return new ProgressInputStream(activeConnection);
	}
}
