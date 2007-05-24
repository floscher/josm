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
import java.util.zip.GZIPInputStream;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.DataSource;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.gui.layer.RawGpsLayer;
import org.openstreetmap.josm.gui.layer.RawGpsLayer.GpsPoint;
import org.openstreetmap.josm.gui.layer.markerlayer.Marker;
import org.openstreetmap.josm.gui.layer.markerlayer.MarkerLayer;
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
				Collection<Collection<GpsPoint>> gpsData = null;
				Collection<Marker> markerData = null;
				if (ExtensionFileFilter.filters[ExtensionFileFilter.GPX].acceptName(fn)) {
					RawGpsReader r = null;
					// Check to see if we are opening a compressed file
					if(file.getName().endsWith(".gpx.gz")) {
						r = new RawGpsReader(new GZIPInputStream(new FileInputStream(file)), file.getAbsoluteFile().getParentFile());
					} else {
						r = new RawGpsReader(new FileInputStream(file), file.getAbsoluteFile().getParentFile());
					}
					gpsData = r.trackData;
					markerData = r.markerData;
				} else if (ExtensionFileFilter.filters[ExtensionFileFilter.CSV].acceptName(fn)) {
					gpsData = new LinkedList<Collection<GpsPoint>>();
					gpsData.add(new RawCsvReader(new FileReader(file)).parse());
				} else
					throw new IllegalStateException();
				if ((gpsData != null) && (!gpsData.isEmpty()))
					Main.main.addLayer(new RawGpsLayer(gpsData, tr("Tracks from {0}", file.getName()), file));
				if ((markerData != null) && (!markerData.isEmpty()))
					Main.main.addLayer(new MarkerLayer(markerData, tr ("Markers from {0}", file.getName()), file));
				
			} else {
				DataSet dataSet;
				if (ExtensionFileFilter.filters[ExtensionFileFilter.OSM].acceptName(fn)) {
					dataSet = OsmReader.parseDataSet(new FileInputStream(file), null, Main.pleaseWaitDlg);
					DataSource src = new DataSource();
					src.sourceSpec = "File " + fn;
					dataSet.dataSources.add(src);
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
