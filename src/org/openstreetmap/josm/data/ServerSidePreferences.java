package org.openstreetmap.josm.data;

import java.net.URL;

/**
 * This class tweak the Preferences class to provide server side preference settings, as example
 * used in the applet version.
 * 
 * @author Imi
 */
public class ServerSidePreferences extends Preferences {

	private final URL serverUrl;
	private final String userName;

	public ServerSidePreferences(URL serverUrl, String userName) {
		this.serverUrl = serverUrl;
		this.userName = userName;
    }
	
	@Override public String getPreferencesDir() {
	    return serverUrl+"/user/"+userName+"/preferences";
    }

	@Override public void load() {
		resetToDefault();
    }

	@Override protected void save() {
    }
}
