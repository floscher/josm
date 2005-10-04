package org.openstreetmap.josm.actions;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import javax.swing.AbstractAction;
import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileFilter;

import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.gui.Main;
import org.openstreetmap.josm.gui.MapFrame;
import org.openstreetmap.josm.io.GpxReader;
import org.openstreetmap.josm.io.DataReader.ConnectionException;
import org.openstreetmap.josm.io.DataReader.ParseException;

/**
 * Open a file chooser dialog and select an file to import. Than call the gpx-import
 * driver. Finally open an internal frame into the main window with the gpx data shown.
 * 
 * @author imi
 */
public class OpenGpxAction extends AbstractAction {

	/**
	 * Create an open action. The name is "&Open GPX".
	 */
	public OpenGpxAction() {
		super("Open GPX", new ImageIcon(Main.class.getResource("/images/opengpx.png")));
		putValue(MNEMONIC_KEY, KeyEvent.VK_O);
	}

	public void actionPerformed(ActionEvent e) {
		JFileChooser fc = new JFileChooser("data");
		fc.setFileFilter(new FileFilter(){
			@Override
			public boolean accept(File f) {
				String name = f.getName().toLowerCase();
				return name.endsWith(".gpx") || name.endsWith(".xml");
			}
			@Override
			public String getDescription() {
				return "GPX or XML Files";
			}});
		fc.showOpenDialog(Main.main);
		File gpxFile = fc.getSelectedFile();
		if (gpxFile == null)
			return;
		
		try {
			DataSet dataSet = new GpxReader(new FileReader(gpxFile)).parse();
			MapFrame map = new MapFrame(dataSet);
			Main.main.setMapFrame(gpxFile.getName(), map);
		} catch (ParseException x) {
			x.printStackTrace();
			JOptionPane.showMessageDialog(Main.main, x.getMessage());
		} catch (IOException x) {
			x.printStackTrace();
			JOptionPane.showMessageDialog(Main.main, "Could not read '"+gpxFile.getName()+"'\n"+x.getMessage());
		} catch (ConnectionException x) {
			x.printStackTrace();
			JOptionPane.showMessageDialog(Main.main, x.getMessage());
		}
	}
}
