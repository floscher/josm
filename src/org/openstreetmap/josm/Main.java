//Licence: GPL
package org.openstreetmap.josm;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.StringTokenizer;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.Action;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JToolBar;
import javax.swing.UIManager;

import org.openstreetmap.josm.actions.AboutAction;
import org.openstreetmap.josm.actions.DownloadAction;
import org.openstreetmap.josm.actions.ExitAction;
import org.openstreetmap.josm.actions.GpxExportAction;
import org.openstreetmap.josm.actions.OpenAction;
import org.openstreetmap.josm.actions.PreferencesAction;
import org.openstreetmap.josm.actions.RedoAction;
import org.openstreetmap.josm.actions.SaveAction;
import org.openstreetmap.josm.actions.UndoAction;
import org.openstreetmap.josm.actions.UploadAction;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.Preferences;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.projection.Epsg4326;
import org.openstreetmap.josm.data.projection.Projection;
import org.openstreetmap.josm.gui.MapFrame;
import org.openstreetmap.josm.gui.ShowModifiers;
import org.openstreetmap.josm.gui.dialogs.SelectionListDialog;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.tools.BugReportExceptionHandler;
import org.openstreetmap.josm.tools.ImageProvider;

/**
 * Main window class application.
 *  
 * @author imi
 */
public class Main extends JFrame {

	/**
	 * Global application window. Use this as JOPtionPane-parent to center on application.
	 */
	public static Main main;

	/**
	 * The worker thread slave. This is for executing all long and intensive
	 * calculations. The executed runnables are guaranteed to be executed seperatly
	 * and sequenciel.
	 */
	public static Executor worker = Executors.newSingleThreadExecutor();


	public static Projection proj;

	/**
	 * Global application preferences
	 */
	public final static Preferences pref = new Preferences();

	/**
	 * The global dataset.
	 */
	public static DataSet ds = new DataSet();

	/**
	 * The main panel.
	 */
	public JPanel panel;
	/**
	 * The mapFrame currently loaded.
	 */
	private MapFrame mapFrame;

	public final UndoAction undoAction;
	public final RedoAction redoAction;

	private OpenAction openAction;
	private DownloadAction downloadAction;
	//private Action wmsServerAction;

	/**
	 * Construct an main frame, ready sized and operating. Does not 
	 * display the frame.
	 */
	public Main() {
		super("Java Open Street Map - Editor");
		Main.main = this;
		setLayout(new BorderLayout());
		panel = new JPanel(new BorderLayout());
		getContentPane().add(panel, BorderLayout.CENTER);
		setSize(1000,740); // some strange default size
		setVisible(true);

		downloadAction = new DownloadAction();
		Action uploadAction = new UploadAction();
		//wmsServerAction = new WmsServerAction();
		openAction = new OpenAction();
		Action saveAction = new SaveAction();
		Action gpxExportAction = new GpxExportAction(null);
		Action exitAction = new ExitAction();
		undoAction = new UndoAction();
		redoAction = new RedoAction();
		Action preferencesAction = new PreferencesAction();
		Action aboutAction = new AboutAction();

		// creating menu
		JMenuBar mainMenu = new JMenuBar();
		setJMenuBar(mainMenu);

		JMenu fileMenu = new JMenu("Files");
		fileMenu.setMnemonic('F');
		fileMenu.add(openAction);
		fileMenu.add(saveAction);
		fileMenu.add(gpxExportAction);
		fileMenu.addSeparator();
		fileMenu.add(exitAction);
		mainMenu.add(fileMenu);


		JMenu layerMenu = new JMenu("Layer");
		layerMenu.setMnemonic('L');
		layerMenu.add(downloadAction);
		layerMenu.add(uploadAction);
		layerMenu.addSeparator();
		//layerMenu.add(new JCheckBoxMenuItem(wmsServerAction));
		mainMenu.add(layerMenu);

		JMenu editMenu = new JMenu("Edit");
		editMenu.setMnemonic('E');
		editMenu.add(undoAction);
		editMenu.add(redoAction);
		editMenu.addSeparator();
		editMenu.add(preferencesAction);
		mainMenu.add(editMenu);

		mainMenu.add(new JSeparator());
		JMenu helpMenu = new JMenu("Help");
		helpMenu.setMnemonic('H');
		helpMenu.add(aboutAction);
		mainMenu.add(helpMenu);

		// creating toolbar
		JToolBar toolBar = new JToolBar();
		toolBar.setFloatable(false);
		toolBar.add(downloadAction);
		toolBar.add(uploadAction);
		//toolBar.add(new IconToggleButton(wmsServerAction));
		toolBar.addSeparator();
		toolBar.add(openAction);
		toolBar.add(saveAction);
		toolBar.add(gpxExportAction);
		toolBar.addSeparator();
		toolBar.add(undoAction);
		toolBar.add(redoAction);
		toolBar.addSeparator();
		toolBar.add(preferencesAction);

		getContentPane().add(toolBar, BorderLayout.NORTH);

		addWindowListener(new WindowAdapter(){
			@Override public void windowClosing(WindowEvent arg0) {
				if (mapFrame != null) {
					boolean modified = false;
					boolean uploadedModified = false;
					for (Layer l : mapFrame.mapView.getAllLayers()) {
						if (l instanceof OsmDataLayer && ((OsmDataLayer)l).isModified()) {
							modified = true;
							uploadedModified = ((OsmDataLayer)l).uploadedModified;
							break;
						}
					}
					if (modified) {
						String msg = uploadedModified ? "\nHint: Some changes came from uploading new data to the server." : "";
						int answer = JOptionPane.showConfirmDialog(
								Main.this, "There are unsaved changes. Really quit?"+msg,
								"Unsaved Changes", JOptionPane.YES_NO_OPTION);
						if (answer != JOptionPane.YES_OPTION)
							return;
					}
				}
				System.exit(0);
			}
		});
		setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
	}

	/**
	 * Main application Startup
	 * @param args	No parameters accepted.
	 */
	public static void main(String[] args) {
		setupExceptionHandler();
		setupUiDefaults();

		LinkedList<String> arguments = new LinkedList<String>(Arrays.asList(args));

		if (arguments.contains("--help") || arguments.contains("-?") || arguments.contains("-h")) {
			System.out.println("Java OpenStreetMap Editor");
			System.out.println();
			System.out.println("usage:");
			System.out.println("\tjava -jar josm.jar <option> <option> <option>...");
			System.out.println();
			System.out.println("options:");
			System.out.println("\t--help|-?|-h                              Show this help");
			System.out.println("\t--geometry=widthxheight(+|-)x(+|-)y       Standard unix geometry argument");
			System.out.println("\t--download=minlat,minlon,maxlat,maxlon    Download the bounding box");
			System.out.println("\t--downloadgps=minlat,minlon,maxlat,maxlon Download the bounding box");
			System.out.println("\t--selection=<searchstring>                Select with the given search");
			System.out.println("\t--no-fullscreen                           Don't launch in fullscreen mode");
			System.out.println("\t--reset-preferences                       Reset the preferences to default");
			System.out.println("\tURL|filename(.osm|.xml|.gpx|.txt|.csv)    Open file / Download url");
			System.out.println();
			System.out.println("examples:");
			System.out.println("\tjava -jar josm.jar track1.gpx track2.gpx london.osm");
			System.out.println("\tjava -jar josm.jar http://www.openstreetmap.org/index.html?lat=43.2&lon=11.1&zoom=13");
			System.out.println("\tjava -jar josm.jar london.osm --selection=http://www.ostertag.name/osm/OSM_errors_node-duplicate.xml");
			System.out.println("\tjava -jar josm.jar osm://43.2,11.1,43.4,11.4");
			System.out.println();
			System.out.println("Parameters are read in the order they are specified, so make sure you load");
			System.out.println("some data before --selection");
			System.out.println();
			System.out.println("Instead of --download=<bbox> you may specify osm://<bbox>");
			System.exit(0);
		}

		File prefDir = new File(Preferences.getPreferencesDir());
		if (prefDir.exists() && !prefDir.isDirectory()) {
			JOptionPane.showMessageDialog(null, "Cannot open preferences directory: "+Preferences.getPreferencesDir());
			return;
		}
		if (!prefDir.exists())
			prefDir.mkdirs();

		// load preferences
		String errMsg = null;
		try {
			if (arguments.remove("--reset-preferences")) {
				pref.resetToDefault();
			} else
				pref.load();
		} catch (IOException e1) {
			e1.printStackTrace();
			errMsg = "Preferences could not be loaded. Write default preference file to '"+Preferences.getPreferencesDir()+"preferences'.";
			pref.resetToDefault();
		}
		if (errMsg != null)
			JOptionPane.showMessageDialog(null, errMsg);

		try {
			proj = (Projection)Class.forName(pref.get("projection")).newInstance();
		} catch (Exception e) {
			e.printStackTrace();
			JOptionPane.showMessageDialog(null, "The projection could not be read from preferences. Using EPSG:4263.");
			proj = new Epsg4326();
		}

		try {
			UIManager.setLookAndFeel(pref.get("laf"));
		} catch (Exception e) {
			e.printStackTrace();
		}

		new Main();
		main.setVisible(true);

		if (!arguments.remove("--no-fullscreen")) {
			if (Toolkit.getDefaultToolkit().isFrameStateSupported(MAXIMIZED_BOTH))
				main.setExtendedState(MAXIMIZED_BOTH); // some platform are able to maximize
			else {
				Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
				main.setSize(d);
			}
		}

		boolean showModifiers = false;

		for (String s : arguments) {
			if (s.startsWith("--download=") || s.startsWith("osm:")) {
				downloadFromParamString(false, s);
			} else if (s.startsWith("--downloadgps=")) {
				downloadFromParamString(true, s);
			} else if (s.startsWith("--geometry=")) {
				Matcher m = Pattern.compile("(\\d+)x(\\d+)(([+-])(\\d+)([+-])(\\d+))?").matcher(s.substring(11));
				if (m.matches()) {
					main.setExtendedState(NORMAL);
					Integer w = Integer.valueOf(m.group(1));
					Integer h = Integer.valueOf(m.group(2));
					main.setSize(w, h);
					if (m.group(3) != null) {
						int x = Integer.valueOf(m.group(5));
						int y = Integer.valueOf(m.group(7));
						if (m.group(4).equals("-"))
							x = Toolkit.getDefaultToolkit().getScreenSize().width - x - w;
						if (m.group(6).equals("-"))
							y = Toolkit.getDefaultToolkit().getScreenSize().height - y - h;
						main.setLocation(x,y);
					}
				} else
					System.out.println("Ignoring malformed geometry: "+s.substring(11));
			} else if (s.equals("--show-modifiers")) {
				showModifiers = true;
			} else if (s.startsWith("--selection=")) {
				SelectionListDialog.search(s.substring(12), SelectionListDialog.SearchMode.add);
			} else if (s.startsWith("http:")) {
				Bounds b = DownloadAction.osmurl2bounds(s);
				if (b == null)
					JOptionPane.showMessageDialog(main, "Ignoring malformed url: "+s);
				else
					main.downloadAction.download(false, b.min.lat(), b.min.lon(), b.max.lat(), b.max.lon());
			} else {
				main.openAction.openFile(new File(s));
			}
		}

		if (showModifiers) {
			Point p = main.getLocationOnScreen();
			Dimension s = main.getSize();
			new ShowModifiers(p.x + s.width - 3, p.y + s.height - 32);
			main.setVisible(true);
		}
	}


	private static void downloadFromParamString(boolean rawGps, String s) {
		s = s.replaceAll("^(osm:/?/?)|(--download(gps)?=)", "");
		StringTokenizer st = new StringTokenizer(s, ",");
		if (st.countTokens() != 4) {
			JOptionPane.showMessageDialog(main, "Malformed bounding box: "+s);
			return;
		}

		try {
			main.downloadAction.download(rawGps, Double.parseDouble(st.nextToken()), Double.parseDouble(st.nextToken()), Double.parseDouble(st.nextToken()), Double.parseDouble(st.nextToken()));
		} catch (NumberFormatException e) {
			JOptionPane.showMessageDialog(main, "Could not parse the Coordinates: "+s);
		}
	}

	//TODO: should be solved better.
	public void setMapFrame(MapFrame mapFrame) {
		if (this.mapFrame != null)
			this.mapFrame.setVisible(false);
		this.mapFrame = mapFrame;
		panel.setVisible(false);
		panel.removeAll();
		if (mapFrame != null) {
			mapFrame.fillPanel(panel);
			panel.setVisible(true);
			mapFrame.setVisible(true);
		}
	}
	/**
	 * @return Returns the mapFrame.
	 */
	public MapFrame getMapFrame() {
		return mapFrame;
	}


	/**
	 * Sets some icons to the ui.
	 */
	private static void setupUiDefaults() {
		UIManager.put("OptionPane.okIcon", ImageProvider.get("ok"));
		UIManager.put("OptionPane.yesIcon", UIManager.get("OptionPane.okIcon"));
		UIManager.put("OptionPane.cancelIcon", ImageProvider.get("cancel"));
		UIManager.put("OptionPane.noIcon", UIManager.get("OptionPane.cancelIcon"));
	}

	/**
	 * Setup an exception handler that displays a sorry message and the possibility
	 * to do a bug report.
	 */
	private static void setupExceptionHandler() {
		Thread.setDefaultUncaughtExceptionHandler(new BugReportExceptionHandler());
	}
}
