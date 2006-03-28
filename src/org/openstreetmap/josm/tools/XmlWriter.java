package org.openstreetmap.josm.tools;

import java.util.HashMap;

/**
 * Helper class to use for xml outputting classes.
 * 
 * @author imi
 */
public class XmlWriter {

	/**
	 * Encode the given string in XML1.0 format.
	 * Optimized to fast pass strings that don't need encoding (normal case).
	 */
	public static String encode(String unencoded) {
		StringBuilder buffer = null;
		for (int i = 0; i < unencoded.length(); ++i) {
			String encS = XmlWriter.encoding.get(unencoded.charAt(i));
			if (encS != null) {
				if (buffer == null)
					buffer = new StringBuilder(unencoded.substring(0,i));
				buffer.append(encS);
			} else if (buffer != null)
				buffer.append(unencoded.charAt(i));
		}
		return (buffer == null) ? unencoded : buffer.toString();
	}

	/**
	 * @return The standard XML1.0 header. Encoding is utf-8
	 */
	public static String header() {
		return "<?xml version='1.0' encoding='UTF-8'?>";
	}

	
	
	final private static HashMap<Character, String> encoding = new HashMap<Character, String>();
	static {
		encoding.put('<', "&lt;");
		encoding.put('>', "&gt;");
		encoding.put('"', "&quot;");
		encoding.put('\'', "&apos;");
		encoding.put('&', "&amp;");
		encoding.put('\n', "&#xA;");
		encoding.put('\r', "&#xD;");
		encoding.put('\t', "&#x9;");
	}
}
