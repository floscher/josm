package org.openstreetmap.josm.data;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.Map.Entry;

import org.openstreetmap.josm.tools.XmlWriter;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import uk.co.wilson.xml.MinML2;


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
		out.println(XmlWriter.header());
		out.println("<josm>");
		for (Entry<String, String> e : properties.entrySet()) {
			out.print("  <"+e.getKey());
			if (!e.getValue().equals(""))
				out.print(" value='"+XmlWriter.encode(e.getValue())+"'");
			out.println(" />");
		}
		out.println("</josm>");
		out.close();
	}


	public void load() throws IOException {
		MinML2 reader = new MinML2(){
			@Override public void startElement(String ns, String name, String qName, Attributes attr) {
				if (name.equals("josm-settings"))
					throw new RuntimeException("old version");
				String v = attr.getValue("value");
				if (!name.equals("josm"))
					properties.put(name, v == null ? "" : v);
			}
		};
		try {
			reader.parse(new FileReader(getPreferencesDir()+"preferences"));
		} catch (SAXException e) {
			e.printStackTrace();
			throw new IOException("Error in preferences file");
		}
	}

	public void resetToDefault() {
		properties.clear();
		properties.put("laf", "javax.swing.plaf.metal.MetalLookAndFeel");
		properties.put("projection", "org.openstreetmap.josm.data.projection.Epsg4263");
		properties.put("osmDataServer", "http://www.openstreetmap.org/api");
	}
}
