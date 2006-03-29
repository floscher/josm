package org.openstreetmap.josm.data;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
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
	
	ArrayList<PreferenceChangedListener> listener = new ArrayList<PreferenceChangedListener>();
	
	/**
	 * Map the property name to the property object.
	 */
	private SortedMap<String, String> properties = new TreeMap<String, String>();
	
	/**
	 * Return the location of the preferences file
	 */
	public static String getPreferencesDir() {
		return System.getProperty("user.home")+"/.josm/";
	}


	public void addPreferenceChangedListener(PreferenceChangedListener listener) {
		this.listener.add(listener);
	}
	public void removePreferenceChangedListener(PreferenceChangedListener listener) {
		this.listener.remove(listener);
	}


	synchronized public String get(String key) {
		if (!properties.containsKey(key))
			return "";
		return properties.get(key);
	}
	synchronized public String get(String key, String def) {
		String prop = properties.get(key);
		if (prop == null || prop.equals(""))
			return def;
		return prop;
	}
	synchronized public Collection<Entry<String, String>> getAllPrefix(String prefix) {
		LinkedList<Entry<String,String>> all = new LinkedList<Entry<String,String>>();
		for (Entry<String,String> e : properties.entrySet())
			if (e.getKey().startsWith(prefix))
				all.add(e);
		return all;
	}
	synchronized public boolean getBoolean(String key) {
		return properties.containsKey(key) ? Boolean.parseBoolean(properties.get(key)) : false;
	}


	synchronized public void put(String key, String value) {
		if (value == null)
			value = "";
		properties.put(key, value);
		firePreferenceChanged(key, value);
	}
	synchronized public void put(String key, boolean value) {
		properties.put(key, Boolean.toString(value));
		firePreferenceChanged(key, Boolean.toString(value));
	}

	private void firePreferenceChanged(String key, String value) {
		for (PreferenceChangedListener l : listener)
			l.preferenceChanged(key, value);
	}


	public void save() throws IOException {
		PrintWriter out = new PrintWriter(new FileWriter(getPreferencesDir()+"preferences"));
		for (Entry<String, String> e : properties.entrySet())
			if (!e.getValue().equals(""))
				out.println(e.getKey()+"="+e.getValue());
		out.close();
	}


	public void load() throws IOException {
		properties.clear();
		BufferedReader in = new BufferedReader(new FileReader(getPreferencesDir()+"preferences"));
		int lineNumber = 0;
		for (String line = in.readLine(); line != null; line = in.readLine(), lineNumber++) {
			int i = line.indexOf('=');
			if (i == -1 || i == 0)
				throw new IOException("Malformed config file at line "+lineNumber);
			properties.put(line.substring(0,i), line.substring(i+1));
		}
	}

	public void resetToDefault() {
		properties.clear();
		properties.put("laf", "javax.swing.plaf.metal.MetalLookAndFeel");
		properties.put("projection", "org.openstreetmap.josm.data.projection.Epsg4263");
		properties.put("osm-server.url", "http://www.openstreetmap.org/api");
		properties.put("color.node", ColorHelper.color2html(Color.red));
		properties.put("color.segment", ColorHelper.color2html(SimplePaintVisitor.darkgreen));
		properties.put("color.way", ColorHelper.color2html(SimplePaintVisitor.darkblue));
		properties.put("color.incomplete way", ColorHelper.color2html(SimplePaintVisitor.darkerblue));
		properties.put("color.selected", ColorHelper.color2html(Color.white));
		properties.put("color.gps point", ColorHelper.color2html(Color.gray));
	}
}
