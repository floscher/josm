package org.openstreetmap.josm.io;

import java.awt.Font;
import java.awt.GridBagLayout;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.PasswordAuthentication;
import java.net.URL;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.gui.GBC;
import org.openstreetmap.josm.gui.Main;

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
	 * Whether importing the raw trackpoints or the regular osm map information
	 */
	private boolean rawGps;
	/**
	 * Whether the user cancelled the password dialog
	 */
	private boolean cancelled = false;
	/**
	 * Set to true, when the autenticator tried the password once.
	 */
	private boolean passwordtried = false;

	/**
	 * Construct the reader and store the information for attaching
	 */
	public OsmReader(String server, boolean rawGps, 
			double lat1, double lon1, double lat2, double lon2) {
		this.rawGps = rawGps;
		urlStr = server.endsWith("/") ? server : server+"/";
		urlStr += rawGps?"trackpoints" : "map";
		urlStr += "?bbox="+lon1+","+lat1+","+lon2+","+lat2;
		if (rawGps)
			urlStr += "&page=";
		
		HttpURLConnection.setFollowRedirects(true);
		Authenticator.setDefault(new Authenticator(){
			@Override
			protected PasswordAuthentication getPasswordAuthentication() {
				String username = Main.pref.osmDataUsername;
				String password = Main.pref.osmDataPassword;
				if (passwordtried || "".equals(username) || password == null || "".equals(password)) {
					JPanel p = new JPanel(new GridBagLayout());
					p.add(new JLabel("Username"), GBC.std().insets(0,0,10,0));
					JTextField usernameField = new JTextField("".equals(username) ? "" : username, 20);
					p.add(usernameField, GBC.eol());
					p.add(new JLabel("Password"), GBC.std().insets(0,0,10,0));
					JPasswordField passwordField = new JPasswordField(password == null ? "" : password, 20);
					p.add(passwordField, GBC.eol());
					JLabel warning = new JLabel("Warning: The password is transferred unencrypted.");
					warning.setFont(warning.getFont().deriveFont(Font.ITALIC));
					p.add(warning, GBC.eol());
					int choice = JOptionPane.showConfirmDialog(Main.main, p, "Enter Password", JOptionPane.OK_CANCEL_OPTION);
					if (choice == JOptionPane.CANCEL_OPTION) {
						cancelled = true;
						return null;
					}
					username = usernameField.getText();
					password = String.valueOf(passwordField.getPassword());
					if ("".equals(username))
						return null;
				}
				passwordtried = true;
				return new PasswordAuthentication(username, password.toCharArray());
			}
		});
	}


	public DataSet parse() throws ParseException, ConnectionException {
		Reader in;
		try {
			if (rawGps) {
				DataSet ds = new DataSet();
				for (int i = 0;;++i) {
					URL url = new URL(urlStr+i);
					HttpURLConnection con = (HttpURLConnection)url.openConnection();
					con.setConnectTimeout(20000);
					if (con.getResponseCode() == 401 && cancelled)
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
			if (con.getResponseCode() == 401 && cancelled)
				return null;
			in = new InputStreamReader(con.getInputStream());
			return new GpxReader(in, false).parse();
		} catch (IOException e) {
			throw new ConnectionException("Failed to open server connection\n"+e.getMessage(), e);
		}
	}
}
