package org.openstreetmap.josm.tools;

import static org.openstreetmap.josm.tools.I18n.tr;

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
		"yyyy-MM-dd'T'HH:mm:ss'Z'",
		"yyyy-MM-dd'T'HH:mm:ssZ",
		"yyyy-MM-dd'T'HH:mm:ss.SSSZ",
		"yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
		"yyyy-MM-dd HH:mm:ss",
		"MM/dd/yyyy HH:mm:ss",
		"MM/dd/yyyy'T'HH:mm:ss.SSSZ",
		"MM/dd/yyyy'T'HH:mm:ss.SSS'Z'",
		"MM/dd/yyyy'T'HH:mm:ss.SSS",
		"MM/dd/yyyy'T'HH:mm:ssZ",
		"MM/dd/yyyy'T'HH:mm:ss",
		"yyyy:MM:dd HH:mm:ss", // unfcklvble, but I have seen this...
	};
	
	public static Date parse(String d) throws ParseException {
		for (String parse : formats) {
			SimpleDateFormat sdf = new SimpleDateFormat(parse);
			try {return sdf.parse(d);} catch (ParseException pe) {}
		}
		throw new ParseException(tr("No applicable parse format"), 0);
	}
}
