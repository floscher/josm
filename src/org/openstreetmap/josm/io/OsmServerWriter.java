// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.io;

import static org.openstreetmap.josm.tools.I18n.marktr;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.UploadAction;
import org.openstreetmap.josm.data.osm.Changeset;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;

/**
 * Class that uploads all changes to the osm server.
 *
 * This is done like this: - All objects with id = 0 are uploaded as new, except
 * those in deleted, which are ignored - All objects in deleted list are
 * deleted. - All remaining objects with modified flag set are updated.
 */
public class OsmServerWriter {
    static private final Logger logger = Logger.getLogger(OsmServerWriter.class.getName());

    /**
     * This list contains all successfully processed objects. The caller of
     * upload* has to check this after the call and update its dataset.
     *
     * If a server connection error occurs, this may contain fewer entries
     * than where passed in the list to upload*.
     */
    private Collection<OsmPrimitive> processed;

    private OsmApi api = OsmApi.getOsmApi();

    private static final int MSECS_PER_SECOND = 1000;
    private static final int SECONDS_PER_MINUTE = 60;
    private static final int MSECS_PER_MINUTE = MSECS_PER_SECOND * SECONDS_PER_MINUTE;

    long uploadStartTime;

    public String timeLeft(int progress, int list_size) {
        long now = System.currentTimeMillis();
        long elapsed = now - uploadStartTime;
        if (elapsed == 0) {
            elapsed = 1;
        }
        float uploads_per_ms = (float)progress / elapsed;
        float uploads_left = list_size - progress;
        int ms_left = (int)(uploads_left / uploads_per_ms);
        int minutes_left = ms_left / MSECS_PER_MINUTE;
        int seconds_left = (ms_left / MSECS_PER_SECOND) % SECONDS_PER_MINUTE ;
        String time_left_str = Integer.toString(minutes_left) + ":";
        if (seconds_left < 10) {
            time_left_str += "0";
        }
        time_left_str += Integer.toString(seconds_left);
        return time_left_str;
    }

    /**
     * retrieves the most recent changeset comment from the preferences
     *
     * @return the most recent changeset comment
     */
    protected String getChangesetComment() {
        String cmt = "";
        List<String> history = new LinkedList<String>(
                Main.pref.getCollection(UploadAction.HISTORY_KEY, new LinkedList<String>()));
        if(history.size() > 0) {
            cmt = history.get(0);
        }
        return cmt;
    }

    /**
     * Uploads the changes individually. Invokes one API call per uploaded primitmive.
     * 
     * @param primitives the collection of primitives to upload
     * @param changeset the changeset to be used if <code>changesetProcessingType</code> indicates that
     *   a new changeset should be opened
     * @param changesetProcessingType how we handle changesets
     * @param progressMonitor the progress monitor
     * @throws OsmTransferException thrown if an exception occurs
     */
    protected void uploadChangesIndividually(Collection<OsmPrimitive> primitives, Changeset changeset, ChangesetProcessingType changesetProcessingType, ProgressMonitor progressMonitor) throws OsmTransferException {
        try {
            progressMonitor.setTicksCount(primitives.size());
            api.createChangeset(changeset, changesetProcessingType,progressMonitor.createSubTaskMonitor(0, false));
            uploadStartTime = System.currentTimeMillis();
            for (OsmPrimitive osm : primitives) {
                int progress = progressMonitor.getTicks();
                String time_left_str = timeLeft(progress, primitives.size());
                String msg = "";
                switch(OsmPrimitiveType.from(osm)) {
                    case NODE: msg = marktr("{0}% ({1}/{2}), {3} left. Uploading node ''{4}'' (id: {5})"); break;
                    case WAY: msg = marktr("{0}% ({1}/{2}), {3} left. Uploading way ''{4}'' (id: {5})"); break;
                    case RELATION: msg = marktr("{0}% ({1}/{2}), {3} left. Uploading relation ''{4}'' (id: {5})"); break;
                }
                progressMonitor.subTask(
                        tr(msg,
                                Math.round(100.0*progress/primitives.size()),
                                progress,
                                primitives.size(),
                                time_left_str,
                                osm.getName() == null ? osm.getId() : osm.getName(),
                                        osm.getId()));
                makeApiRequest(osm,progressMonitor);
                processed.add(osm);
                progressMonitor.worked(1);
            }
        } catch(OsmTransferException e) {
            throw e;
        } catch(Exception e) {
            throw new OsmTransferException(e);
        } finally {
            try {
                // starting the changeset may have failed, for instance because the user
                // cancelled the upload task. Only close the changeset if we currently have
                // an open changeset

                if (api.getCurrentChangeset() != null && api.getCurrentChangeset().getId() > 0) {
                    api.stopChangeset(changesetProcessingType, progressMonitor.createSubTaskMonitor(0, false));
                }
            } catch(Exception e) {
                OsmChangesetCloseException closeException = new OsmChangesetCloseException(e);
                closeException.setChangeset(api.getCurrentChangeset());
                throw closeException;
            }
        }
    }

    /**
     * Upload all changes in one diff upload
     * 
     * @param primitives the collection of primitives to upload
     * @param progressMonitor  the progress monitor
     * @throws OsmTransferException thrown if an exception occurs
     */
    protected void uploadChangesAsDiffUpload(Collection<OsmPrimitive> primitives, Changeset changeset, ChangesetProcessingType changesetProcessingType, ProgressMonitor progressMonitor) throws OsmTransferException {
        // upload everything in one changeset
        //
        try {
            api.createChangeset(changeset, changesetProcessingType, progressMonitor.createSubTaskMonitor(0, false));
            processed.addAll(api.uploadDiff(primitives, progressMonitor.createSubTaskMonitor(ProgressMonitor.ALL_TICKS, false)));
        } catch(OsmTransferException e) {
            throw e;
        } catch(Exception e) {
            throw new OsmTransferException(e);
        } finally {
            try {
                api.stopChangeset(changesetProcessingType, progressMonitor.createSubTaskMonitor(0, false));
            } catch (Exception ee) {
                OsmChangesetCloseException closeException = new OsmChangesetCloseException(ee);
                closeException.setChangeset(api.getCurrentChangeset());
                throw closeException;
            }
        }
    }

    /**
     * Send the dataset to the server.
     *
     * @param apiVersion version of the data set
     * @param primitives list of objects to send
     */
    public void uploadOsm(String apiVersion, Collection<OsmPrimitive> primitives, Changeset changeset, ChangesetProcessingType changesetProcessingType, ProgressMonitor progressMonitor) throws OsmTransferException {
        processed = new LinkedList<OsmPrimitive>();

        api.initialize(progressMonitor);

        try {
            // check whether we can use diff upload
            //
            boolean casUseDiffUploads = api.hasSupportForDiffUploads();
            boolean useDiffUpload = Main.pref.getBoolean("osm-server.atomic-upload", apiVersion.compareTo("0.6")>=0);
            if (useDiffUpload && ! casUseDiffUploads) {
                System.out.println(tr("WARNING: preference ''{0}'' or api version ''{1}'' of dataset requires to use diff uploads, but API is not able to handle them. Ignoring diff upload.", "osm-server.atomic-upload", apiVersion));
                useDiffUpload = false;
            }

            if (useDiffUpload) {
                progressMonitor.beginTask(tr("Starting to upload in one request ..."));
                uploadChangesAsDiffUpload(primitives,changeset, changesetProcessingType, progressMonitor);
            } else {
                progressMonitor.beginTask(tr("Starting to upload with one request per primitive ..."));
                uploadChangesIndividually(primitives,changeset,changesetProcessingType,  progressMonitor);
            }
        } finally {
            progressMonitor.finishTask();
        }
    }

    void makeApiRequest(OsmPrimitive osm, ProgressMonitor progressMonitor) throws OsmTransferException {
        if (osm.isDeleted()) {
            api.deletePrimitive(osm, progressMonitor);
        } else if (osm.getId() == 0) {
            api.createPrimitive(osm, progressMonitor);
        } else {
            api.modifyPrimitive(osm,progressMonitor);
        }
    }

    public void cancel() {
        if (api != null) {
            api.cancel();
        }
    }

    /**
     * Replies the collection of successfully processed primitives
     * 
     * @return the collection of successfully processed primitives
     */
    public Collection<OsmPrimitive> getProcessedPrimitives() {
        return processed;
    }
}
