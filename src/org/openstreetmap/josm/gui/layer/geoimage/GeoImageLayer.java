// License: GPL. See LICENSE file for details.
// Copyright 2007 by Christian Gallioz (aka khris78)
// Parts of code from Geotagged plugin (by Rob Neild)
// and the core JOSM source code (by Immanuel Scholz and others)
package org.openstreetmap.josm.gui.layer.geoimage;

import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trn;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;

import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.SwingConstants;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.RenameLayerAction;
import org.openstreetmap.josm.actions.mapmode.MapMode;
import org.openstreetmap.josm.actions.mapmode.SelectAction;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.visitor.BoundingXYVisitor;
import org.openstreetmap.josm.gui.ExtendedDialog;
import org.openstreetmap.josm.gui.MapFrame;
import org.openstreetmap.josm.gui.MapFrame.MapModeChangeListener;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.gui.MapView.LayerChangeListener;
import org.openstreetmap.josm.gui.NavigatableComponent;
import org.openstreetmap.josm.gui.PleaseWaitRunnable;
import org.openstreetmap.josm.gui.dialogs.LayerListDialog;
import org.openstreetmap.josm.gui.dialogs.LayerListPopup;
import org.openstreetmap.josm.gui.layer.GpxLayer;
import org.openstreetmap.josm.gui.layer.JumpToMarkerActions.JumpToMarkerLayer;
import org.openstreetmap.josm.gui.layer.JumpToMarkerActions.JumpToNextMarker;
import org.openstreetmap.josm.gui.layer.JumpToMarkerActions.JumpToPreviousMarker;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.tools.ExifReader;
import org.openstreetmap.josm.tools.ImageProvider;

import com.drew.imaging.jpeg.JpegMetadataReader;
import com.drew.lang.CompoundException;
import com.drew.lang.Rational;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.MetadataException;
import com.drew.metadata.exif.ExifDirectory;
import com.drew.metadata.exif.GpsDirectory;

public class GeoImageLayer extends Layer implements PropertyChangeListener, JumpToMarkerLayer {

    List<ImageEntry> data;
    GpxLayer gpxLayer;

    private Icon icon = ImageProvider.get("dialogs/geoimage/photo-marker");
    private Icon selectedIcon = ImageProvider.get("dialogs/geoimage/photo-marker-selected");

    private int currentPhoto = -1;

    boolean useThumbs = false;
    ThumbsLoader thumbsloader;
    boolean thumbsLoaded = false;
    private BufferedImage offscreenBuffer;
    boolean updateOffscreenBuffer = true;

    /** Loads a set of images, while displaying a dialog that indicates what the plugin is currently doing.
     * In facts, this object is instantiated with a list of files. These files may be JPEG files or
     * directories. In case of directories, they are scanned to find all the images they contain.
     * Then all the images that have be found are loaded as ImageEntry instances.
     */
    private static final class Loader extends PleaseWaitRunnable {

        private boolean canceled = false;
        private GeoImageLayer layer;
        private Collection<File> selection;
        private HashSet<String> loadedDirectories = new HashSet<String>();
        private LinkedHashSet<String> errorMessages;
        private GpxLayer gpxLayer;

        protected void rememberError(String message) {
            this.errorMessages.add(message);
        }

        public Loader(Collection<File> selection, GpxLayer gpxLayer) {
            super(tr("Extracting GPS locations from EXIF"));
            this.selection = selection;
            this.gpxLayer = gpxLayer;
            errorMessages = new LinkedHashSet<String>();
        }

        @Override protected void realRun() throws IOException {

            progressMonitor.subTask(tr("Starting directory scan"));
            Collection<File> files = new ArrayList<File>();
            try {
                addRecursiveFiles(files, selection);
            } catch(NullPointerException npe) {
                rememberError(tr("One of the selected files was null"));
            }

            if (canceled)
                return;
            progressMonitor.subTask(tr("Read photos..."));
            progressMonitor.setTicksCount(files.size());

            progressMonitor.subTask(tr("Read photos..."));
            progressMonitor.setTicksCount(files.size());

            // read the image files
            List<ImageEntry> data = new ArrayList<ImageEntry>(files.size());

            for (File f : files) {

                if (canceled) {
                    break;
                }

                progressMonitor.subTask(tr("Reading {0}...", f.getName()));
                progressMonitor.worked(1);

                ImageEntry e = new ImageEntry();

                // Changed to silently cope with no time info in exif. One case
                // of person having time that couldn't be parsed, but valid GPS info

                try {
                    e.setExifTime(ExifReader.readTime(f));
                } catch (ParseException e1) {
                    e.setExifTime(null);
                }
                e.setFile(f);
                extractExif(e);
                data.add(e);
            }
            layer = new GeoImageLayer(data, gpxLayer);
            files.clear();
        }

        private void addRecursiveFiles(Collection<File> files, Collection<File> sel) {
            boolean nullFile = false;

            for (File f : sel) {

                if(canceled) {
                    break;
                }

                if (f == null) {
                    nullFile = true;

                } else if (f.isDirectory()) {
                    String canonical = null;
                    try {
                        canonical = f.getCanonicalPath();
                    } catch (IOException e) {
                        e.printStackTrace();
                        rememberError(tr("Unable to get canonical path for directory {0}\n",
                                f.getAbsolutePath()));
                    }

                    if (canonical == null || loadedDirectories.contains(canonical)) {
                        continue;
                    } else {
                        loadedDirectories.add(canonical);
                    }

                    Collection<File> children = Arrays.asList(f.listFiles(JpegFileFilter.getInstance()));
                    if (children != null) {
                        progressMonitor.subTask(tr("Scanning directory {0}", f.getPath()));
                        try {
                            addRecursiveFiles(files, children);
                        } catch(NullPointerException npe) {
                            npe.printStackTrace();
                            rememberError(tr("Found null file in directory {0}\n", f.getPath()));
                        }
                    } else {
                        rememberError(tr("Error while getting files from directory {0}\n", f.getPath()));
                    }

                } else {
                    files.add(f);
                }
            }

            if (nullFile)
                throw new NullPointerException();
        }

        protected String formatErrorMessages() {
            StringBuilder sb = new StringBuilder();
            sb.append("<html>");
            if (errorMessages.size() == 1) {
                sb.append(errorMessages.iterator().next());
            } else {
                sb.append("<ul>");
                for (String msg: errorMessages) {
                    sb.append("<li>").append(msg).append("</li>");
                }
                sb.append("/ul>");
            }
            sb.append("</html>");
            return sb.toString();
        }

        @Override protected void finish() {
            if (!errorMessages.isEmpty()) {
                JOptionPane.showMessageDialog(
                        Main.parent,
                        formatErrorMessages(),
                        tr("Error"),
                        JOptionPane.ERROR_MESSAGE
                        );
            }
            if (layer != null) {
                Main.main.addLayer(layer);

                if (! canceled && layer.data.size() > 0) {
                    boolean noGeotagFound = true;
                    for (ImageEntry e : layer.data) {
                        if (e.getPos() != null) {
                            noGeotagFound = false;
                        }
                    }
                    if (noGeotagFound) {
                        new CorrelateGpxWithImages(layer).actionPerformed(null);
                    }
                }
            }
        }

        @Override protected void cancel() {
            canceled = true;
        }
    }

    public static void create(Collection<File> files, GpxLayer gpxLayer) {
        Loader loader = new Loader(files, gpxLayer);
        Main.worker.execute(loader);
    }

    public GeoImageLayer(final List<ImageEntry> data, GpxLayer gpxLayer) {

        super(tr("Geotagged Images"));

        Collections.sort(data);
        this.data = data;
        this.gpxLayer = gpxLayer;
    }

    @Override
    public Icon getIcon() {
        return ImageProvider.get("dialogs/geoimage");
    }

    private static List<Action> menuAdditions = new LinkedList<Action>();
    public static void registerMenuAddition(Action addition) {
        menuAdditions.add(addition);
    }

    @Override
    public Action[] getMenuEntries() {

        List<Action> entries = new ArrayList<Action>();
        entries.add(LayerListDialog.getInstance().createShowHideLayerAction());
        entries.add(LayerListDialog.getInstance().createDeleteLayerAction());
        entries.add(new RenameLayerAction(null, this));
        entries.add(SeparatorLayerAction.INSTANCE);
        entries.add(new CorrelateGpxWithImages(this));
        if (!menuAdditions.isEmpty()) {
            entries.add(SeparatorLayerAction.INSTANCE);
            entries.addAll(menuAdditions);
        }
        entries.add(SeparatorLayerAction.INSTANCE);
        entries.add(new JumpToNextMarker(this));
        entries.add(new JumpToPreviousMarker(this));
        entries.add(SeparatorLayerAction.INSTANCE);
        entries.add(new LayerListPopup.InfoAction(this));

        return entries.toArray(new Action[entries.size()]);

    }

    private String infoText() {
        int i = 0;
        for (ImageEntry e : data)
            if (e.getPos() != null) {
                i++;
            }
        return trn("{0} image loaded.", "{0} images loaded.", data.size(), data.size())
                + " " + trn("{0} was found to be GPS tagged.", "{0} were found to be GPS tagged.", i, i);
    }

    @Override public Object getInfoComponent() {
        return infoText();
    }

    @Override
    public String getToolTipText() {
        return infoText();
    }

    @Override
    public boolean isMergable(Layer other) {
        return other instanceof GeoImageLayer;
    }

    @Override
    public void mergeFrom(Layer from) {
        GeoImageLayer l = (GeoImageLayer) from;

        ImageEntry selected = null;
        if (l.currentPhoto >= 0) {
            selected = l.data.get(l.currentPhoto);
        }

        data.addAll(l.data);
        Collections.sort(data);

        // Supress the double photos.
        if (data.size() > 1) {
            ImageEntry cur;
            ImageEntry prev = data.get(data.size() - 1);
            for (int i = data.size() - 2; i >= 0; i--) {
                cur = data.get(i);
                if (cur.getFile().equals(prev.getFile())) {
                    data.remove(i);
                } else {
                    prev = cur;
                }
            }
        }

        if (selected != null) {
            for (int i = 0; i < data.size() ; i++) {
                if (data.get(i) == selected) {
                    currentPhoto = i;
                    ImageViewerDialog.showImage(GeoImageLayer.this, data.get(i));
                    break;
                }
            }
        }

        setName(l.getName());
    }

    private Dimension scaledDimension(Image thumb) {
        final double d = Main.map.mapView.getDist100Pixel();
        final double size = 10 /*meter*/;     /* size of the photo on the map */
        double s = size * 100 /*px*/ / d;

        final double sMin = ThumbsLoader.minSize;
        final double sMax = ThumbsLoader.maxSize;

        if (s < sMin) {
            s = sMin;
        }
        if (s > sMax) {
            s = sMax;
        }
        final double f = s / sMax;  /* scale factor */

        if (thumb == null)
            return null;

        return new Dimension(
                (int) Math.round(f * thumb.getWidth(null)),
                (int) Math.round(f * thumb.getHeight(null)));
    }

    @Override
    public void paint(Graphics2D g, MapView mv, Bounds bounds) {
        int width = mv.getWidth();
        int height = mv.getHeight();
        Rectangle clip = g.getClipBounds();
        if (useThumbs) {
            if (null == offscreenBuffer || offscreenBuffer.getWidth() != width  // reuse the old buffer if possible
                    || offscreenBuffer.getHeight() != height) {
                offscreenBuffer = new BufferedImage(width, height,
                        BufferedImage.TYPE_INT_ARGB);
                updateOffscreenBuffer = true;
            }

            if (updateOffscreenBuffer) {
                Graphics2D tempG = offscreenBuffer.createGraphics();
                tempG.setColor(new Color(0,0,0,0));
                Composite saveComp = tempG.getComposite();
                tempG.setComposite(AlphaComposite.Clear);   // remove the old images
                tempG.fillRect(0, 0, width, height);
                tempG.setComposite(saveComp);

                for (ImageEntry e : data) {
                    if (e.getPos() == null) {
                        continue;
                    }
                    Point p = mv.getPoint(e.getPos());
                    if (e.thumbnail != null) {
                        Dimension d = scaledDimension(e.thumbnail);
                        Rectangle target = new Rectangle(p.x - d.width / 2, p.y - d.height / 2, d.width, d.height);
                        if (clip.intersects(target)) {
                            tempG.drawImage(e.thumbnail, target.x, target.y, target.width, target.height, null);
                        }
                    }
                    else { // thumbnail not loaded yet
                        icon.paintIcon(mv, tempG,
                                p.x - icon.getIconWidth() / 2,
                                p.y - icon.getIconHeight() / 2);
                    }
                }
                updateOffscreenBuffer = false;
            }
            g.drawImage(offscreenBuffer, 0, 0, null);
        }
        else {
            for (ImageEntry e : data) {
                if (e.getPos() == null) {
                    continue;
                }
                Point p = mv.getPoint(e.getPos());
                icon.paintIcon(mv, g,
                        p.x - icon.getIconWidth() / 2,
                        p.y - icon.getIconHeight() / 2);
            }
        }

        if (currentPhoto >= 0 && currentPhoto < data.size()) {
            ImageEntry e = data.get(currentPhoto);

            if (e.getPos() != null) {
                Point p = mv.getPoint(e.getPos());

                if (e.thumbnail != null) {
                    Dimension d = scaledDimension(e.thumbnail);
                    g.setColor(new Color(128, 0, 0, 122));
                    g.fillRect(p.x - d.width / 2, p.y - d.height / 2, d.width, d.height);
                } else {
                    if (e.getExifImgDir() != null) {
                        double arrowlength = 25;
                        double arrowwidth = 18;

                        double dir = e.getExifImgDir();
                        // Rotate 90 degrees CCW
                        double headdir = ( dir < 90 ) ? dir + 270 : dir - 90;
                        double leftdir = ( headdir < 90 ) ? headdir + 270 : headdir - 90;
                        double rightdir = ( headdir > 270 ) ? headdir - 270 : headdir + 90;

                        double ptx = p.x + Math.cos(Math.toRadians(headdir)) * arrowlength;
                        double pty = p.y + Math.sin(Math.toRadians(headdir)) * arrowlength;

                        double ltx = p.x + Math.cos(Math.toRadians(leftdir)) * arrowwidth/2;
                        double lty = p.y + Math.sin(Math.toRadians(leftdir)) * arrowwidth/2;

                        double rtx = p.x + Math.cos(Math.toRadians(rightdir)) * arrowwidth/2;
                        double rty = p.y + Math.sin(Math.toRadians(rightdir)) * arrowwidth/2;

                        g.setColor(Color.white);
                        int[] xar = {(int) ltx, (int) ptx, (int) rtx, (int) ltx};
                        int[] yar = {(int) lty, (int) pty, (int) rty, (int) lty};
                        g.fillPolygon(xar, yar, 4);
                    }

                    selectedIcon.paintIcon(mv, g,
                            p.x - selectedIcon.getIconWidth() / 2,
                            p.y - selectedIcon.getIconHeight() / 2);

                }
            }
        }
    }

    @Override
    public void visitBoundingBox(BoundingXYVisitor v) {
        for (ImageEntry e : data) {
            v.visit(e.getPos());
        }
    }

    /*
     * Extract gps from image exif
     *
     * If successful, fills in the LatLon and EastNorth attributes of passed in
     * image;
     */

    private static void extractExif(ImageEntry e) {

        double deg;
        double min, sec;
        double lon, lat;
        Metadata metadata = null;
        Directory dirExif = null, dirGps = null;

        try {
            metadata = JpegMetadataReader.readMetadata(e.getFile());
            dirExif = metadata.getDirectory(ExifDirectory.class);
            dirGps = metadata.getDirectory(GpsDirectory.class);
        } catch (CompoundException p) {
            e.setExifCoor(null);
            e.setPos(null);
            return;
        }

        try {
            int orientation = dirExif.getInt(ExifDirectory.TAG_ORIENTATION);
            e.setExifOrientation(orientation);
        } catch (MetadataException ex) {
        }

        try {
            double ele=dirGps.getDouble(GpsDirectory.TAG_GPS_ALTITUDE);
            int d = dirGps.getInt(GpsDirectory.TAG_GPS_ALTITUDE_REF);
            if (d == 1) {
                ele *= -1;
            }
            e.setElevation(ele);
        } catch (MetadataException ex) {
        }

        try {
            // longitude

            Rational[] components = dirGps.getRationalArray(GpsDirectory.TAG_GPS_LONGITUDE);

            deg = components[0].doubleValue();
            min = components[1].doubleValue();
            sec = components[2].doubleValue();

            if (Double.isNaN(deg) && Double.isNaN(min) && Double.isNaN(sec))
                throw new IllegalArgumentException();

            lon = (Double.isNaN(deg) ? 0 : deg + (Double.isNaN(min) ? 0 : (min / 60)) + (Double.isNaN(sec) ? 0 : (sec / 3600)));

            if (dirGps.getString(GpsDirectory.TAG_GPS_LONGITUDE_REF).charAt(0) == 'W') {
                lon = -lon;
            }

            // latitude

            components = dirGps.getRationalArray(GpsDirectory.TAG_GPS_LATITUDE);

            deg = components[0].doubleValue();
            min = components[1].doubleValue();
            sec = components[2].doubleValue();

            if (Double.isNaN(deg) && Double.isNaN(min) && Double.isNaN(sec))
                throw new IllegalArgumentException();

            lat = (Double.isNaN(deg) ? 0 : deg + (Double.isNaN(min) ? 0 : (min / 60)) + (Double.isNaN(sec) ? 0 : (sec / 3600)));

            if (Double.isNaN(lat))
                throw new IllegalArgumentException();

            if (dirGps.getString(GpsDirectory.TAG_GPS_LATITUDE_REF).charAt(0) == 'S') {
                lat = -lat;
            }

            // Store values

            e.setExifCoor(new LatLon(lat, lon));
            e.setPos(e.getExifCoor());

        } catch (CompoundException p) {
            // Try to read lon/lat as double value (Nonstandard, created by some cameras -> #5220)
            try {
                Double longitude = dirGps.getDouble(GpsDirectory.TAG_GPS_LONGITUDE);
                Double latitude = dirGps.getDouble(GpsDirectory.TAG_GPS_LATITUDE);
                if (longitude == null || latitude == null)
                    throw new CompoundException("");

                // Store values

                e.setExifCoor(new LatLon(latitude, longitude));
                e.setPos(e.getExifCoor());
            } catch (CompoundException ex) {
                e.setExifCoor(null);
                e.setPos(null);
            }
        } catch (Exception ex) { // (other exceptions, e.g. #5271)
            System.err.println("Error reading EXIF from file: "+ex);
            e.setExifCoor(null);
            e.setPos(null);
        }

        // compass direction value

        Rational direction = null;

        try {
            direction = dirGps.getRational(GpsDirectory.TAG_GPS_IMG_DIRECTION);
            if (direction != null) {
                e.setExifImgDir(direction.doubleValue());
            }
        } catch (Exception ex) { // (CompoundException and other exceptions, e.g. #5271)
            // Do nothing
        }
    }

    public void showNextPhoto() {
        if (data != null && data.size() > 0) {
            currentPhoto++;
            if (currentPhoto >= data.size()) {
                currentPhoto = data.size() - 1;
            }
            ImageViewerDialog.showImage(this, data.get(currentPhoto));
        } else {
            currentPhoto = -1;
        }
        Main.map.repaint();
    }

    public void showPreviousPhoto() {
        if (data != null && data.size() > 0) {
            currentPhoto--;
            if (currentPhoto < 0) {
                currentPhoto = 0;
            }
            ImageViewerDialog.showImage(this, data.get(currentPhoto));
        } else {
            currentPhoto = -1;
        }
        Main.map.repaint();
    }

    public void checkPreviousNextButtons() {
        ImageViewerDialog.setNextEnabled(currentPhoto < data.size() - 1);
        ImageViewerDialog.setPreviousEnabled(currentPhoto > 0);
    }

    public void removeCurrentPhoto() {
        if (data != null && data.size() > 0 && currentPhoto >= 0 && currentPhoto < data.size()) {
            data.remove(currentPhoto);
            if (currentPhoto >= data.size()) {
                currentPhoto = data.size() - 1;
            }
            if (currentPhoto >= 0) {
                ImageViewerDialog.showImage(this, data.get(currentPhoto));
            } else {
                ImageViewerDialog.showImage(this, null);
            }
            updateOffscreenBuffer = true;
            Main.map.repaint();
        }
    }

    public void removeCurrentPhotoFromDisk() {
        ImageEntry toDelete = null;
        if (data != null && data.size() > 0 && currentPhoto >= 0 && currentPhoto < data.size()) {
            toDelete = data.get(currentPhoto);

            int result = new ExtendedDialog(
                    Main.parent,
                    tr("Delete image file from disk"),
                    new String[] {tr("Cancel"), tr("Delete")})
            .setButtonIcons(new String[] {"cancel.png", "dialogs/delete.png"})
            .setContent(new JLabel(tr("<html><h3>Delete the file {0} from disk?<p>The image file will be permanently lost!</h3></html>"
                    ,toDelete.getFile().getName()), ImageProvider.get("dialogs/geoimage/deletefromdisk"),SwingConstants.LEFT))
                    .toggleEnable("geoimage.deleteimagefromdisk")
                    .setCancelButton(1)
                    .setDefaultButton(2)
                    .showDialog()
                    .getValue();

            if(result == 2)
            {
                data.remove(currentPhoto);
                if (currentPhoto >= data.size()) {
                    currentPhoto = data.size() - 1;
                }
                if (currentPhoto >= 0) {
                    ImageViewerDialog.showImage(this, data.get(currentPhoto));
                } else {
                    ImageViewerDialog.showImage(this, null);
                }

                if (toDelete.getFile().delete()) {
                    System.out.println("File "+toDelete.getFile().toString()+" deleted. ");
                } else {
                    JOptionPane.showMessageDialog(
                            Main.parent,
                            tr("Image file could not be deleted."),
                            tr("Error"),
                            JOptionPane.ERROR_MESSAGE
                            );
                }

                updateOffscreenBuffer = true;
                Main.map.repaint();
            }
        }
    }

    private MouseAdapter mouseAdapter = null;
    private MapModeChangeListener mapModeListener = null;

    @Override
    public void hookUpMapView() {
        mouseAdapter = new MouseAdapter() {
            private final boolean isMapModeOk() {
                return Main.map.mapMode == null || Main.map.mapMode instanceof SelectAction;
            }
            @Override public void mousePressed(MouseEvent e) {

                if (e.getButton() != MouseEvent.BUTTON1)
                    return;
                if (isVisible() && isMapModeOk()) {
                    Main.map.mapView.repaint();
                }
            }

            @Override public void mouseReleased(MouseEvent ev) {
                if (ev.getButton() != MouseEvent.BUTTON1)
                    return;
                if (data == null || !isVisible() || !isMapModeOk())
                    return;

                for (int i = data.size() - 1; i >= 0; --i) {
                    ImageEntry e = data.get(i);
                    if (e.getPos() == null) {
                        continue;
                    }
                    Point p = Main.map.mapView.getPoint(e.getPos());
                    Rectangle r;
                    if (e.thumbnail != null) {
                        Dimension d = scaledDimension(e.thumbnail);
                        r = new Rectangle(p.x - d.width / 2, p.y - d.height / 2, d.width, d.height);
                    } else {
                        r = new Rectangle(p.x - icon.getIconWidth() / 2,
                                p.y - icon.getIconHeight() / 2,
                                icon.getIconWidth(),
                                icon.getIconHeight());
                    }
                    if (r.contains(ev.getPoint())) {
                        currentPhoto = i;
                        ImageViewerDialog.showImage(GeoImageLayer.this, e);
                        Main.map.repaint();
                        break;
                    }
                }
            }
        };

        mapModeListener = new MapModeChangeListener() {
            public void mapModeChange(MapMode oldMapMode, MapMode newMapMode) {
                if (newMapMode == null || (newMapMode instanceof org.openstreetmap.josm.actions.mapmode.SelectAction)) {
                    Main.map.mapView.addMouseListener(mouseAdapter);
                } else {
                    Main.map.mapView.removeMouseListener(mouseAdapter);
                }
            }
        };

        MapFrame.addMapModeChangeListener(mapModeListener);
        mapModeListener.mapModeChange(null, Main.map.mapMode);

        MapView.addLayerChangeListener(new LayerChangeListener() {
            public void activeLayerChange(Layer oldLayer, Layer newLayer) {
                if (newLayer == GeoImageLayer.this) {
                    // only in select mode it is possible to click the images
                    Main.map.selectSelectTool(false);
                }
            }

            public void layerAdded(Layer newLayer) {
            }

            public void layerRemoved(Layer oldLayer) {
                if (oldLayer == GeoImageLayer.this) {
                    if (thumbsloader != null) {
                        thumbsloader.stop = true;
                    }
                    Main.map.mapView.removeMouseListener(mouseAdapter);
                    MapFrame.removeMapModeChangeListener(mapModeListener);
                    currentPhoto = -1;
                    data.clear();
                    data = null;
                    // stop listening to layer change events
                    MapView.removeLayerChangeListener(this);
                }
            }
        });

        Main.map.mapView.addPropertyChangeListener(this);
        if (Main.map.getToggleDialog(ImageViewerDialog.class) == null) {
            ImageViewerDialog.newInstance();
            Main.map.addToggleDialog(ImageViewerDialog.getInstance());
        }
    }

    public void propertyChange(PropertyChangeEvent evt) {
        if (NavigatableComponent.PROPNAME_CENTER.equals(evt.getPropertyName()) || NavigatableComponent.PROPNAME_SCALE.equals(evt.getPropertyName())) {
            updateOffscreenBuffer = true;
        }
    }

    public void loadThumbs() {
        if (useThumbs && !thumbsLoaded) {
            thumbsLoaded = true;
            thumbsloader = new ThumbsLoader(this);
            Thread t = new Thread(thumbsloader);
            t.setPriority(Thread.MIN_PRIORITY);
            t.start();
        }
    }

    public void updateBufferAndRepaint() {
        updateOffscreenBuffer = true;
        Main.map.mapView.repaint();
    }

    public List<ImageEntry> getImages() {
        List<ImageEntry> copy = new ArrayList<ImageEntry>();
        for (ImageEntry ie : data) {
            copy.add(ie.clone());
        }
        return copy;
    }

    public GpxLayer getGpxLayer() {
        return gpxLayer;
    }

    public void jumpToNextMarker() {
        showNextPhoto();
    }

    public void jumpToPreviousMarker() {
        showPreviousPhoto();
    }
}
