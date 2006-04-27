package org.openstreetmap.josm.data;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.Map.Entry;

import org.openstreetmap.josm.data.osm.visitor.SimplePaintVisitor;
import org.openstreetmap.josm.tools.ColorHelper;


/**
 * This class holds all preferences for JOSM.
 * 
 * Other classes can register their beloved properties here. All properties will be
 * saved upon set-access.
 * 
 * @author imi
 */
public class Preferences {

	public static interface PreferenceChangedListener {
		void preferenceChanged(String key, String newValue);
	}
	
	public final ArrayList<PreferenceChangedListener> listener = new ArrayList<PreferenceChangedListener>();
	
	/**
	 * Map the property name to the property object.
	 */
	private final SortedMap<String, String> properties = new TreeMap<String, String>();
	
	/**
	 * Return the location of the preferences file
	 */
	public String getPreferencesDir() {
		return System.getProperty("user.home")+"/.josm/";
	}

	synchronized final public String get(final String key) {
		if (!properties.containsKey(key))
			return "";
		return properties.get(key);
	}
	synchronized final public String get(final String key, final String def) {
		final String prop = properties.get(key);
		if (prop == null || prop.equals(""))
			return def;
		return prop;
	}
	synchronized final public Map<String, String> getAllPrefix(final String prefix) {
		final Map<String,String> all = new TreeMap<String,String>();
		for (final Entry<String,String> e : properties.entrySet())
			if (e.getKey().startsWith(prefix))
				all.put(e.getKey(), e.getValue());
		return all;
	}
	synchronized final public boolean getBoolean(final String key) {
		return getBoolean(key, false);
	}
	synchronized final public boolean getBoolean(final String key, final boolean def) {
		return properties.containsKey(key) ? Boolean.parseBoolean(properties.get(key)) : def;
	}


	synchronized final public void put(final String key, final String value) {
		if (value == null)
			properties.remove(key);
		else
			properties.put(key, value);
		save();
		firePreferenceChanged(key, value);
	}
	synchronized final public void put(final String key, final boolean value) {
		properties.put(key, Boolean.toString(value));
		save();
		firePreferenceChanged(key, Boolean.toString(value));
	}

	private final void firePreferenceChanged(final String key, final String value) {
		for (final PreferenceChangedListener l : listener)
			l.preferenceChanged(key, value);
	}


	/**
	 * Called after every put. In case of a problem, do nothing but output the error
	 * in log.
	 */
	protected void save() {
		try {
			final PrintWriter out = new PrintWriter(new FileWriter(
					getPreferencesDir() + "preferences"));
			for (final Entry<String, String> e : properties.entrySet())
				if (!e.getValue().equals(""))
					out.println(e.getKey() + "=" + e.getValue());
			out.close();
		} catch (final IOException e) {
			e.printStackTrace();
			// do not message anything, since this can be called from strange
			// places.
		}		
	}


	public void load() throws IOException {
		properties.clear();
		final BufferedReader in = new BufferedReader(new FileReader(getPreferencesDir()+"preferences"));
		int lineNumber = 0;
		for (String line = in.readLine(); line != null; line = in.readLine(), lineNumber++) {
			final int i = line.indexOf('=');
			if (i == -1 || i == 0)
				throw new IOException("Malformed config file at line "+lineNumber);
			properties.put(line.substring(0,i), line.substring(i+1));
		}
	}

	public final void resetToDefault() {
		properties.clear();
		properties.put("laf", "javax.swing.plaf.metal.MetalLookAndFeel");
		properties.put("projection", "org.openstreetmap.josm.data.projection.Epsg4326");
		properties.put("osm-server.url", "http://www.openstreetmap.org/api");
		properties.put("color.background", ColorHelper.color2html(Color.black));
		properties.put("color.node", ColorHelper.color2html(Color.red));
		properties.put("color.segment", ColorHelper.color2html(SimplePaintVisitor.darkgreen));
		properties.put("color.way", ColorHelper.color2html(SimplePaintVisitor.darkblue));
		properties.put("color.incomplete way", ColorHelper.color2html(SimplePaintVisitor.darkerblue));
		properties.put("color.selected", ColorHelper.color2html(Color.white));
		properties.put("color.gps point", ColorHelper.color2html(Color.gray));
		properties.put("color.conflict", ColorHelper.color2html(Color.gray));
		save();
	}
}
