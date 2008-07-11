// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;

public class OsmUtils {

	static ArrayList<String> TRUE_VALUES = new ArrayList<String>(Arrays
	        .asList(new String[] { "true", "yes", "1", "on" }));
	static ArrayList<String> FALSE_VALUES = new ArrayList<String>(Arrays
	        .asList(new String[] { "false", "no", "0", "off" }));

	public static final String trueval = "yes";
	public static final String falseval = "no";

	public static Boolean getOsmBoolean(String value) {
		if(value == null) return null;
		String lowerValue = value.toLowerCase(Locale.ENGLISH);
		if (TRUE_VALUES.contains(lowerValue)) return Boolean.TRUE;
		if (FALSE_VALUES.contains(lowerValue)) return Boolean.FALSE;
		return null;
	}
	public static String getNamedOsmBoolean(String value) {
		Boolean res = getOsmBoolean(value);
		return res == null ? value : (res ? trueval : falseval);
	}
}
