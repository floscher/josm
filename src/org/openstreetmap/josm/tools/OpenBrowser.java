package org.openstreetmap.josm.tools;

import java.io.IOException;

/**
 * Helper to open platform web browser on different platforms
 * @author Imi
 */
public class OpenBrowser {

	/**
	 * @return <code>null</code> for success or a string in case of an error.
	 */
	public static String displayUrl(String url) {
		String os = System.getProperty("os.name");
		try {
			if (os != null && os.startsWith("Windows"))
				Runtime.getRuntime().exec("rundll32 url.dll,FileProtocolHandler " + url);
			else {
				//...
			}
		} catch (IOException e) {
			return e.getMessage();
		}
		return null;
	}
}
