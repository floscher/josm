// Licence: GPL
package org.openstreetmap.josm;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.Arrays;
import java.util.Collection;

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
import org.openstreetmap.josm.data.Preferences.PreferencesException;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.gui.BugReportExceptionHandler;
import org.openstreetmap.josm.gui.ImageProvider;
import org.openstreetmap.josm.gui.MapFrame;
import org.openstreetmap.josm.gui.ShowModifiers;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;

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
	private Container panel;
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
	
	/**
	 * Construct an main frame, ready sized and operating. Does not 
	 * display the frame.
	 */
	public Main() {
		super("Java Open Street Map - Editor");
		setLayout(new BorderLayout());
		panel = new JPanel(new BorderLayout());
		getContentPane().add(panel, BorderLayout.CENTER);
		setSize(1000,740); // some strange default size
		setVisible(true);
		
		// creating actions
		DownloadAction downloadAction = new DownloadAction();
		UploadAction uploadAction = new UploadAction();
		OpenAction openAction = new OpenAction();
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

		// load preferences
		String errMsg = null;
		try {
			pref.load();
		} catch (PreferencesException e1) {
			e1.printStackTrace();
			errMsg = "Preferences could not be loaded. Write default preference file to '"+Preferences.getPreferencesDir()+"preferences'.";
			try {
				pref.save();
			} catch (PreferencesException e) {
				e.printStackTrace();
				errMsg = "Preferences could not be loaded. Reverting to default.";
			}
		}
		if (errMsg != null)
			JOptionPane.showMessageDialog(null, errMsg);
		
		try {
			UIManager.setLookAndFeel(pref.laf.getClassName());
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		main = new Main();
		main.setVisible(true);

		Collection<String> arguments = Arrays.asList(args);

		if (arguments.contains("--show-modifiers")) {
			Point p = main.getLocationOnScreen();
			Dimension s = main.getSize();
			new ShowModifiers(p.x + s.width - 3, p.y + s.height - 32);
			main.setVisible(true);
		}
		
		if (!arguments.contains("--no-fullscreen")) {
			if (Toolkit.getDefaultToolkit().isFrameStateSupported(MAXIMIZED_BOTH))
				main.setExtendedState(MAXIMIZED_BOTH); // some platform are able to maximize
			else {
				Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
				main.setSize(d);
			}
		}
	}


	/**
	 * Set the main's mapframe. If a changed old mapFrame is already set, 
	 * ask the user whether he want to save, discard or abort. If the user
	 * aborts, nothing happens. 
	 */
	public void setMapFrame(MapFrame mapFrame) {
		//TODO: Check for changes and ask user
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
