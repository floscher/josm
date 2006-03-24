package org.openstreetmap.josm.actions;

import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collection;
import java.util.LinkedList;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.KeyStroke;

import org.jdom.JDOMException;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.GeoPoint;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.gui.MapFrame;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.gui.layer.RawGpsDataLayer;
import org.openstreetmap.josm.io.GpxReader;
import org.openstreetmap.josm.io.OsmReader;
import org.openstreetmap.josm.io.OsmReaderOld;
import org.openstreetmap.josm.io.RawCsvReader;
import org.openstreetmap.josm.io.RawGpsReader;
import org.xml.sax.SAXException;

/**
 * Open a file chooser dialog and select an file to import. Than call the gpx-import
 * driver. Finally open an internal frame into the main window with the gpx data shown.
 * 
 * @author imi
 */
public class OpenAction extends JosmAction {

	/**
	 * Create an open action. The name is "Open a file".
	 */
	public OpenAction() {
		super("Open", "open", "Open a file.", "Ctrl-O", KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_DOWN_MASK));
	}

	public void actionPerformed(ActionEvent e) {
		JFileChooser fc = new JFileChooser(Main.main.currentDirectory);
		for (int i = 0; i < ExtensionFileFilter.filters.length; ++i)
			fc.addChoosableFileFilter(ExtensionFileFilter.filters[i]);
		fc.setAcceptAllFileFilterUsed(true);

		if (fc.showOpenDialog(Main.main) != JFileChooser.APPROVE_OPTION)
			return;
		
		Main.main.currentDirectory = fc.getCurrentDirectory();

		File filename = fc.getSelectedFile();
		if (filename == null)
			return;

		openFile(filename);
	}

	/**
	 * Open the given file.
	 */
	public void openFile(File filename) {
		String fn = filename.getName();
		try {
			Layer layer;

			if (asRawData(fn)) {
				Collection<Collection<GeoPoint>> data;
				if (ExtensionFileFilter.filters[ExtensionFileFilter.GPX].acceptName(fn)) {
					data = new RawGpsReader(new FileReader(filename)).parse();
				} else if (ExtensionFileFilter.filters[ExtensionFileFilter.CSV].acceptName(fn)) {
					data = new LinkedList<Collection<GeoPoint>>();
					data.add(new RawCsvReader(new FileReader(filename)).parse());
				} else
					throw new IllegalStateException();
				layer = new RawGpsDataLayer(data, filename.getName());
			} else {
				DataSet dataSet;
				if (ExtensionFileFilter.filters[ExtensionFileFilter.GPX].acceptName(fn))
					dataSet = new GpxReader(new FileReader(filename)).parse();
				else if (ExtensionFileFilter.filters[ExtensionFileFilter.OSM].acceptName(fn)) {
					try {
						// temporary allow loading of old xml format.
						dataSet = OsmReader.parseDataSet(new FileReader(filename));
					} catch (SAXException x) {
						if (x.getMessage().equals("Unknown version: null")) {
							int answer = JOptionPane.showConfirmDialog(Main.main, 
									"This seems to be an old 0.2 API XML file.\n" +
									"JOSM can try to open it with the old parser. This option\n" +
									"will not be available in future JOSM version. You should\n" +
									"immediatly save the file, if successfull imported.",
									"Load as 0.2 API file?",
									JOptionPane.YES_NO_OPTION);
							if (answer != JOptionPane.YES_OPTION)
								return;
							dataSet = new OsmReaderOld(new FileReader(filename)).parse();
						} else 
							throw x;
					}					
				} else if (ExtensionFileFilter.filters[ExtensionFileFilter.CSV].acceptName(fn)) {
					JOptionPane.showMessageDialog(Main.main, "CSV Data import for non-GPS data is not implemented yet.");
					return;
				} else {
					JOptionPane.showMessageDialog(Main.main, "Unknown file extension: "+fn.substring(filename.getName().lastIndexOf('.')+1));
					return;
				}
				layer = new OsmDataLayer(dataSet, "Data Layer", true);
			}
			
			if (Main.main.getMapFrame() == null)
				Main.main.setMapFrame(new MapFrame(layer));
			else
				Main.main.getMapFrame().mapView.addLayer(layer);

		} catch (SAXException x) {
			x.printStackTrace();
			JOptionPane.showMessageDialog(Main.main, x.getMessage());
		} catch (JDOMException x) {
			x.printStackTrace();
			JOptionPane.showMessageDialog(Main.main, x.getMessage());
		} catch (IOException x) {
			x.printStackTrace();
			JOptionPane.showMessageDialog(Main.main, "Could not read '"+fn+"'\n"+x.getMessage());
		}
	}

	/**
	 * @return Return whether the file should be opened as raw gps data. May ask the
	 * user, if unsure.
	 */
	private boolean asRawData(String fn) {
		if (ExtensionFileFilter.filters[ExtensionFileFilter.CSV].acceptName(fn))
			return true;
		if (!ExtensionFileFilter.filters[ExtensionFileFilter.GPX].acceptName(fn))
			return false;
		return JOptionPane.YES_OPTION == JOptionPane.showConfirmDialog(
				Main.main, "Do you want to open the file as raw gps data?",
				"Open as raw data?", JOptionPane.YES_NO_OPTION);
	}
}
