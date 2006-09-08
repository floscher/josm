//Licence: GPL
package org.openstreetmap.josm.gui;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Toolkit;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.tools.BugReportExceptionHandler;
/**
 * Main window class application.
 *  
 * @author imi
 */
public class MainApplication extends Main {
	/**
	 * Construct an main frame, ready sized and operating. Does not 
	 * display the frame.
	 */
	public MainApplication(JFrame mainFrame) {
		mainFrame.setContentPane(contentPane);
		mainFrame.setJMenuBar(mainMenu);
		mainFrame.setBounds(bounds);
		mainFrame.addWindowListener(new WindowAdapter(){
			@Override public void windowClosing(final WindowEvent arg0) {
				if (Main.map != null) {
					boolean modified = false;
					boolean uploadedModified = false;
					for (final Layer l : Main.map.mapView.getAllLayers()) {
						if (l instanceof OsmDataLayer && ((OsmDataLayer)l).isModified()) {
							modified = true;
							uploadedModified = ((OsmDataLayer)l).uploadedModified;
							break;
						}
					}
					if (modified) {
						final String msg = uploadedModified ? "\n"+tr("Hint: Some changes came from uploading new data to the server.") : "";
						final int answer = JOptionPane.showConfirmDialog(
								Main.parent, tr("There are unsaved changes. Really quit?")+msg,
								tr("Unsaved Changes"), JOptionPane.YES_NO_OPTION);
						if (answer != JOptionPane.YES_OPTION)
							return;
					}
				}
				System.exit(0);
			}
		});
		mainFrame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
	}

	/**
	 * Main application Startup
	 * @param args	No parameters accepted.
	 */
	public static void main(final String[] argArray) {
		/////////////////////////////////////////////////////////////////////////
		//                        TO ALL TRANSLATORS
		/////////////////////////////////////////////////////////////////////////
		// Do not translate the early strings below until the locale is set up.
		// The cannot be translated. That's live. Really. Sorry.
		//
		// The next sending me a patch translating these strings owe me a beer!
		//
		//                                                                 Imi.
		/////////////////////////////////////////////////////////////////////////
		
		Thread.setDefaultUncaughtExceptionHandler(new BugReportExceptionHandler());

		// construct argument table
		List<String> argList = Arrays.asList(argArray);
		Map<String, Collection<String>> args = new HashMap<String, Collection<String>>();
		for (String arg : argArray) {
			if (!arg.startsWith("--"))
				arg = "--download="+arg;
			int i = arg.indexOf('=');
			String key = i == -1 ? arg.substring(2) : arg.substring(2,i);
			String value = i == -1 ? "" : arg.substring(i+1);
			Collection<String> v = args.get(key);
			if (v == null)
				v = new LinkedList<String>();
			v.add(value);
			args.put(key, v);
		}

		// get the preferences.
		final File prefDir = new File(Main.pref.getPreferencesDir());
		if (prefDir.exists() && !prefDir.isDirectory()) {
			JOptionPane.showMessageDialog(null, "Cannot open preferences directory: "+Main.pref.getPreferencesDir());
			return;
		}
		if (!prefDir.exists())
			prefDir.mkdirs();
		try {
			if (args.containsKey("reset-preferences")) {
				Main.pref.resetToDefault();
			} else {
				Main.pref.load();
			}
		} catch (final IOException e1) {
			e1.printStackTrace();
			JOptionPane.showMessageDialog(null, "Preferences could not be loaded. Writing default preference file to "+pref.getPreferencesDir()+"preferences");
			Main.pref.resetToDefault();
		}

		// setup the locale
		if (args.containsKey("language") && !args.get("language").isEmpty() && args.get("language").iterator().next().length() >= 2) {
			String s = args.get("language").iterator().next();
			Locale l = null;
			if (s.length() <= 2 || s.charAt(2) != '_')
				l = new Locale(s);
			else if (s.length() <= 5 || s.charAt(5) != '.')
				l = new Locale(s.substring(0,2), s.substring(3));
			else
				l = new Locale(s.substring(0,2), s.substring(3,5), s.substring(6));
			Locale.setDefault(l);
		} else if (!Main.pref.get("language").equals("")) {
			String lang = Main.pref.get("language");
			for (Locale l : Locale.getAvailableLocales()) {
				if (l.toString().equals(lang)) {
					Locale.setDefault(l);
					break;
				}
			}
		}
		
		// Locale is set. From now on, tr(), trn() and trc() may be called.

		if (argList.contains("--help") || argList.contains("-?") || argList.contains("-h")) {
			System.out.println(tr("Java OpenStreetMap Editor")+"\n\n"+
					tr("usage")+":\n"+
					"\tjava -jar josm.jar <option> <option> <option>...\n\n"+
					tr("options")+":\n"+
					"\t--help|-?|-h                              "+tr("Show this help")+"\n"+
					"\t--geometry=widthxheight(+|-)x(+|-)y       "+tr("Standard unix geometry argument")+"\n"+
					"\t[--download=]minlat,minlon,maxlat,maxlon  "+tr("Download the bounding box")+"\n"+
					"\t[--download=]<url>                        "+tr("Download the location at the url (with lat=x&lon=y&zoom=z)")+"\n"+
					"\t[--download=]<filename>                   "+tr("Open file (as raw gps, if .gpx or .csv)")+"\n"+
					"\t--downloadgps=minlat,minlon,maxlat,maxlon "+tr("Download the bounding box as raw gps")+"\n"+
					"\t--selection=<searchstring>                "+tr("Select with the given search")+"\n"+
					"\t--no-fullscreen                           "+tr("Don't launch in fullscreen mode")+"\n"+
					"\t--reset-preferences                       "+tr("Reset the preferences to default")+"\n\n"+
					"\t--language=<language>                     "+tr("Set the language. Example: ")+"\n\n"+
					tr("examples")+":\n"+
					"\tjava -jar josm.jar track1.gpx track2.gpx london.osm\n"+
					"\tjava -jar josm.jar http://www.openstreetmap.org/index.html?lat=43.2&lon=11.1&zoom=13\n"+
					"\tjava -jar josm.jar london.osm --selection=http://www.ostertag.name/osm/OSM_errors_node-duplicate.xml\n"+
					"\tjava -jar josm.jar 43.2,11.1,43.4,11.4\n\n"+

					tr("Parameters are read in the order they are specified, so make sure you load\n"+
					"some data before --selection")+"\n\n"+
					tr("Instead of --download=<bbox> you may specify osm://<bbox>\n"));
			System.exit(0);
		}

		preConstructorInit(args);
		JFrame mainFrame = new JFrame(tr("Java Open Street Map - Editor"));
		Main.parent = mainFrame;
		Main main = new MainApplication(mainFrame);

		mainFrame.setVisible(true);

		if (!args.containsKey("no-fullscreen") && !args.containsKey("geometry") && Toolkit.getDefaultToolkit().isFrameStateSupported(JFrame.MAXIMIZED_BOTH))
			mainFrame.setExtendedState(JFrame.MAXIMIZED_BOTH);

		main.postConstructorProcessCmdLine(args);
	}
}
