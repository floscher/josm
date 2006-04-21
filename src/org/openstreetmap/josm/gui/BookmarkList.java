package org.openstreetmap.josm.gui;

import java.awt.Component;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.StringTokenizer;

import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.Preferences;
import org.openstreetmap.josm.tools.ImageProvider;

/**
 * List class that read and save its content from the bookmark file.
 * @author imi
 */
public class BookmarkList extends JList {

	/**
	 * Class holding one bookmarkentry.
	 * @author imi
	 */
	public static class Bookmark {
		public String name;
		public double[] latlon = new double[4]; // minlat, minlon, maxlat, maxlon
		public boolean rawgps;
		@Override public String toString() {
			return name;
		}
	}
	
	/**
	 * Create a bookmark list as well as the Buttons add and remove.
	 */
	public BookmarkList() {
		setModel(new DefaultListModel());
		load();
		setVisibleRowCount(7);
		setCellRenderer(new DefaultListCellRenderer(){
			@Override public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
				Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
				if (c instanceof JLabel) {
					Bookmark b = (Bookmark)value;
					((JLabel)c).setIcon(ImageProvider.get("layer/"+(b.rawgps?"rawgps_small":"osmdata_small")));
				}
				return c;
			}
		});
	}

	/**
	 * Loads the bookmarks from file.
	 */
	public void load() {
		DefaultListModel model = (DefaultListModel)getModel();
		model.removeAllElements();
		File bookmarkFile = new File(Preferences.getPreferencesDir()+"bookmarks");
		try {
			if (!bookmarkFile.exists())
				bookmarkFile.createNewFile();
			BufferedReader in = new BufferedReader(new FileReader(bookmarkFile));
			
			for (String line = in.readLine(); line != null; line = in.readLine()) {
				StringTokenizer st = new StringTokenizer(line, ",");
				if (st.countTokens() != 6)
					continue;
				Bookmark b = new Bookmark();
				b.name = st.nextToken();
				try {
					for (int i = 0; i < b.latlon.length; ++i)
						b.latlon[i] = Double.parseDouble(st.nextToken());
					b.rawgps = Boolean.parseBoolean(st.nextToken());
					model.addElement(b);
				} catch (NumberFormatException x) {
					// line not parsed
				}
			}
			in.close();
		} catch (IOException e) {
			JOptionPane.showMessageDialog(Main.main, "Could not read bookmarks.\n"+e.getMessage());
		}
	}

	/**
	 * Save all bookmarks to the preferences file
	 */
	public void save() {
		File bookmarkFile = new File(Preferences.getPreferencesDir()+"bookmarks");
		try {
			if (!bookmarkFile.exists())
				bookmarkFile.createNewFile();
			PrintWriter out = new PrintWriter(new FileWriter(bookmarkFile));
			DefaultListModel m = (DefaultListModel)getModel();
			for (Object o : m.toArray()) {
				Bookmark b = (Bookmark)o;
				b.name.replace(',', '_');
				out.print(b.name+",");
				for (int i = 0; i < b.latlon.length; ++i)
					out.print(b.latlon[i]+",");
				out.println(b.rawgps);
			}
			out.close();
		} catch (IOException e) {
			JOptionPane.showMessageDialog(Main.main, "Could not write bookmark.\n"+e.getMessage());
		}
	}
}
