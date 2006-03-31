// Licence: GPL
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
import java.util.Iterator;
import java.util.LinkedList;
import java.util.StringTokenizer;

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
import org.openstreetmap.josm.actions.OpenAction;
import org.openstreetmap.josm.actions.PreferencesAction;
import org.openstreetmap.josm.actions.RedoAction;
import org.openstreetmap.josm.actions.SaveAction;
import org.openstreetmap.josm.actions.UndoAction;
import org.openstreetmap.josm.actions.UploadAction;
import org.openstreetmap.josm.data.Preferences;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.projection.Epsg4263;
import org.openstreetmap.josm.data.projection.Projection;
import org.openstreetmap.josm.gui.MapFrame;
import org.openstreetmap.josm.gui.ShowModifiers;
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

	public static Projection proj;

	/**
	 * Global application preferences
	 */
	public final static Preferences pref = new Preferences();

	/**
	 * The global dataset.
	 */
	public DataSet ds = new DataSet();
	
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

	/**
	 * This directory is used for all disc IO access as starting directory. Should
	 * be set accordingly after the IO action.
	 */
	public File currentDirectory = new File(".");

	private OpenAction openAction;

	private DownloadAction downloadAction;
	
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
		UploadAction uploadAction = new UploadAction();
		openAction = new OpenAction();
		SaveAction saveAction = new SaveAction();
		ExitAction exitAction = new ExitAction();
		undoAction = new UndoAction();
		redoAction = new RedoAction();
		PreferencesAction preferencesAction = new PreferencesAction();
		AboutAction aboutAction = new AboutAction();

		// creating menu
		JMenuBar mainMenu = new JMenuBar();
		setJMenuBar(mainMenu);

		JMenu fileMenu = new JMenu("Files");
		fileMenu.setMnemonic('F');
		fileMenu.add(openAction);
		fileMenu.add(saveAction);
		fileMenu.addSeparator();
		fileMenu.add(exitAction);
		mainMenu.add(fileMenu);

		
		JMenu connectionMenu = new JMenu("Connection");
		connectionMenu.setMnemonic('C');
		connectionMenu.add(downloadAction);
		connectionMenu.add(uploadAction);
		mainMenu.add(connectionMenu);
		
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
		toolBar.add(openAction);
		toolBar.add(saveAction);
		toolBar.addSeparator();
		toolBar.add(undoAction);
		toolBar.add(redoAction);
		toolBar.addSeparator();
		toolBar.add(preferencesAction);
		
		getContentPane().add(toolBar, BorderLayout.NORTH);
	
		addWindowListener(new WindowAdapter(){
			@Override
			public void windowClosing(WindowEvent arg0) {
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

		if (arguments.contains("--help")) {
			System.out.println("Java OpenStreetMap Editor");
			System.out.println();
			System.out.println("usage:");
			System.out.println("\tjava -jar josm.jar <options>");
			System.out.println();
			System.out.println("options:");
			System.out.println("\t--help                                  Show this help");
			System.out.println("\t--download=minlat,minlon,maxlat,maxlon  Download the bounding box");
			System.out.println("\t--open=file(.osm|.xml|.gpx|.txt|.csv)   Open the specific file");
			System.out.println("\t--no-fullscreen                         Don't launch in fullscreen mode");
			System.out.println("\t--reset-preferences                     Reset the preferences to default");
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
			proj = new Epsg4263();
		}
		
		try {
			UIManager.setLookAndFeel(pref.get("laf"));
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		new Main();
		main.setVisible(true);


		if (arguments.remove("--show-modifiers")) {
			Point p = main.getLocationOnScreen();
			Dimension s = main.getSize();
			new ShowModifiers(p.x + s.width - 3, p.y + s.height - 32);
			main.setVisible(true);
		}
		
		if (!arguments.remove("--no-fullscreen")) {
			if (Toolkit.getDefaultToolkit().isFrameStateSupported(MAXIMIZED_BOTH))
				main.setExtendedState(MAXIMIZED_BOTH); // some platform are able to maximize
			else {
				Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
				main.setSize(d);
			}
		}

		for (Iterator<String> it = arguments.iterator(); it.hasNext();) {
			String s = it.next();
			if (s.startsWith("--open=")) {
				main.openAction.openFile(new File(s.substring(7)));
				it.remove();
			} else if (s.startsWith("--download=")) {
				downloadFromParamString(false, s.substring(11));
				it.remove();
			} else if (s.startsWith("--downloadgps=")) {
				downloadFromParamString(true, s.substring(14));
				it.remove();
			}
		}
		
		if (!arguments.isEmpty()) {
			String s = "Unknown Parameter:\n";
			for (String arg : arguments)
				s += arg+"\n";
			JOptionPane.showMessageDialog(main, s);
		}
	}


	private static void downloadFromParamString(boolean rawGps, String s) {
		StringTokenizer st = new StringTokenizer(s, ",");
		if (st.countTokens() != 4) {
			JOptionPane.showMessageDialog(main, "Download option does not take "+st.countTokens()+" bounding parameter.");
			return;
		}

		try {
			main.downloadAction.download(rawGps, Double.parseDouble(st.nextToken()), Double.parseDouble(st.nextToken()), Double.parseDouble(st.nextToken()), Double.parseDouble(st.nextToken()));
		} catch (NumberFormatException e) {
			JOptionPane.showMessageDialog(main, "Could not parse the Coordinates.");
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
