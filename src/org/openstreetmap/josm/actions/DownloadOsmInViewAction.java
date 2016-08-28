// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.util.concurrent.Future;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.downloadtasks.DownloadOsmTask;
import org.openstreetmap.josm.actions.downloadtasks.PostDownloadHandler;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.io.BoundingBoxDownloader;
import org.openstreetmap.josm.io.OnlineResource;

/**
 * Action that downloads the OSM data within the current view from the server.
 *
 * No interaction is required.
 */
public final class DownloadOsmInViewAction extends JosmAction {

    /**
     * Creates a new {@code DownloadOsmInViewAction}.
     */
    public DownloadOsmInViewAction() {
        super(tr("Download in current view"), "download_in_view", tr("Download map data from the OSM server in current view"), null, false,
                "dialogs/download_in_view", true);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        final Bounds bounds = Main.map.mapView.getRealBounds();
        DownloadOsmInViewTask task = new DownloadOsmInViewTask();
        Future<?> future = task.download(bounds);
        Main.worker.submit(new PostDownloadHandler(task, future));
    }

    @Override
    protected void updateEnabledState() {
        setEnabled(Main.getLayerManager().getActiveLayer() != null
                && !Main.isOffline(OnlineResource.OSM_API));
    }

    private static class DownloadOsmInViewTask extends DownloadOsmTask {
        Future<?> download(Bounds downloadArea) {
            return download(new DownloadTask(false, new BoundingBoxDownloader(downloadArea), null, false), downloadArea);
        }
    }
}
