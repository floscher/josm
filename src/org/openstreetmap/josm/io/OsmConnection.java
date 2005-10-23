package org.openstreetmap.josm.io;

import java.awt.Font;
import java.awt.GridBagLayout;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.PasswordAuthentication;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.gui.GBC;

/**
 * Base class that handles common things like authentication for the reader and writer
 * to the osm server.
 * 
 * @author imi
 */
public class OsmConnection {
	


	/**
	 * The authentication class handling the login requests.
	 */
	private static class OsmAuth extends Authenticator {
		/**
		 * Set to true, when the autenticator tried the password once.
		 */
		boolean passwordtried = false;
		/**
		 * Whether the user cancelled the password dialog
		 */
		boolean cancelled = false;

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
	}

	/**
	 * The authenticator.
	 */
	private static OsmAuth authentication;
	
	/**
	 * Initialize the http defaults and the authenticator.
	 */
	static {
		HttpURLConnection.setFollowRedirects(true);
		Authenticator.setDefault(authentication = new OsmAuth());
	}
	
	/**
	 * Must be called before each connection attemp to initialize the authentication.
	 */
	protected final void initAuthentication() {
		authentication.cancelled = false;
		authentication.passwordtried = false;
	}
	
	/**
	 * @return Whether the connection was cancelled.
	 */
	protected final boolean isCancelled() {
		return authentication.cancelled;
	}
}
