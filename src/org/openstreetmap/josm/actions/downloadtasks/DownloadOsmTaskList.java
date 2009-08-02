// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions.downloadtasks;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.EventQueue;
import java.awt.geom.Area;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.swing.JOptionPane;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.UpdateSelectionAction;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.gui.OptionPaneUtil;
import org.openstreetmap.josm.gui.download.DownloadDialog.DownloadTask;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;

/**
 * This class encapsulates the downloading of several bounding boxes that would otherwise be too
 * large to download in one go. Error messages will be collected for all downloads and displayed
 * as a list in the end.
 * @author xeen
 *
 */
public class DownloadOsmTaskList implements Runnable {
    private List<DownloadTask> osmTasks = new LinkedList<DownloadTask>();
    private ProgressMonitor progressMonitor;

    /**
     * Downloads a list of areas from the OSM Server
     * @param newLayer Set to true if all areas should be put into a single new layer
     * @param The List of Rectangle2D to download
     */
    public void download(boolean newLayer, List<Rectangle2D> rects, ProgressMonitor progressMonitor) {
        this.progressMonitor = progressMonitor;
        if(newLayer) {
            Layer l = new OsmDataLayer(new DataSet(), OsmDataLayer.createNewName(), null);
            Main.main.addLayer(l);
            Main.map.mapView.setActiveLayer(l);
        }

        progressMonitor.beginTask(null, rects.size());
        try {
            int i = 0;
            for(Rectangle2D td : rects) {
                i++;
                DownloadTask dt = new DownloadOsmTask();
                ProgressMonitor childProgress = progressMonitor.createSubTaskMonitor(1, false);
                childProgress.setSilent(true);
                childProgress.setCustomText(tr("Download {0} of {1} ({2} left)", i, rects.size(), rects.size()-i));
                dt.download(null, td.getMinY(), td.getMinX(), td.getMaxY(), td.getMaxX(), childProgress);
                osmTasks.add(dt);
            }
        } finally {
            // If we try to get the error message now the download task will never have been started
            // and we'd be stuck in a classical dead lock. Instead attach this to the worker and once
            // run() gets called all downloadTasks have finished and we can grab the error messages.
            Main.worker.execute(this);
        }
    }

    /**
     * Downloads a list of areas from the OSM Server
     * @param newLayer Set to true if all areas should be put into a single new layer
     * @param The Collection of Areas to download
     */
    public void download(boolean newLayer, Collection<Area> areas, ProgressMonitor progressMonitor) {
        progressMonitor.beginTask(tr("Updating data"));
        try {
            List<Rectangle2D> rects = new LinkedList<Rectangle2D>();
            for(Area a : areas) {
                rects.add(a.getBounds2D());
            }

            download(newLayer, rects, progressMonitor.createSubTaskMonitor(ProgressMonitor.ALL_TICKS, false));
        } finally {
            progressMonitor.finishTask();
        }
    }

    /**
     * Grabs and displays the error messages after all download threads have finished.
     */
    public void run() {
        progressMonitor.finishTask();
        String errors = "";

        for(DownloadTask dt : osmTasks) {
            String err = dt.getErrorMessage();
            if(err.equals("")) {
                continue;
            }
            errors += "<br>* " + err;
        }

        if(! errors.equals("")) {
            OptionPaneUtil.showMessageDialog(
                    Main.parent,
                    "<html>"+tr("The following errors occurred during mass download:{0}", errors)
                    +"</html>",
                    tr("Errors during Download"),
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        // FIXME: this is a hack. We assume that the user canceled the whole download if at least
        // one task was canceled or if it failed
        //
        for (DownloadTask task: osmTasks) {
            if (task instanceof DownloadOsmTask) {
                DownloadOsmTask osmTask = (DownloadOsmTask)task;
                if (osmTask.isCanceled() || osmTask.isFailed())
                    return;
            }
        }
        Set<Long> myPrimitiveIds = Main.map.mapView.getEditLayer().data.getCompletePrimitiveIds();
        Set<Long> downloadedIds = getDownloadedIds();
        myPrimitiveIds.removeAll(downloadedIds);
        myPrimitiveIds.remove(new Long(0)); // ignore new primitives
        if (! myPrimitiveIds.isEmpty()) {
            handlePotentiallyDeletedPrimitives(myPrimitiveIds);
        }
    }

    /**
     * Updates the local state of a set of primitives (given by a set of primitive
     * ids) with the state currently held on the server.
     *
     * @param potentiallyDeleted a set of ids to check update from the server
     */
    protected void updatePotentiallyDeletedPrimitives(Set<Long> potentiallyDeleted) {
        DataSet ds =  Main.map.mapView.getEditLayer().data;
        final ArrayList<OsmPrimitive> toSelect = new ArrayList<OsmPrimitive>();
        for (Long id : potentiallyDeleted) {
            OsmPrimitive primitive = ds.getPrimitiveById(id);
            if (primitive != null) {
                toSelect.add(primitive);
            }
        }
        EventQueue.invokeLater(
                new Runnable() {
                    public void run() {
                        new UpdateSelectionAction().updatePrimitives(toSelect);
                    }
                }
        );
    }

    /**
     * Processes a set of primitives (given by a set of their ids) which might be
     * deleted on the server. First prompts the user whether he wants to check
     * the current state on the server. If yes, retrieves the current state on the server
     * and checks whether the primitives are indeed deleted on the server.
     *
     * @param potentiallyDeleted a set of primitives (given by their ids)
     */
    protected void handlePotentiallyDeletedPrimitives(Set<Long> potentiallyDeleted) {
        String [] options = {
                "Check on the server",
                "Ignore"
        };

        String message = tr("<html>"
                +  "There are {0} primitives in your local dataset which<br>"
                + "might be deleted on the server. If you later try to delete or<br>"
                + "update them the server is likely to report a<br>"
                + "conflict.<br>"
                + "<br>"
                + "Click <strong>{1}</strong> to check the state of these primitives<br>"
                + "on the server.<br>"
                + "Click <strong>{2}</strong> to ignore.<br>"
                + "</html>",
                potentiallyDeleted.size(), options[0], options[1]
        );

        int ret =OptionPaneUtil.showOptionDialog(
                Main.parent,
                message,
                tr("Deleted or moved primitives"),
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE,
                options,
                options[0]
        );
        switch(ret) {
        case JOptionPane.CLOSED_OPTION: return;
        case JOptionPane.NO_OPTION: return;
        case JOptionPane.YES_OPTION: updatePotentiallyDeletedPrimitives(potentiallyDeleted); break;
        }
    }

    /**
     * replies true, if the primitive with id <code>id</code> was downloaded into the
     * dataset <code>ds</code>
     *
     * @param id the id
     * @param ds the dataset
     * @return true, if the primitive with id <code>id</code> was downloaded into the
     * dataset <code>ds</code>; false otherwise
     */
    protected boolean wasDownloaded(long id, DataSet ds) {
        OsmPrimitive primitive = ds.getPrimitiveById(id);
        return primitive != null;
    }

    /**
     * replies true, if the primitive with id <code>id</code> was downloaded into the
     * dataset of one of the download tasks
     *
     * @param id the id
     * @return true, if the primitive with id <code>id</code> was downloaded into the
     * dataset of one of the download tasks
     *
     */
    public boolean wasDownloaded(long id) {
        for (DownloadTask task : osmTasks) {
            if(task instanceof DownloadOsmTask) {
                DataSet ds = ((DownloadOsmTask)task).getDownloadedData();
                if(wasDownloaded(id,ds)) return true;
            }
        }
        return false;
    }

    /**
     * Replies the set of primitive ids which have been downloaded by this task list
     *
     * @return the set of primitive ids which have been downloaded by this task list
     */
    public Set<Long> getDownloadedIds() {
        HashSet<Long> ret = new HashSet<Long>();
        for (DownloadTask task : osmTasks) {
            if(task instanceof DownloadOsmTask) {
                DataSet ds = ((DownloadOsmTask)task).getDownloadedData();
                if (ds != null) {
                    ret.addAll(ds.getPrimitiveIds());
                }
            }
        }
        return ret;
    }
}
