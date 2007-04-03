package org.openstreetmap.josm.data;

import static org.xnap.commons.i18n.I18n.marktr;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Map;
import java.util.SortedMap;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.Map.Entry;

import org.openstreetmap.josm.Main;
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

	/**
	 * Class holding one bookmarkentry.
	 * @author imi
	 */
	public static class Bookmark {
		public String name;
		public double[] latlon = new double[4]; // minlat, minlon, maxlat, maxlon
		@Override public String toString() {
			return name;
		}
	}

	public final ArrayList<PreferenceChangedListener> listener = new ArrayList<PreferenceChangedListener>();

	/**
	 * Map the property name to the property object.
	 */
	protected final SortedMap<String, String> properties = new TreeMap<String, String>();

	/**
	 * Return the location of the preferences file
	 */
	public String getPreferencesDir() {
		if (System.getenv("APPDATA") != null)
			return System.getenv("APPDATA")+"/JOSM/";
		return System.getProperty("user.home")+"/.josm/";
	}

	synchronized public boolean hasKey(final String key) {
		return properties.containsKey(key);
	}
	synchronized public String get(final String key) {
		if (!properties.containsKey(key))
			return "";
		return properties.get(key);
	}
	synchronized public String get(final String key, final String def) {
		final String prop = properties.get(key);
		if (prop == null || prop.equals(""))
			return def;
		return prop;
	}
	synchronized public Map<String, String> getAllPrefix(final String prefix) {
		final Map<String,String> all = new TreeMap<String,String>();
		for (final Entry<String,String> e : properties.entrySet())
			if (e.getKey().startsWith(prefix))
				all.put(e.getKey(), e.getValue());
		return all;
	}
	synchronized public boolean getBoolean(final String key) {
		return getBoolean(key, false);
	}
	synchronized public boolean getBoolean(final String key, final boolean def) {
		return properties.containsKey(key) ? Boolean.parseBoolean(properties.get(key)) : def;
	}


	synchronized public void put(final String key, final String value) {
		if (value == null)
			properties.remove(key);
		else
			properties.put(key, value);
		save();
		firePreferenceChanged(key, value);
	}
	synchronized public void put(final String key, final boolean value) {
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
			final PrintWriter out = new PrintWriter(new FileWriter(getPreferencesDir() + "preferences"), false);
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
		properties.put("propertiesdialog.visible", "true");
		properties.put("osm-server.url", "http://www.openstreetmap.org/api");
		properties.put("color."+marktr("background"), ColorHelper.color2html(Color.black));
		properties.put("color."+marktr("node"), ColorHelper.color2html(Color.red));
		properties.put("color."+marktr("segment"), ColorHelper.color2html(SimplePaintVisitor.darkgreen));
		properties.put("color."+marktr("way"), ColorHelper.color2html(SimplePaintVisitor.darkblue));
		properties.put("color."+marktr("incomplete way"), ColorHelper.color2html(SimplePaintVisitor.darkerblue));
		properties.put("color."+marktr("selected"), ColorHelper.color2html(Color.white));
		properties.put("color."+marktr("gps point"), ColorHelper.color2html(Color.gray));
		properties.put("color."+marktr("conflict"), ColorHelper.color2html(Color.gray));
		properties.put("color."+marktr("scale"), ColorHelper.color2html(Color.white));
		save();
	}

	public Collection<Bookmark> loadBookmarks() throws IOException {
		File bookmarkFile = new File(getPreferencesDir()+"bookmarks");
		if (!bookmarkFile.exists())
			bookmarkFile.createNewFile();
		BufferedReader in = new BufferedReader(new FileReader(bookmarkFile));

		Collection<Bookmark> bookmarks = new LinkedList<Bookmark>();
		for (String line = in.readLine(); line != null; line = in.readLine()) {
			StringTokenizer st = new StringTokenizer(line, ",");
			if (st.countTokens() < 5)
				continue;
			Bookmark b = new Bookmark();
			b.name = st.nextToken();
			try {
				for (int i = 0; i < b.latlon.length; ++i)
					b.latlon[i] = Double.parseDouble(st.nextToken());
				bookmarks.add(b);
			} catch (NumberFormatException x) {
				// line not parsed
			}
		}
		in.close();
		return bookmarks;
	}

	public void saveBookmarks(Collection<Bookmark> bookmarks) throws IOException {
		File bookmarkFile = new File(Main.pref.getPreferencesDir()+"bookmarks");
		if (!bookmarkFile.exists())
			bookmarkFile.createNewFile();
		PrintWriter out = new PrintWriter(new FileWriter(bookmarkFile));
		for (Bookmark b : bookmarks) {
			b.name.replace(',', '_');
			out.print(b.name+",");
			for (int i = 0; i < b.latlon.length; ++i)
				out.print(b.latlon[i]+(i<b.latlon.length-1?",":""));
			out.println();
		}
		out.close();
	}
}
