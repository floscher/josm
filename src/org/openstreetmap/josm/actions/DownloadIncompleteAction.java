package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trn;

import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;

import javax.swing.JOptionPane;
import javax.swing.KeyStroke;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Segment;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.PleaseWaitRunnable;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.io.ObjectListDownloader;
import org.xml.sax.SAXException;

/**
 * Action that opens a connection to the osm server and download map data.
 * 
 * An dialog is displayed asking the user to specify a rectangle to grab.
 * The url and account settings from the preferences are used.
 *  
 * @author imi
 */
public class DownloadIncompleteAction extends JosmAction {

	/**
	 * Open the download dialog and download the data.
	 * Run in the worker thread.
	 */
	private final class DownloadTask extends PleaseWaitRunnable {
		private ObjectListDownloader reader;
		private DataSet dataSet;
		private boolean nodesLoaded = false;

		private DownloadTask(Collection<OsmPrimitive> toDownload) {
			super(trn("Downloading {0} segments", "Downloading {0} segment", toDownload.size(), toDownload.size()));
			reader = new ObjectListDownloader(toDownload);
			reader.setProgressInformation(currentAction, progress);
		}

		@Override public void realRun() throws IOException, SAXException {
			dataSet = reader.parse();
		}

		@Override protected void finish() {
			if (dataSet == null)
				return; // user cancelled download or error occoured
			if (dataSet.allPrimitives().isEmpty())
				errorMessage = tr("No data imported.");
			if (errorMessage == null && nodesLoaded == false)
				startDownloadNodes();
			else if (errorMessage == null)
				Main.main.addLayer(new OsmDataLayer(dataSet, tr("Data Layer"), false));
		}

		private void startDownloadNodes() {
			Collection<OsmPrimitive> nodes = new HashSet<OsmPrimitive>();
			for (Segment s : dataSet.segments) {
				nodes.add(s.from);
				nodes.add(s.to);
			}
			reader = new ObjectListDownloader(nodes);
			reader.setProgressInformation(currentAction, progress);
			nodesLoaded = true;
			Main.worker.execute(this);
			pleaseWaitDlg.setVisible(true);
		}

		@Override protected void cancel() {
			reader.cancel();
		}
	}

	public DownloadIncompleteAction() {
		super(tr("Download incomplete objects"), "downloadincomplete", tr("Download all (selected) incomplete ways from the OSM server."), tr("Ctrl-Shift-Alt-D"), 
				KeyStroke.getKeyStroke(KeyEvent.VK_D, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK | InputEvent.ALT_DOWN_MASK));
	}

	public void actionPerformed(ActionEvent e) {
		Collection<Way> ways = new HashSet<Way>();
		boolean sel = false;
		for (Way w : Main.ds.ways) {
			if (w.isIncomplete())
				ways.add(w);
			sel = sel || w.selected;
		}
		if (sel)
			for (Iterator<Way> it = ways.iterator(); it.hasNext();)
				if (!it.next().selected)
					it.remove();
		Collection<OsmPrimitive> toDownload = new HashSet<OsmPrimitive>();
		for (Way w : ways)
			toDownload.addAll(w.segments);
		if (JOptionPane.YES_OPTION != JOptionPane.showConfirmDialog(Main.parent, tr("Download {0} ways containing a total of {1} segments?", ways.size(), toDownload.size()), tr("Download?"), JOptionPane.YES_NO_OPTION))
			return;
		PleaseWaitRunnable task = new DownloadTask(toDownload);
		Main.worker.execute(task);
		task.pleaseWaitDlg.setVisible(true);
	}
}
