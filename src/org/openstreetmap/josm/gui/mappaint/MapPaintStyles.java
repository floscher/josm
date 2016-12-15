// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.mappaint;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.swing.ImageIcon;
import javax.swing.SwingUtilities;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.Tag;
import org.openstreetmap.josm.gui.PleaseWaitRunnable;
import org.openstreetmap.josm.gui.mappaint.mapcss.MapCSSStyleSource;
import org.openstreetmap.josm.gui.mappaint.styleelement.MapImage;
import org.openstreetmap.josm.gui.mappaint.styleelement.NodeElement;
import org.openstreetmap.josm.gui.mappaint.styleelement.StyleElement;
import org.openstreetmap.josm.gui.preferences.SourceEntry;
import org.openstreetmap.josm.gui.preferences.map.MapPaintPreference.MapPaintPrefHelper;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.io.CachedFile;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.Utils;

/**
 * This class manages the list of available map paint styles and gives access to
 * the ElemStyles singleton.
 *
 * On change, {@link MapPaintSylesUpdateListener#mapPaintStylesUpdated()} is fired
 * for all listeners.
 */
public final class MapPaintStyles {

    private static final Collection<String> DEPRECATED_IMAGE_NAMES = Arrays.asList(
            "presets/misc/deprecated.svg",
            "misc/deprecated.png");

    private static ElemStyles styles = new ElemStyles();

    /**
     * Returns the {@link ElemStyles} singleton instance.
     *
     * The returned object is read only, any manipulation happens via one of
     * the other wrapper methods in this class. ({@link #readFromPreferences},
     * {@link #moveStyles}, ...)
     * @return the {@code ElemStyles} singleton instance
     */
    public static ElemStyles getStyles() {
        return styles;
    }

    private MapPaintStyles() {
        // Hide default constructor for utils classes
    }

    /**
     * Value holder for a reference to a tag name. A style instruction
     * <pre>
     *    text: a_tag_name;
     * </pre>
     * results in a tag reference for the tag <tt>a_tag_name</tt> in the
     * style cascade.
     */
    public static class TagKeyReference {
        public final String key;

        public TagKeyReference(String key) {
            this.key = key;
        }

        @Override
        public String toString() {
            return "TagKeyReference{" + "key='" + key + "'}";
        }
    }

    /**
     * IconReference is used to remember the associated style source for each icon URL.
     * This is necessary because image URLs can be paths relative
     * to the source file and we have cascading of properties from different source files.
     */
    public static class IconReference {

        public final String iconName;
        public final StyleSource source;

        public IconReference(String iconName, StyleSource source) {
            this.iconName = iconName;
            this.source = source;
        }

        @Override
        public String toString() {
            return "IconReference{" + "iconName='" + iconName + "' source='" + source.getDisplayString() + "'}";
        }

        /**
         * Determines whether this icon represents a deprecated icon
         * @return whether this icon represents a deprecated icon
         * @since 10927
         */
        public boolean isDeprecatedIcon() {
            return DEPRECATED_IMAGE_NAMES.contains(iconName);
        }
    }

    /**
     * Image provider for icon. Note that this is a provider only. A @link{ImageProvider#get()} call may still fail!
     *
     * @param ref reference to the requested icon
     * @param test if <code>true</code> than the icon is request is tested
     * @return image provider for icon (can be <code>null</code> when <code>test</code> is <code>true</code>).
     * @see #getIcon(IconReference, int,int)
     * @since 8097
     */
    public static ImageProvider getIconProvider(IconReference ref, boolean test) {
        final String namespace = ref.source.getPrefName();
        ImageProvider i = new ImageProvider(ref.iconName)
                .setDirs(getIconSourceDirs(ref.source))
                .setId("mappaint."+namespace)
                .setArchive(ref.source.zipIcons)
                .setInArchiveDir(ref.source.getZipEntryDirName())
                .setOptional(true);
        if (test && i.get() == null) {
            String msg = "Mappaint style \""+namespace+"\" ("+ref.source.getDisplayString()+") icon \"" + ref.iconName + "\" not found.";
            ref.source.logWarning(msg);
            Main.warn(msg);
            return null;
        }
        return i;
    }

    /**
     * Return scaled icon.
     *
     * @param ref reference to the requested icon
     * @param width icon width or -1 for autoscale
     * @param height icon height or -1 for autoscale
     * @return image icon or <code>null</code>.
     * @see #getIconProvider(IconReference, boolean)
     */
    public static ImageIcon getIcon(IconReference ref, int width, int height) {
        final String namespace = ref.source.getPrefName();
        ImageIcon i = getIconProvider(ref, false).setSize(width, height).get();
        if (i == null) {
            Main.warn("Mappaint style \""+namespace+"\" ("+ref.source.getDisplayString()+") icon \"" + ref.iconName + "\" not found.");
            return null;
        }
        return i;
    }

    /**
     * No icon with the given name was found, show a dummy icon instead
     * @param source style source
     * @return the icon misc/no_icon.png, in descending priority:
     *   - relative to source file
     *   - from user icon paths
     *   - josm's default icon
     *  can be null if the defaults are turned off by user
     */
    public static ImageIcon getNoIconIcon(StyleSource source) {
        return new ImageProvider("presets/misc/no_icon")
                .setDirs(getIconSourceDirs(source))
                .setId("mappaint."+source.getPrefName())
                .setArchive(source.zipIcons)
                .setInArchiveDir(source.getZipEntryDirName())
                .setOptional(true).get();
    }

    public static ImageIcon getNodeIcon(Tag tag) {
        return getNodeIcon(tag, true);
    }

    /**
     * Returns the node icon that would be displayed for the given tag.
     * @param tag The tag to look an icon for
     * @param includeDeprecatedIcon if {@code true}, the special deprecated icon will be returned if applicable
     * @return {@code null} if no icon found, or if the icon is deprecated and not wanted
     */
    public static ImageIcon getNodeIcon(Tag tag, boolean includeDeprecatedIcon) {
        if (tag != null) {
            DataSet ds = new DataSet();
            Node virtualNode = new Node(LatLon.ZERO);
            virtualNode.put(tag.getKey(), tag.getValue());
            StyleElementList styleList;
            MapCSSStyleSource.STYLE_SOURCE_LOCK.readLock().lock();
            try {
                // Add primitive to dataset to avoid DataIntegrityProblemException when evaluating selectors
                ds.addPrimitive(virtualNode);
                styleList = getStyles().generateStyles(virtualNode, 0.5, false).a;
                ds.removePrimitive(virtualNode);
            } finally {
                MapCSSStyleSource.STYLE_SOURCE_LOCK.readLock().unlock();
            }
            if (styleList != null) {
                for (StyleElement style : styleList) {
                    if (style instanceof NodeElement) {
                        MapImage mapImage = ((NodeElement) style).mapImage;
                        if (mapImage != null) {
                            if (includeDeprecatedIcon || mapImage.name == null || !DEPRECATED_IMAGE_NAMES.contains(mapImage.name)) {
                                return new ImageIcon(mapImage.getImage(false));
                            } else {
                                return null; // Deprecated icon found but not wanted
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    public static List<String> getIconSourceDirs(StyleSource source) {
        List<String> dirs = new LinkedList<>();

        File sourceDir = source.getLocalSourceDir();
        if (sourceDir != null) {
            dirs.add(sourceDir.getPath());
        }

        Collection<String> prefIconDirs = Main.pref.getCollection("mappaint.icon.sources");
        for (String fileset : prefIconDirs) {
            String[] a;
            if (fileset.indexOf('=') >= 0) {
                a = fileset.split("=", 2);
            } else {
                a = new String[] {"", fileset};
            }

            /* non-prefixed path is generic path, always take it */
            if (a[0].isEmpty() || source.getPrefName().equals(a[0])) {
                dirs.add(a[1]);
            }
        }

        if (Main.pref.getBoolean("mappaint.icon.enable-defaults", true)) {
            /* don't prefix icon path, as it should be generic */
            dirs.add("resource://images/");
        }

        return dirs;
    }

    public static void readFromPreferences() {
        styles.clear();

        Collection<? extends SourceEntry> sourceEntries = MapPaintPrefHelper.INSTANCE.get();

        for (SourceEntry entry : sourceEntries) {
            styles.add(fromSourceEntry(entry));
        }
        for (StyleSource source : styles.getStyleSources()) {
            loadStyleForFirstTime(source);
        }
        fireMapPaintSylesUpdated();
    }

    private static void loadStyleForFirstTime(StyleSource source) {
        final long startTime = System.currentTimeMillis();
        source.loadStyleSource();
        if (Main.pref.getBoolean("mappaint.auto_reload_local_styles", true) && source.isLocal()) {
            try {
                Main.fileWatcher.registerStyleSource(source);
            } catch (IOException e) {
                Main.error(e);
            }
        }
        if (Main.isDebugEnabled() || !source.isValid()) {
            final long elapsedTime = System.currentTimeMillis() - startTime;
            String message = "Initializing map style " + source.url + " completed in " + Utils.getDurationString(elapsedTime);
            if (!source.isValid()) {
                Main.warn(message + " (" + source.getErrors().size() + " errors, " + source.getWarnings().size() + " warnings)");
            } else {
                Main.debug(message);
            }
        }
    }

    private static StyleSource fromSourceEntry(SourceEntry entry) {
        Set<String> mimes = new HashSet<>(Arrays.asList(MapCSSStyleSource.MAPCSS_STYLE_MIME_TYPES.split(", ")));
        try (CachedFile cf = new CachedFile(entry.url).setHttpAccept(Utils.join(", ", mimes))) {
            String zipEntryPath = cf.findZipEntryPath("mapcss", "style");
            if (zipEntryPath != null) {
                entry.isZip = true;
                entry.zipEntryPath = zipEntryPath;
            }
            return new MapCSSStyleSource(entry);
        }
    }

    /**
     * reload styles
     * preferences are the same, but the file source may have changed
     * @param sel the indices of styles to reload
     */
    public static void reloadStyles(final int... sel) {
        List<StyleSource> toReload = new ArrayList<>();
        List<StyleSource> data = styles.getStyleSources();
        for (int i : sel) {
            toReload.add(data.get(i));
        }
        Main.worker.submit(new MapPaintStyleLoader(toReload));
    }

    public static class MapPaintStyleLoader extends PleaseWaitRunnable {
        private boolean canceled;
        private final Collection<StyleSource> sources;

        public MapPaintStyleLoader(Collection<StyleSource> sources) {
            super(tr("Reloading style sources"));
            this.sources = sources;
        }

        @Override
        protected void cancel() {
            canceled = true;
        }

        @Override
        protected void finish() {
            SwingUtilities.invokeLater(() -> {
                fireMapPaintSylesUpdated();
                styles.clearCached();
                if (Main.isDisplayingMapView()) {
                    Main.map.mapView.preferenceChanged(null);
                    Main.map.mapView.repaint();
                }
            });
        }

        @Override
        protected void realRun() {
            ProgressMonitor monitor = getProgressMonitor();
            monitor.setTicksCount(sources.size());
            for (StyleSource s : sources) {
                if (canceled)
                    return;
                monitor.subTask(tr("loading style ''{0}''...", s.getDisplayString()));
                s.loadStyleSource();
                monitor.worked(1);
            }
        }
    }

    /**
     * Move position of entries in the current list of StyleSources
     * @param sel The indices of styles to be moved.
     * @param delta The number of lines it should move. positive int moves
     *      down and negative moves up.
     */
    public static void moveStyles(int[] sel, int delta) {
        if (!canMoveStyles(sel, delta))
            return;
        int[] selSorted = Utils.copyArray(sel);
        Arrays.sort(selSorted);
        List<StyleSource> data = new ArrayList<>(styles.getStyleSources());
        for (int row: selSorted) {
            StyleSource t1 = data.get(row);
            StyleSource t2 = data.get(row + delta);
            data.set(row, t2);
            data.set(row + delta, t1);
        }
        styles.setStyleSources(data);
        MapPaintPrefHelper.INSTANCE.put(data);
        fireMapPaintSylesUpdated();
        styles.clearCached();
        Main.map.mapView.repaint();
    }

    public static boolean canMoveStyles(int[] sel, int i) {
        if (sel.length == 0)
            return false;
        int[] selSorted = Utils.copyArray(sel);
        Arrays.sort(selSorted);

        if (i < 0) // Up
            return selSorted[0] >= -i;
        else if (i > 0) // Down
            return selSorted[selSorted.length-1] <= styles.getStyleSources().size() - 1 - i;
        else
            return true;
    }

    public static void toggleStyleActive(int... sel) {
        List<StyleSource> data = styles.getStyleSources();
        for (int p : sel) {
            StyleSource s = data.get(p);
            s.active = !s.active;
        }
        MapPaintPrefHelper.INSTANCE.put(data);
        if (sel.length == 1) {
            fireMapPaintStyleEntryUpdated(sel[0]);
        } else {
            fireMapPaintSylesUpdated();
        }
        styles.clearCached();
        Main.map.mapView.repaint();
    }

    /**
     * Add a new map paint style.
     * @param entry map paint style
     * @return loaded style source, or {@code null}
     */
    public static StyleSource addStyle(SourceEntry entry) {
        StyleSource source = fromSourceEntry(entry);
        styles.add(source);
        loadStyleForFirstTime(source);
        MapPaintPrefHelper.INSTANCE.put(styles.getStyleSources());
        fireMapPaintSylesUpdated();
        styles.clearCached();
        if (Main.isDisplayingMapView()) {
            Main.map.mapView.repaint();
        }
        return source;
    }

    /***********************************
     * MapPaintSylesUpdateListener &amp; related code
     *  (get informed when the list of MapPaint StyleSources changes)
     */

    public interface MapPaintSylesUpdateListener {
        void mapPaintStylesUpdated();

        void mapPaintStyleEntryUpdated(int idx);
    }

    private static final CopyOnWriteArrayList<MapPaintSylesUpdateListener> listeners
            = new CopyOnWriteArrayList<>();

    public static void addMapPaintSylesUpdateListener(MapPaintSylesUpdateListener listener) {
        if (listener != null) {
            listeners.addIfAbsent(listener);
        }
    }

    public static void removeMapPaintSylesUpdateListener(MapPaintSylesUpdateListener listener) {
        listeners.remove(listener);
    }

    public static void fireMapPaintSylesUpdated() {
        for (MapPaintSylesUpdateListener l : listeners) {
            l.mapPaintStylesUpdated();
        }
    }

    public static void fireMapPaintStyleEntryUpdated(int idx) {
        for (MapPaintSylesUpdateListener l : listeners) {
            l.mapPaintStyleEntryUpdated(idx);
        }
    }
}
