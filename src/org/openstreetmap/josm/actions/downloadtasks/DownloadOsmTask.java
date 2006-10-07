package org.openstreetmap.josm.actions.downloadtasks;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.IOException;

import javax.swing.JCheckBox;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.DownloadAction;
import org.openstreetmap.josm.actions.DownloadAction.DownloadTask;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.gui.PleaseWaitRunnable;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.io.BoundingBoxDownloader;
import org.xml.sax.SAXException;

/**
 * Open the download dialog and download the data.
 * Run in the worker thread.
 */
public class DownloadOsmTask extends PleaseWaitRunnable implements DownloadTask {
	private BoundingBoxDownloader reader;
	private DataSet dataSet;
	private JCheckBox checkBox = new JCheckBox(tr("OpenStreetMap data"));

	public DownloadOsmTask() {
		super(tr("Downloading data"));
	}

	@Override public void realRun() throws IOException, SAXException {
		dataSet = reader.parseOsm();
	}

	@Override protected void finish() {
		if (dataSet == null)
			return; // user cancelled download or error occoured
		if (dataSet.allPrimitives().isEmpty())
			errorMessage = tr("No data imported.");
		Main.main.addLayer(new OsmDataLayer(dataSet, tr("Data Layer"), null));
	}

	@Override protected void cancel() {
		if (reader != null)
			reader.cancel();
	}

	public void download(DownloadAction action, double minlat, double minlon, double maxlat, double maxlon) {
		reader = new BoundingBoxDownloader(minlat, minlon, maxlat, maxlon);
		Main.worker.execute(this);
    }

	public JCheckBox getCheckBox() {
	    return checkBox;
    }

	public String getPreferencesSuffix() {
	    return "osm";
    }
}