package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collection;
import java.util.LinkedList;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.gui.layer.RawGpsLayer;
import org.openstreetmap.josm.gui.layer.RawGpsLayer.GpsPoint;
import org.openstreetmap.josm.io.OsmReader;
import org.openstreetmap.josm.io.RawCsvReader;
import org.openstreetmap.josm.io.RawGpsReader;
import org.xml.sax.SAXException;

/**
 * Open a file chooser dialog and select an file to import. Than call the gpx-import
 * driver. Finally open an internal frame into the main window with the gpx data shown.
 * 
 * @author imi
 */
public class OpenAction extends DiskAccessAction {
	
	/**
	 * Create an open action. The name is "Open a file".
	 */
	public OpenAction() {
		super(tr("Open"), "open", tr("Open a file."), KeyEvent.VK_O, InputEvent.CTRL_DOWN_MASK);
	}

	public void actionPerformed(ActionEvent e) {
		JFileChooser fc = createAndOpenFileChooser(true, true);
		if (fc == null)
			return;
		File[] files = fc.getSelectedFiles();
		for (int i = files.length; i > 0; --i)
			openFile(files[i-1]);
	}

	/**
	 * Open the given file.
	 */
	public void openFile(File file) {
		String fn = file.getName();
		try {
			if (asRawData(fn)) {
				Collection<Collection<GpsPoint>> data;
				if (ExtensionFileFilter.filters[ExtensionFileFilter.GPX].acceptName(fn)) {
					data = RawGpsReader.parse(new FileInputStream(file));
				} else if (ExtensionFileFilter.filters[ExtensionFileFilter.CSV].acceptName(fn)) {
					data = new LinkedList<Collection<GpsPoint>>();
					data.add(new RawCsvReader(new FileReader(file)).parse());
				} else
					throw new IllegalStateException();
				Main.main.addLayer(new RawGpsLayer(data, file.getName(), file));
			} else {
				DataSet dataSet;
				if (ExtensionFileFilter.filters[ExtensionFileFilter.OSM].acceptName(fn)) {
					dataSet = OsmReader.parseDataSet(new FileInputStream(file), null, null);
				} else if (ExtensionFileFilter.filters[ExtensionFileFilter.CSV].acceptName(fn)) {
					JOptionPane.showMessageDialog(Main.parent, fn+": "+tr("CSV Data import for non-GPS data is not implemented yet."));
					return;
				} else {
					JOptionPane.showMessageDialog(Main.parent, fn+": "+tr("Unknown file extension: {0}", fn.substring(file.getName().lastIndexOf('.')+1)));
					return;
				}
				Main.main.addLayer(new OsmDataLayer(dataSet, file.getName(), file));
			}
		} catch (SAXException x) {
			x.printStackTrace();
			JOptionPane.showMessageDialog(Main.parent, tr("Error while parsing {0}",fn)+": "+x.getMessage());
		} catch (IOException x) {
			x.printStackTrace();
			JOptionPane.showMessageDialog(Main.parent, tr("Could not read \"{0}\"",fn)+"\n"+x.getMessage());
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
		return true;
	}
}
