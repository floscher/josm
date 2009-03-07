// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.io;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.IOException;
import java.io.InputStream;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.gpx.GpxData;
import org.xml.sax.SAXException;


public class BoundingBoxDownloader extends OsmServerReader {

    /**
     * The boundings of the desired map data.
     */
    private final double lat1;
    private final double lon1;
    private final double lat2;
    private final double lon2;

    public BoundingBoxDownloader(double lat1, double lon1, double lat2, double lon2) {
        this.lat1 = lat1;
        this.lon1 = lon1;
        this.lat2 = lat2;
        this.lon2 = lon2;
        // store the bounding box in the preferences so it can be
        // re-used across invocations of josm
        Main.pref.put("osm-download.bounds", lat1+";"+lon1+";"+lat2+";"+lon2);
    }

    /**
     * Retrieve raw gps waypoints from the server API.
     * @return A list of all primitives retrieved. Currently, the list of lists
     *      contain only one list, since the server cannot distinguish between
     *      ways.
     */
    public GpxData parseRawGps() throws IOException, SAXException {
        Main.pleaseWaitDlg.progress.setValue(0);
        Main.pleaseWaitDlg.currentAction.setText(tr("Contacting OSM Server..."));
        try {
            String url = "trackpoints?bbox="+lon1+","+lat1+","+lon2+","+lat2+"&page=";

            boolean done = false;
            GpxData result = null;
            for (int i = 0;!done;++i) {
                Main.pleaseWaitDlg.currentAction.setText(tr("Downloading points {0} to {1}...", i * 5000, ((i + 1) * 5000)));
                InputStream in = getInputStream(url+i, Main.pleaseWaitDlg);
                if (in == null)
                    break;
                GpxData currentGpx = new GpxReader(in, null).data;
                if (result == null) {
                    result = currentGpx;
                } else if (currentGpx.hasTrackPoints()) {
                    result.mergeFrom(currentGpx);
                } else{
                    done = true;
                }
                in.close();
                activeConnection = null;
            }
            result.fromServer = true;
            return result;
        } catch (IllegalArgumentException e) {
            // caused by HttpUrlConnection in case of illegal stuff in the response
            if (cancel)
                return null;
            throw new SAXException("Illegal characters within the HTTP-header response", e);
        } catch (IOException e) {
            if (cancel)
                return null;
            throw e;
        } catch (SAXException e) {
            throw e;
        } catch (Exception e) {
            if (cancel)
                return null;
            if (e instanceof RuntimeException)
                throw (RuntimeException)e;
            throw new RuntimeException(e);
        }
    }

    /**
     * Read the data from the osm server address.
     * @return A data set containing all data retrieved from that url
     */
    public DataSet parseOsm() throws SAXException, IOException {
        try {
            Main.pleaseWaitDlg.progress.setValue(0);
            Main.pleaseWaitDlg.currentAction.setText(tr("Contacting OSM Server..."));
            Main.pleaseWaitDlg.setIndeterminate(true);
            final InputStream in = getInputStream("map?bbox="+lon1+","+lat1+","+lon2+","+lat2, Main.pleaseWaitDlg);
            Main.pleaseWaitDlg.setIndeterminate(false);
            if (in == null)
                return null;
            Main.pleaseWaitDlg.currentAction.setText(tr("Downloading OSM data..."));
            final DataSet data = OsmReader.parseDataSet(in, null, Main.pleaseWaitDlg);
            in.close();
            activeConnection = null;
            return data;
        } catch (IOException e) {
            if (cancel)
                return null;
            throw e;
        } catch (SAXException e) {
            throw e;
        } catch (Exception e) {
            if (cancel)
                return null;
            if (e instanceof RuntimeException)
                throw (RuntimeException)e;
            throw new RuntimeException(e);
        }
    }
}
