// Licence: GPL
package org.openstreetmap.josm.gui;

import java.awt.BorderLayout;
import java.awt.Container;

import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JToolBar;
import javax.swing.UIManager;

import org.openstreetmap.josm.actions.AboutAction;
import org.openstreetmap.josm.actions.ExitAction;
import org.openstreetmap.josm.actions.OpenGpxAction;
import org.openstreetmap.josm.actions.OpenOsmServerAction;
import org.openstreetmap.josm.actions.PreferencesAction;
import org.openstreetmap.josm.actions.SaveGpxAction;
import org.openstreetmap.josm.data.Preferences;
import org.openstreetmap.josm.data.Preferences.PreferencesException;

/**
 * Main window class consisting of the mainframe MDI application.
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
	 * The main panel.
	 */
	private Container panel;
	/**
	 * The name of the current loaded mapFrame
	 */
	private String name;
	/**
	 * The mapFrame currently loaded.
	 */
	private MapFrame mapFrame;
	
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
		setExtendedState(MAXIMIZED_BOTH); // some platform are able to maximize
		
		// creating actions
		OpenOsmServerAction openServerAction = new OpenOsmServerAction();
		OpenGpxAction openGpxAction = new OpenGpxAction();
		SaveGpxAction saveGpxAction = new SaveGpxAction();
		ExitAction exitAction = new ExitAction();
		PreferencesAction preferencesAction = new PreferencesAction();
		AboutAction aboutAction = new AboutAction();

		// creating menu
		JMenuBar mainMenu = new JMenuBar();
		setJMenuBar(mainMenu);

		JMenu fileMenu = new JMenu("Files");
		fileMenu.setMnemonic('F');
		fileMenu.add(openGpxAction);
		fileMenu.add(saveGpxAction);
		fileMenu.addSeparator();
		fileMenu.add(exitAction);
		mainMenu.add(fileMenu);
		
		JMenu connectionMenu = new JMenu("Connection");
		connectionMenu.setMnemonic('C');
		connectionMenu.add(openServerAction);
		mainMenu.add(connectionMenu);
		
		JMenu editMenu = new JMenu("Edit");
		editMenu.setMnemonic('E');
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
		toolBar.add(openServerAction);
		toolBar.add(openGpxAction);
		toolBar.add(saveGpxAction);
		toolBar.addSeparator();
		toolBar.add(preferencesAction);
		
		getContentPane().add(toolBar, BorderLayout.NORTH);
	}

	/**
	 * Main application Startup
	 * @param args	No parameters accepted.
	 */
	public static void main(String[] args) {
		setupUiDefaults();
		
		// load preferences
		String errMsg = null;
		try {
			pref.load();
		} catch (PreferencesException e1) {
			e1.printStackTrace();
			errMsg = "Preferences could not be loaded. Write default preference file to '"+Preferences.getPreferencesFile()+"'.";
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
		main.setDefaultCloseOperation(EXIT_ON_CLOSE);
		main.setVisible(true);
	}


	/**
	 * Set the main's mapframe. If a changed old mapFrame is already set, 
	 * ask the user whether he want to save, discard or abort. If the user
	 * aborts, nothing happens. 
	 */
	public void setMapFrame(String name, MapFrame mapFrame) {
		//TODO: Check for changes and ask user
		this.name = name;
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
	 * @return Returns the name.
	 */
	public String getNameOfLoadedMapFrame() {
		return name;
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
}
