//Licence: GPL
package org.openstreetmap.josm.gui;

import java.awt.Toolkit;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
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
						final String msg = uploadedModified ? "\nHint: Some changes came from uploading new data to the server." : "";
						final int answer = JOptionPane.showConfirmDialog(
								Main.parent, "There are unsaved changes. Really quit?"+msg,
								"Unsaved Changes", JOptionPane.YES_NO_OPTION);
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
		Thread.setDefaultUncaughtExceptionHandler(new BugReportExceptionHandler());

		List<String> argList = Arrays.asList(argArray);
		if (argList.contains("--help") || argList.contains("-?") || argList.contains("-h")) {
			System.out.println("Java OpenStreetMap Editor");
			System.out.println();
			System.out.println("usage:");
			System.out.println("\tjava -jar josm.jar <option> <option> <option>...");
			System.out.println();
			System.out.println("options:");
			System.out.println("\t--help|-?|-h                              Show this help");
			System.out.println("\t--geometry=widthxheight(+|-)x(+|-)y       Standard unix geometry argument");
			System.out.println("\t[--download=]minlat,minlon,maxlat,maxlon  Download the bounding box");
			System.out.println("\t[--download=]<url>                        Download the location at the url (with lat=x&lon=y&zoom=z)");
			System.out.println("\t[--download=]<filename>                   Open file (as raw gps, if .gpx or .csv)");
			System.out.println("\t--downloadgps=minlat,minlon,maxlat,maxlon Download the bounding box as raw gps");
			System.out.println("\t--selection=<searchstring>                Select with the given search");
			System.out.println("\t--no-fullscreen                           Don't launch in fullscreen mode");
			System.out.println("\t--reset-preferences                       Reset the preferences to default");
			System.out.println();
			System.out.println("examples:");
			System.out.println("\tjava -jar josm.jar track1.gpx track2.gpx london.osm");
			System.out.println("\tjava -jar josm.jar http://www.openstreetmap.org/index.html?lat=43.2&lon=11.1&zoom=13");
			System.out.println("\tjava -jar josm.jar london.osm --selection=http://www.ostertag.name/osm/OSM_errors_node-duplicate.xml");
			System.out.println("\tjava -jar josm.jar 43.2,11.1,43.4,11.4");
			System.out.println();
			System.out.println("Parameters are read in the order they are specified, so make sure you load");
			System.out.println("some data before --selection");
			System.out.println();
			System.out.println("Instead of --download=<bbox> you may specify osm://<bbox>");
			System.exit(0);
		}

		final File prefDir = new File(Main.pref.getPreferencesDir());
		if (prefDir.exists() && !prefDir.isDirectory()) {
			JOptionPane.showMessageDialog(null, "Cannot open preferences directory: "+Main.pref.getPreferencesDir());
			return;
		}
		if (!prefDir.exists())
			prefDir.mkdirs();

		// construct argument table
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

		preConstructorInit(args);
		JFrame mainFrame = new JFrame("Java Open Street Map - Editor");
		Main.parent = mainFrame;
		Main main = new MainApplication(mainFrame);

		mainFrame.setVisible(true);

		if (!args.containsKey("no-fullscreen") && !args.containsKey("geometry") && Toolkit.getDefaultToolkit().isFrameStateSupported(JFrame.MAXIMIZED_BOTH))
			mainFrame.setExtendedState(JFrame.MAXIMIZED_BOTH);

		main.postConstructorProcessCmdLine(args);
	}
}
