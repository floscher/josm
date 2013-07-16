// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.io;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.IOException;
import java.io.InputStream;

import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.gpx.GpxData;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.tools.Utils;
import org.xml.sax.SAXException;

public class BoundingBoxDownloader extends OsmServerReader {

    /**
     * The boundings of the desired map data.
     */
    protected final double lat1;
    protected final double lon1;
    protected final double lat2;
    protected final double lon2;
    protected final boolean crosses180th;

    public BoundingBoxDownloader(Bounds downloadArea) {
        this.lat1 = downloadArea.getMin().lat();
        this.lon1 = downloadArea.getMin().lon();
        this.lat2 = downloadArea.getMax().lat();
        this.lon2 = downloadArea.getMax().lon();
        this.crosses180th = downloadArea.crosses180thMeridian();
    }

    private GpxData downloadRawGps(String url, ProgressMonitor progressMonitor) throws IOException, OsmTransferException, SAXException {
        boolean done = false;
        GpxData result = null;
        for (int i = 0;!done;++i) {
            progressMonitor.subTask(tr("Downloading points {0} to {1}...", i * 5000, ((i + 1) * 5000)));
            InputStream in = getInputStream(url+i, progressMonitor.createSubTaskMonitor(1, true));
            if (in == null) {
                break;
            }
            progressMonitor.setTicks(0);
            GpxReader reader = new GpxReader(in);
            gpxParsedProperly = reader.parse(false);
            GpxData currentGpx = reader.getGpxData();
            if (result == null) {
                result = currentGpx;
            } else if (currentGpx.hasTrackPoints()) {
                result.mergeFrom(currentGpx);
            } else{
                done = true;
            }
            Utils.close(in);
            activeConnection = null;
        }
        result.fromServer = true;
        return result;
    }

    /**
     * Retrieve raw gps waypoints from the server API.
     * @return A list of all primitives retrieved. Currently, the list of lists
     *      contain only one list, since the server cannot distinguish between
     *      ways.
     */
    @Override
    public GpxData parseRawGps(ProgressMonitor progressMonitor) throws OsmTransferException {
        progressMonitor.beginTask("", 1);
        try {
            progressMonitor.indeterminateSubTask(tr("Contacting OSM Server..."));
            if (crosses180th) {
                // API 0.6 does not support requests crossing the 180th meridian, so make two requests
                GpxData result = downloadRawGps("trackpoints?bbox="+lon1+","+lat1+",180.0,"+lat2+"&page=", progressMonitor);
                result.mergeFrom(downloadRawGps("trackpoints?bbox=-180.0,"+lat1+","+lon2+","+lat2+"&page=", progressMonitor));
                return result;
            } else {
                // Simple request
                return downloadRawGps("trackpoints?bbox="+lon1+","+lat1+","+lon2+","+lat2+"&page=", progressMonitor);
            }
        } catch (IllegalArgumentException e) {
            // caused by HttpUrlConnection in case of illegal stuff in the response
            if (cancel)
                return null;
            throw new OsmTransferException("Illegal characters within the HTTP-header response.", e);
        } catch (IOException e) {
            if (cancel)
                return null;
            throw new OsmTransferException(e);
        } catch (SAXException e) {
            throw new OsmTransferException(e);
        } catch (OsmTransferException e) {
            throw e;
        } catch (RuntimeException e) {
            if (cancel)
                return null;
            throw e;
        } finally {
            progressMonitor.finishTask();
        }
    }

    protected String getRequestForBbox(double lon1, double lat1, double lon2, double lat2) {
        return "map?bbox=" + lon1 + "," + lat1 + "," + lon2 + "," + lat2;
    }

    /**
     * Read the data from the osm server address.
     * @return A data set containing all data retrieved from that url
     */
    @Override
    public DataSet parseOsm(ProgressMonitor progressMonitor) throws OsmTransferException {
        progressMonitor.beginTask(tr("Contacting OSM Server..."), 10);
        InputStream in = null;
        try {
            DataSet ds = null;
            progressMonitor.indeterminateSubTask(null);
            if (crosses180th) {
                // API 0.6 does not support requests crossing the 180th meridian, so make two requests
                in = getInputStream(getRequestForBbox(lon1, lat1, 180.0, lat2), progressMonitor.createSubTaskMonitor(9, false));
                if (in == null)
                    return null;
                ds = OsmReader.parseDataSet(in, progressMonitor.createSubTaskMonitor(1, false));

                in = getInputStream(getRequestForBbox(-180.0, lat1, lon2, lat2), progressMonitor.createSubTaskMonitor(9, false));
                if (in == null)
                    return null;
                DataSet ds2 = OsmReader.parseDataSet(in, progressMonitor.createSubTaskMonitor(1, false));
                if (ds2 == null)
                    return null;
                ds.mergeFrom(ds2);

            } else {
                // Simple request
                in = getInputStream(getRequestForBbox(lon1, lat1, lon2, lat2), progressMonitor.createSubTaskMonitor(9, false));
                if (in == null)
                    return null;
                ds = OsmReader.parseDataSet(in, progressMonitor.createSubTaskMonitor(1, false));
            }
            return ds;
        } catch(OsmTransferException e) {
            throw e;
        } catch (Exception e) {
            throw new OsmTransferException(e);
        } finally {
            progressMonitor.finishTask();
            Utils.close(in);
            activeConnection = null;
        }
    }
}
