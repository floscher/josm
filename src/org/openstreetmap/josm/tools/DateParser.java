package org.openstreetmap.josm.tools;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Tries to parse a date as good as it can.
 * 
 * @author Immanuel.Scholz
 */
public class DateParser {

	private static final String[] formats = {
		"MM/dd/yyyy HH:mm:ss",
		"MM/dd/yyyy'T'HH:mm:ss.SSSZ",
		"MM/dd/yyyy'T'HH:mm:ss.SSS",
		"MM/dd/yyyy'T'HH:mm:ssZ",
		"MM/dd/yyyy'T'HH:mm:ss",
	};
	
	public static Date parse(String d) throws ParseException {
		for (String parse : formats) {
			SimpleDateFormat sdf = new SimpleDateFormat(parse);
			try {return sdf.parse(d);} catch (ParseException pe) {}
		}
		throw new ParseException("No applicable parse format", 0);
	}
}
