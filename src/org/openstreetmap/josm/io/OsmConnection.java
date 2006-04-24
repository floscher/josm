package org.openstreetmap.josm.io;

import java.awt.Font;
import java.awt.GridBagLayout;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.PasswordAuthentication;

import javax.swing.BoundedRangeModel;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.tools.GBC;

/**
 * Base class that handles common things like authentication for the reader and writer
 * to the osm server.
 * 
 * @author imi
 */
public class OsmConnection {

	protected boolean cancel = false;
	protected HttpURLConnection activeConnection;
	protected JLabel currentAction;
	protected BoundedRangeModel progress;
	
	private static OsmAuth authentication;
	/**
	 * Initialize the http defaults and the authenticator.
	 */
	static {
		HttpURLConnection.setFollowRedirects(true);
		Authenticator.setDefault(authentication = new OsmAuth());
	}
	
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
    	boolean authCancelled = false;
    
    	@Override protected PasswordAuthentication getPasswordAuthentication() {
    		String username = Main.pref.get("osm-server.username");
    		String password = Main.pref.get("osm-server.password");
    		if (passwordtried || username.equals("") || password.equals("")) {
    			JPanel p = new JPanel(new GridBagLayout());
    			p.add(new JLabel("Username"), GBC.std().insets(0,0,10,0));
    			JTextField usernameField = new JTextField(username, 20);
    			p.add(usernameField, GBC.eol());
    			p.add(new JLabel("Password"), GBC.std().insets(0,0,10,0));
    			JPasswordField passwordField = new JPasswordField(password, 20);
    			p.add(passwordField, GBC.eol());
    			JLabel warning = new JLabel("Warning: The password is transferred unencrypted.");
    			warning.setFont(warning.getFont().deriveFont(Font.ITALIC));
    			p.add(warning, GBC.eol());
    			int choice = JOptionPane.showConfirmDialog(Main.parent, p, "Enter Password", JOptionPane.OK_CANCEL_OPTION);
    			if (choice == JOptionPane.CANCEL_OPTION) {
    				authCancelled = true;
    				return null;
    			}
    			username = usernameField.getText();
    			password = String.valueOf(passwordField.getPassword());
    			if (username.equals(""))
    				return null;
    		}
    		passwordtried = true;
    		return new PasswordAuthentication(username, password.toCharArray());
    	}
    }

	/**
	 * Must be called before each connection attemp to initialize the authentication.
	 */
	protected final void initAuthentication() {
		authentication.authCancelled = false;
		authentication.passwordtried = false;
	}
	
	/**
	 * @return Whether the connection was cancelled.
	 */
	protected final boolean isAuthCancelled() {
		return authentication.authCancelled;
	}

	public void setProgressInformation(JLabel currentAction, BoundedRangeModel progress) {
		this.currentAction = currentAction;
		this.progress = progress;
    }

	public void cancel() {
		currentAction.setText("Aborting...");
    	cancel = true;
    	if (activeConnection != null) {
    		activeConnection.setConnectTimeout(1);
    		activeConnection.setReadTimeout(1);
    		activeConnection.disconnect();
    	}
    }
}
