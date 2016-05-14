// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.gpx.GpxData;
import org.openstreetmap.josm.data.notes.Note;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.tools.HttpClient;

/**
 * This DataReader reads directly from the REST API of the osm server.
 *
 * It supports plain text transfer as well as gzip or deflate encoded transfers;
 * if compressed transfers are unwanted, set property osm-server.use-compression
 * to false.
 *
 * @author imi
 */
public abstract class OsmServerReader extends OsmConnection {
    private final OsmApi api = OsmApi.getOsmApi();
    private boolean doAuthenticate;
    protected boolean gpxParsedProperly;

    /**
     * Open a connection to the given url and return a reader on the input stream
     * from that connection. In case of user cancel, return <code>null</code>.
     * Relative URL's are directed to API base URL.
     * @param urlStr The url to connect to.
     * @param progressMonitor progress monitoring and abort handler
     * @return A reader reading the input stream (servers answer) or <code>null</code>.
     * @throws OsmTransferException if data transfer errors occur
     */
    protected InputStream getInputStream(String urlStr, ProgressMonitor progressMonitor) throws OsmTransferException  {
        return getInputStream(urlStr, progressMonitor, null);
    }

    /**
     * Open a connection to the given url and return a reader on the input stream
     * from that connection. In case of user cancel, return <code>null</code>.
     * Relative URL's are directed to API base URL.
     * @param urlStr The url to connect to.
     * @param progressMonitor progress monitoring and abort handler
     * @param reason The reason to show on console. Can be {@code null} if no reason is given
     * @return A reader reading the input stream (servers answer) or <code>null</code>.
     * @throws OsmTransferException if data transfer errors occur
     */
    protected InputStream getInputStream(String urlStr, ProgressMonitor progressMonitor, String reason) throws OsmTransferException  {
        try {
            api.initialize(progressMonitor);
            String url = urlStr.startsWith("http") ? urlStr : (getBaseUrl() + urlStr);
            return getInputStreamRaw(url, progressMonitor, reason);
        } finally {
            progressMonitor.invalidate();
        }
    }

    /**
     * Return the base URL for relative URL requests
     * @return base url of API
     */
    protected String getBaseUrl() {
        return api.getBaseUrl();
    }

    /**
     * Open a connection to the given url and return a reader on the input stream
     * from that connection. In case of user cancel, return <code>null</code>.
     * @param urlStr The exact url to connect to.
     * @param progressMonitor progress monitoring and abort handler
     * @return An reader reading the input stream (servers answer) or <code>null</code>.
     * @throws OsmTransferException if data transfer errors occur
     */
    protected InputStream getInputStreamRaw(String urlStr, ProgressMonitor progressMonitor) throws OsmTransferException {
        return getInputStreamRaw(urlStr, progressMonitor, null);
    }

    /**
     * Open a connection to the given url and return a reader on the input stream
     * from that connection. In case of user cancel, return <code>null</code>.
     * @param urlStr The exact url to connect to.
     * @param progressMonitor progress monitoring and abort handler
     * @param reason The reason to show on console. Can be {@code null} if no reason is given
     * @return An reader reading the input stream (servers answer) or <code>null</code>.
     * @throws OsmTransferException if data transfer errors occur
     */
    protected InputStream getInputStreamRaw(String urlStr, ProgressMonitor progressMonitor, String reason) throws OsmTransferException {
        return getInputStreamRaw(urlStr, progressMonitor, reason, false);
    }

    /**
     * Open a connection to the given url and return a reader on the input stream
     * from that connection. In case of user cancel, return <code>null</code>.
     * @param urlStr The exact url to connect to.
     * @param progressMonitor progress monitoring and abort handler
     * @param reason The reason to show on console. Can be {@code null} if no reason is given
     * @param uncompressAccordingToContentDisposition Whether to inspect the HTTP header {@code Content-Disposition}
     *                                                for {@code filename} and uncompress a gzip/bzip2 stream.
     * @return An reader reading the input stream (servers answer) or <code>null</code>.
     * @throws OsmTransferException if data transfer errors occur
     */
    @SuppressWarnings("resource")
    protected InputStream getInputStreamRaw(String urlStr, ProgressMonitor progressMonitor, String reason,
            boolean uncompressAccordingToContentDisposition) throws OsmTransferException {
        try {
            OnlineResource.JOSM_WEBSITE.checkOfflineAccess(urlStr, Main.getJOSMWebsite());
            OnlineResource.OSM_API.checkOfflineAccess(urlStr, OsmApi.getOsmApi().getServerUrl());

            URL url = null;
            try {
                url = new URL(urlStr.replace(" ", "%20"));
            } catch (MalformedURLException e) {
                throw new OsmTransferException(e);
            }

            if ("file".equals(url.getProtocol())) {
                try {
                    return url.openStream();
                } catch (IOException e) {
                    throw new OsmTransferException(e);
                }
            }

            final HttpClient client = HttpClient.create(url);
            activeConnection = client;
            client.setReasonForRequest(reason);
            adaptRequest(client);
            if (doAuthenticate) {
                addAuth(client);
            }
            if (cancel)
                throw new OsmTransferCanceledException("Operation canceled");

            final HttpClient.Response response;
            try {
                response = client.connect(progressMonitor);
            } catch (IOException e) {
                Main.error(e);
                OsmTransferException ote = new OsmTransferException(
                        tr("Could not connect to the OSM server. Please check your internet connection."), e);
                ote.setUrl(url.toString());
                throw ote;
            }
            try {
                if (response.getResponseCode() == HttpURLConnection.HTTP_UNAUTHORIZED)
                    throw new OsmApiException(HttpURLConnection.HTTP_UNAUTHORIZED, null, null);

                if (response.getResponseCode() == HttpURLConnection.HTTP_PROXY_AUTH)
                    throw new OsmTransferCanceledException("Proxy Authentication Required");

                if (response.getResponseCode() != HttpURLConnection.HTTP_OK) {
                    String errorHeader = response.getHeaderField("Error");
                    String errorBody;
                    try {
                        errorBody = response.fetchContent();
                    } catch (IOException e) {
                        errorBody = tr("Reading error text failed.");
                    }
                    throw new OsmApiException(response.getResponseCode(), errorHeader, errorBody, url.toString());
                }

                response.uncompressAccordingToContentDisposition(uncompressAccordingToContentDisposition);
                return response.getContent();
            } catch (OsmTransferException e) {
                throw e;
            } catch (IOException e) {
                throw new OsmTransferException(e);
            }
        } finally {
            progressMonitor.invalidate();
        }
    }

    /**
     * Allows subclasses to modify the request.
     * @param request the prepared request
     * @since 9308
     */
    protected void adaptRequest(HttpClient request) {
    }

    /**
     * Download OSM files from somewhere
     * @param progressMonitor The progress monitor
     * @return The corresponding dataset
     * @throws OsmTransferException if any error occurs
     */
    public abstract DataSet parseOsm(final ProgressMonitor progressMonitor) throws OsmTransferException;

    /**
     * Download OSM Change files from somewhere
     * @param progressMonitor The progress monitor
     * @return The corresponding dataset
     * @throws OsmTransferException if any error occurs
     */
    public DataSet parseOsmChange(final ProgressMonitor progressMonitor) throws OsmTransferException {
        return null;
    }

    /**
     * Download BZip2-compressed OSM Change files from somewhere
     * @param progressMonitor The progress monitor
     * @return The corresponding dataset
     * @throws OsmTransferException if any error occurs
     */
    public DataSet parseOsmChangeBzip2(final ProgressMonitor progressMonitor) throws OsmTransferException {
        return null;
    }

    /**
     * Download GZip-compressed OSM Change files from somewhere
     * @param progressMonitor The progress monitor
     * @return The corresponding dataset
     * @throws OsmTransferException if any error occurs
     */
    public DataSet parseOsmChangeGzip(final ProgressMonitor progressMonitor) throws OsmTransferException {
        return null;
    }

    /**
     * Retrieve raw gps waypoints from the server API.
     * @param progressMonitor The progress monitor
     * @return The corresponding GPX tracks
     * @throws OsmTransferException if any error occurs
     */
    public GpxData parseRawGps(final ProgressMonitor progressMonitor) throws OsmTransferException {
        return null;
    }

    /**
     * Retrieve BZip2-compressed GPX files from somewhere.
     * @param progressMonitor The progress monitor
     * @return The corresponding GPX tracks
     * @throws OsmTransferException if any error occurs
     * @since 6244
     */
    public GpxData parseRawGpsBzip2(final ProgressMonitor progressMonitor) throws OsmTransferException {
        return null;
    }

    /**
     * Download BZip2-compressed OSM files from somewhere
     * @param progressMonitor The progress monitor
     * @return The corresponding dataset
     * @throws OsmTransferException if any error occurs
     */
    public DataSet parseOsmBzip2(final ProgressMonitor progressMonitor) throws OsmTransferException {
        return null;
    }

    /**
     * Download GZip-compressed OSM files from somewhere
     * @param progressMonitor The progress monitor
     * @return The corresponding dataset
     * @throws OsmTransferException if any error occurs
     */
    public DataSet parseOsmGzip(final ProgressMonitor progressMonitor) throws OsmTransferException {
        return null;
    }

    /**
     * Download Zip-compressed OSM files from somewhere
     * @param progressMonitor The progress monitor
     * @return The corresponding dataset
     * @throws OsmTransferException if any error occurs
     * @since 6882
     */
    public DataSet parseOsmZip(final ProgressMonitor progressMonitor) throws OsmTransferException {
        return null;
    }

    /**
     * Returns true if this reader is adding authentication credentials to the read
     * request sent to the server.
     *
     * @return true if this reader is adding authentication credentials to the read
     * request sent to the server
     */
    public boolean isDoAuthenticate() {
        return doAuthenticate;
    }

    /**
     * Sets whether this reader adds authentication credentials to the read
     * request sent to the server.
     *
     * @param doAuthenticate  true if  this reader adds authentication credentials to the read
     * request sent to the server
     */
    public void setDoAuthenticate(boolean doAuthenticate) {
        this.doAuthenticate = doAuthenticate;
    }

    /**
     * Determines if the GPX data has been parsed properly.
     * @return true if the GPX data has been parsed properly, false otherwise
     * @see GpxReader#parse
     */
    public final boolean isGpxParsedProperly() {
        return gpxParsedProperly;
    }

    /**
     * Downloads notes from the API, given API limit parameters
     *
     * @param noteLimit How many notes to download.
     * @param daysClosed Return notes closed this many days in the past. -1 means all notes, ever. 0 means only unresolved notes.
     * @param progressMonitor Progress monitor for user feedback
     * @return List of notes returned by the API
     * @throws OsmTransferException if any errors happen
     */
    public List<Note> parseNotes(int noteLimit, int daysClosed, ProgressMonitor progressMonitor) throws OsmTransferException {
        return null;
    }

    /**
     * Downloads notes from a given raw URL. The URL is assumed to be complete and no API limits are added
     *
     * @param progressMonitor progress monitor
     * @return A list of notes parsed from the URL
     * @throws OsmTransferException if any error occurs during dialog with OSM API
     */
    public List<Note> parseRawNotes(final ProgressMonitor progressMonitor) throws OsmTransferException {
        return null;
    }

    /**
     * Download notes from a URL that contains a bzip2 compressed notes dump file
     * @param progressMonitor progress monitor
     * @return A list of notes parsed from the URL
     * @throws OsmTransferException if any error occurs during dialog with OSM API
     */
    public List<Note> parseRawNotesBzip2(final ProgressMonitor progressMonitor) throws OsmTransferException {
        return null;
    }
}
