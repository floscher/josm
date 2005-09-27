// Licence: GPL
package org.openstreetmap.josm.gui;

import java.awt.BorderLayout;
import java.awt.Container;

import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JToolBar;
import javax.swing.UIManager;

import org.openstreetmap.josm.actions.ExitAction;
import org.openstreetmap.josm.actions.OpenGpxAction;
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
	public static Preferences pref = new Preferences();
	
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
		
		// creating menu
		JMenuBar mainMenu = new JMenuBar();
		setJMenuBar(mainMenu);

		JMenu fileMenu = new JMenu("Files");
		fileMenu.setMnemonic('F');
		fileMenu.add(new OpenGpxAction());
		fileMenu.add(new SaveGpxAction());
		fileMenu.addSeparator();
		fileMenu.add(new ExitAction());
		mainMenu.add(fileMenu);
		
		JMenu editMenu = new JMenu("Edit");
		editMenu.setMnemonic('E');
		editMenu.add(new PreferencesAction());
		mainMenu.add(editMenu);

		// creating toolbar
		JToolBar toolBar = new JToolBar();
		toolBar.setFloatable(false);
		toolBar.add(new OpenGpxAction());
		toolBar.add(new SaveGpxAction());
		toolBar.addSeparator();
		toolBar.add(new PreferencesAction());
		
		getContentPane().add(toolBar, BorderLayout.NORTH);
	}

	/**
	 * Main application Startup
	 * @param args	No parameters accepted.
	 */
	public static void main(String[] args) {
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
		this.mapFrame = mapFrame;
		panel.removeAll();
		panel.add(mapFrame, BorderLayout.CENTER);
		panel.add(mapFrame.getToolBarActions(), BorderLayout.WEST);
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

}
