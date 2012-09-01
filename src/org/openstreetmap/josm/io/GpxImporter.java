// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;

import javax.swing.JOptionPane;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.ExtensionFileFilter;
import org.openstreetmap.josm.data.gpx.GpxData;
import org.openstreetmap.josm.gui.layer.GpxLayer;
import org.openstreetmap.josm.gui.layer.markerlayer.MarkerLayer;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.xml.sax.SAXException;

/**
 * File importer allowing to import GPX files (*.gpx/gpx.gz files).
 *
 */
public class GpxImporter extends FileImporter {

    /**
     * The GPX file filter (*.gpx and *.gpx.gz files).
     */
    public static final ExtensionFileFilter FILE_FILTER = new ExtensionFileFilter(
            "gpx,gpx.gz", "gpx", tr("GPX Files") + " (*.gpx *.gpx.gz)");
    
    /**
     * Utility class containing imported GPX and marker layers, and a task to run after they are added to MapView. 
     */
    public static class GpxImporterData {
        /**
         * The imported GPX layer. May be null if no GPX data.
         */
        public GpxLayer gpxLayer;
        /**
         * The imported marker layer. May be null if no marker.
         */
        public MarkerLayer markerLayer;
        /**
         * The task to run after GPX and/or marker layer has been added to MapView.
         */
        public Runnable postLayerTask;
    }

    /**
     * Constructs a new {@code GpxImporter}.
     */
    public GpxImporter() {
        super(FILE_FILTER);
    }

    @Override
    public void importData(File file, ProgressMonitor progressMonitor) throws IOException {
        InputStream is;
        if (file.getName().endsWith(".gpx.gz")) {
            is = new GZIPInputStream(new FileInputStream(file));
        } else {
            is = new FileInputStream(file);
        }
        String fileName = file.getName();
        
        try {
            GpxReader r = new GpxReader(is);
            boolean parsedProperly = r.parse(true);
            r.data.storageFile = file;
            addLayers(loadLayers(r.data, parsedProperly, fileName, tr("Markers from {0}", fileName)));
        } catch (SAXException e) {
            e.printStackTrace();
            throw new IOException(tr("Parsing data for layer ''{0}'' failed", fileName));
        }
    }
    
    /**
     * Adds the specified GPX and marker layers to Map.main
     * @param data The layers to add
     * @see #loadLayers
     */
    public static void addLayers(final GpxImporterData data) {
        // FIXME: remove UI stuff from the IO subsystem
        GuiHelper.runInEDT(new Runnable() {
            public void run() {
                if (data.markerLayer != null) {
                    Main.main.addLayer(data.markerLayer);
                }
                if (data.gpxLayer != null) {
                    Main.main.addLayer(data.gpxLayer);
                }
                data.postLayerTask.run();
            }
        });
    }

    /**
     * Replies the new GPX and marker layers corresponding to the specified GPX data.
     * @param data The GPX data
     * @param parsedProperly True if GPX data has been properly parsed by {@link GpxReader#parse}
     * @param gpxLayerName The GPX layer name
     * @param markerLayerName The marker layer name
     * @return the new GPX and marker layers corresponding to the specified GPX data, to be used with {@link #addLayers}
     * @see #addLayers
     */
    public static GpxImporterData loadLayers(final GpxData data, final boolean parsedProperly, final String gpxLayerName, String markerLayerName) {
        final GpxImporterData result = new GpxImporterData();
        if (data.hasRoutePoints() || data.hasTrackPoints()) {
            result.gpxLayer = new GpxLayer(data, gpxLayerName, data.storageFile != null);
        }
        if (Main.pref.getBoolean("marker.makeautomarkers", true) && !data.waypoints.isEmpty()) {
            result.markerLayer = new MarkerLayer(data, markerLayerName, data.storageFile, result.gpxLayer, false);
            if (result.markerLayer.data.size() == 0) {
                result.markerLayer = null;
            }
        }
        result.postLayerTask = new Runnable() {
            @Override
            public void run() {
                if (result.markerLayer != null) {
                    result.markerLayer.addMouseHandler();
                }
                if (!parsedProperly) {
                    String msg;
                    if (data.storageFile == null) {
                        msg = tr("Error occurred while parsing gpx data for layer ''{0}''. Only a part of the file will be available.",
                                gpxLayerName);
                    } else {
                        msg = tr("Error occurred while parsing gpx file ''{0}''. Only a part of the file will be available.",
                                data.storageFile.getPath());
                    }
                    JOptionPane.showMessageDialog(null, msg);
                }
            }
        };
        return result;
    }
}
