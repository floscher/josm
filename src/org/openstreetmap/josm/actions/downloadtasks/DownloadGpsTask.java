package org.openstreetmap.josm.actions.downloadtasks;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.IOException;
import java.util.Collection;

import javax.swing.JCheckBox;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.DownloadAction;
import org.openstreetmap.josm.actions.DownloadAction.DownloadTask;
import org.openstreetmap.josm.gui.PleaseWaitRunnable;
import org.openstreetmap.josm.gui.layer.RawGpsLayer;
import org.openstreetmap.josm.gui.layer.RawGpsLayer.GpsPoint;
import org.openstreetmap.josm.io.BoundingBoxDownloader;
import org.xml.sax.SAXException;

public class DownloadGpsTask implements DownloadTask {

	private static class Task extends PleaseWaitRunnable {
		private BoundingBoxDownloader reader;
		private DownloadAction action;
		private Collection<Collection<GpsPoint>> rawData;

		public Task(BoundingBoxDownloader reader, DownloadAction action) {
			super(tr("Downloading GPS data"));
			this.reader = reader;
			this.action = action;
		}

		@Override public void realRun() throws IOException, SAXException {
			rawData = reader.parseRawGps();
		}

		@Override protected void finish() {
			if (rawData == null)
				return;
			String name = action.latlon[0].getText() + " " + action.latlon[1].getText() + " x " + this.action.latlon[2].getText() + " " + this.action.latlon[3].getText();
			Main.main.addLayer(new RawGpsLayer(rawData, name, null));
		}

		@Override protected void cancel() {
			if (reader != null)
				reader.cancel();
		}
	}

	private JCheckBox checkBox = new JCheckBox(tr("Raw GPS data"));

	public void download(DownloadAction action, double minlat, double minlon, double maxlat, double maxlon) {
		Task task = new Task(new BoundingBoxDownloader(minlat, minlon, maxlat, maxlon), action);
		Main.worker.execute(task);
	}

	public JCheckBox getCheckBox() {
	    return checkBox;
    }

	public String getPreferencesSuffix() {
	    return "gps";
    }
}
