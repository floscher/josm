package org.openstreetmap.josm.io;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.StringTokenizer;

import org.jdom.JDOMException;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.gui.layer.RawGpsDataLayer.GpsPoint;

/**
 * Read raw information from a csv style file (as defined in the preferences).
 * @author imi
 */
public class RawCsvReader {

	/**
	 * Reader to read the input from.
	 */
	private BufferedReader in;

	public RawCsvReader(Reader in) {
		this.in = new BufferedReader(in);
	}
	
	public Collection<GpsPoint> parse() throws JDOMException, IOException {
		Collection<GpsPoint> data = new LinkedList<GpsPoint>();
		String formatStr = Main.pref.get("csvImportString");
		if (formatStr == null)
			formatStr = in.readLine();
		if (formatStr == null)
			throw new JDOMException("Could not detect data format string.");
		
		// get delimiter
		String delim = ",";
		for (int i = 0; i < formatStr.length(); ++i) {
			if (!Character.isLetterOrDigit(formatStr.charAt(i))) {
				delim = ""+formatStr.charAt(i);
				break;
			}
		}
		
		// convert format string
		ArrayList<String> format = new ArrayList<String>();
		for (StringTokenizer st = new StringTokenizer(formatStr, delim); st.hasMoreTokens();)
			format.add(st.nextToken());

		// test for completness
		if (!format.contains("lat") || !format.contains("lon")) {
			if (Main.pref.get("csvImportString").equals(""))
				throw new JDOMException("Format string in data is incomplete or not found. Try setting an manual format string in Preferences.");
			throw new JDOMException("Format string is incomplete. Need at least 'lat' and 'lon' specification");
		}
		
		int lineNo = 0;
		try {
			for (String line = in.readLine(); line != null; line = in.readLine()) {
				lineNo++;
				StringTokenizer st = new StringTokenizer(line, delim);
				double lat = 0, lon = 0;
				String time = null;
				for (String token : format) {
					if (token.equals("lat"))
						lat = Double.parseDouble(st.nextToken());
					else if (token.equals("lon"))
						lon = Double.parseDouble(st.nextToken());
					else if (token.equals("time"))
						time = (time == null?"":(time+" ")) + st.nextToken();
					else if (token.equals("ignore"))
						st.nextToken();
					else
						throw new JDOMException("Unknown data type: '"+token+"'."+(Main.pref.get("csvImportString").equals("") ? " Maybe add an format string in preferences." : ""));
				}
				data.add(new GpsPoint(new LatLon(lat, lon), time));
			}
		} catch (RuntimeException e) {
			throw new JDOMException("Parsing error in line "+lineNo, e);
		}
		return data;
	}
}
