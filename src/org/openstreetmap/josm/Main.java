// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.lang.ref.WeakReference;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.Action;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.UIManager;

import org.openstreetmap.gui.jmapviewer.FeatureAdapter;
import org.openstreetmap.josm.actions.JosmAction;
import org.openstreetmap.josm.actions.OpenFileAction;
import org.openstreetmap.josm.actions.downloadtasks.DownloadGpsTask;
import org.openstreetmap.josm.actions.downloadtasks.DownloadOsmTask;
import org.openstreetmap.josm.actions.downloadtasks.DownloadTask;
import org.openstreetmap.josm.actions.downloadtasks.PostDownloadHandler;
import org.openstreetmap.josm.actions.mapmode.MapMode;
import org.openstreetmap.josm.actions.search.SearchAction;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.Preferences;
import org.openstreetmap.josm.data.UndoRedoHandler;
import org.openstreetmap.josm.data.coor.CoordinateFormat;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.PrimitiveDeepCopy;
import org.openstreetmap.josm.data.projection.Projection;
import org.openstreetmap.josm.data.projection.ProjectionChangeListener;
import org.openstreetmap.josm.data.validation.OsmValidator;
import org.openstreetmap.josm.gui.GettingStarted;
import org.openstreetmap.josm.gui.MainMenu;
import org.openstreetmap.josm.gui.MapFrame;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.gui.dialogs.LayerListDialog;
import org.openstreetmap.josm.gui.io.SaveLayersDialog;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.gui.layer.OsmDataLayer.CommandQueueListener;
import org.openstreetmap.josm.gui.preferences.ImageryPreference;
import org.openstreetmap.josm.gui.preferences.MapPaintPreference;
import org.openstreetmap.josm.gui.preferences.ProjectionPreference;
import org.openstreetmap.josm.gui.preferences.TaggingPresetPreference;
import org.openstreetmap.josm.gui.preferences.ToolbarPreferences;
import org.openstreetmap.josm.gui.progress.PleaseWaitProgressMonitor;
import org.openstreetmap.josm.gui.progress.ProgressMonitorExecutor;
import org.openstreetmap.josm.gui.util.RedirectInputMap;
import org.openstreetmap.josm.io.OsmApi;
import org.openstreetmap.josm.plugins.PluginHandler;
import org.openstreetmap.josm.tools.CheckParameterUtil;
import org.openstreetmap.josm.tools.I18n;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.OpenBrowser;
import org.openstreetmap.josm.tools.OsmUrlToBounds;
import org.openstreetmap.josm.tools.PlatformHook;
import org.openstreetmap.josm.tools.PlatformHookOsx;
import org.openstreetmap.josm.tools.PlatformHookUnixoid;
import org.openstreetmap.josm.tools.PlatformHookWindows;
import org.openstreetmap.josm.tools.Shortcut;
import org.openstreetmap.josm.tools.Utils;

abstract public class Main {

    /**
     * Replies true if JOSM currently displays a map view. False, if it doesn't, i.e. if
     * it only shows the MOTD panel.
     *
     * @return true if JOSM currently displays a map view
     */
    static public boolean isDisplayingMapView() {
        if (map == null) return false;
        if (map.mapView == null) return false;
        return true;
    }
    /**
     * Global parent component for all dialogs and message boxes
     */
    public static Component parent;
    /**
     * Global application.
     */
    public static Main main;
    /**
     * The worker thread slave. This is for executing all long and intensive
     * calculations. The executed runnables are guaranteed to be executed separately
     * and sequential.
     */
    public final static ExecutorService worker = new ProgressMonitorExecutor();
    /**
     * Global application preferences
     */
    public static Preferences pref;

    /**
     * The global paste buffer.
     */
    public static final PrimitiveDeepCopy pasteBuffer = new PrimitiveDeepCopy();
    public static Layer pasteSource;

    /**
     * The MapFrame. Use setMapFrame to set or clear it.
     */
    public static MapFrame map;
    /**
     * True, when in applet mode
     */
    public static boolean applet = false;

    /**
     * The toolbar preference control to register new actions.
     */
    public static ToolbarPreferences toolbar;

    public UndoRedoHandler undoRedo = new UndoRedoHandler();

    public static PleaseWaitProgressMonitor currentProgressMonitor;

    /**
     * The main menu bar at top of screen.
     */
    public final MainMenu menu;

    public final OsmValidator validator;
    /**
     * The MOTD Layer.
     */
    private GettingStarted gettingStarted = new GettingStarted();

    /**
     * Print a message if logging is on.
     */
    static public int log_level = 2;
    static public void warn(String msg) {
        if (log_level < 1)
            return;
        System.out.println(msg);
    }
    static public void info(String msg) {
        if (log_level < 2)
            return;
        System.out.println(msg);
    }
    static public void debug(String msg) {
        if (log_level < 3)
            return;
        System.out.println(msg);
    }

    /**
     * Platform specific code goes in here.
     * Plugins may replace it, however, some hooks will be called before any plugins have been loeaded.
     * So if you need to hook into those early ones, split your class and send the one with the early hooks
     * to the JOSM team for inclusion.
     */
    public static PlatformHook platform;

    /**
     * Wheather or not the java vm is openjdk
     * We use this to work around openjdk bugs
     */
    public static boolean isOpenjdk;

    /**
     * Set or clear (if passed <code>null</code>) the map.
     */
    public final void setMapFrame(final MapFrame map) {
        MapFrame old = Main.map;
        panel.setVisible(false);
        panel.removeAll();
        if (map != null) {
            map.fillPanel(panel);
        } else {
            old.destroy();
            panel.add(gettingStarted, BorderLayout.CENTER);
        }
        panel.setVisible(true);
        redoUndoListener.commandChanged(0,0);

        Main.map = map;

        PluginHandler.notifyMapFrameChanged(old, map);
        if (map == null && currentProgressMonitor != null) {
            currentProgressMonitor.showForegroundDialog();
        }
    }

    /**
     * Remove the specified layer from the map. If it is the last layer,
     * remove the map as well.
     */
    public final void removeLayer(final Layer layer) {
        if (map != null) {
            map.mapView.removeLayer(layer);
            if (map != null && map.mapView.getAllLayers().isEmpty()) {
                setMapFrame(null);
            }
        }
    }

    private static InitStatusListener initListener = null;

    public static interface InitStatusListener {

        void updateStatus(String event);
    }

    public static void setInitStatusListener(InitStatusListener listener) {
        initListener = listener;
    }

    public Main() {
        main = this;
        isOpenjdk = System.getProperty("java.vm.name").toUpperCase().indexOf("OPENJDK") != -1;

        if (initListener != null) {
            initListener.updateStatus(tr("Executing platform startup hook"));
        }
        platform.startupHook();

        // We try to establish an API connection early, so that any API
        // capabilities are already known to the editor instance. However
        // if it goes wrong that's not critical at this stage.
        if (initListener != null) {
            initListener.updateStatus(tr("Initializing OSM API"));
        }
        try {
            OsmApi.getOsmApi().initialize(null, true);
        } catch (Exception x) {
            // ignore any exception here.
        }

        if (initListener != null) {
            initListener.updateStatus(tr("Building main menu"));
        }
        contentPanePrivate.add(panel, BorderLayout.CENTER);
        panel.add(gettingStarted, BorderLayout.CENTER);
        menu = new MainMenu();

        undoRedo.listenerCommands.add(redoUndoListener);

        // creating toolbar
        contentPanePrivate.add(toolbar.control, BorderLayout.NORTH);

        registerActionShortcut(menu.help, Shortcut.registerShortcut("system:help", tr("Help"),
                KeyEvent.VK_F1, Shortcut.GROUP_DIRECT));

        if (initListener != null) {
            initListener.updateStatus(tr("Initializing presets"));
        }
        TaggingPresetPreference.initialize();

        if (initListener != null) {
            initListener.updateStatus(tr("Initializing map styles"));
        }
        MapPaintPreference.initialize();

        if (initListener != null) {
            initListener.updateStatus(tr("Loading imagery preferences"));
        }
        ImageryPreference.initialize();

        if (initListener != null) {
            initListener.updateStatus(tr("Initializing validator"));
        }
        validator = new OsmValidator();
        MapView.addLayerChangeListener(validator);

        // hooks for the jmapviewer component
        FeatureAdapter.registerBrowserAdapter(new FeatureAdapter.BrowserAdapter() {
            @Override
            public void openLink(String url) {
                OpenBrowser.displayUrl(url);
            }
        });
        FeatureAdapter.registerTranslationAdapter(I18n.getTranslationAdapter());

        if (initListener != null) {
            initListener.updateStatus(tr("Updating user interface"));
        }

        toolbar.refreshToolbarControl();

        toolbar.control.updateUI();
        contentPanePrivate.updateUI();

    }

    /**
     * Add a new layer to the map. If no map exists, create one.
     */
    public final void addLayer(final Layer layer) {
        if (map == null) {
            final MapFrame mapFrame = new MapFrame(contentPanePrivate);
            setMapFrame(mapFrame);
            mapFrame.selectMapMode((MapMode)mapFrame.getDefaultButtonAction(), layer);
            mapFrame.setVisible(true);
            mapFrame.initializeDialogsPane();
            // bootstrapping problem: make sure the layer list dialog is going to
            // listen to change events of the very first layer
            //
            layer.addPropertyChangeListener(LayerListDialog.getInstance().getModel());
        }
        map.mapView.addLayer(layer);
    }

    /**
     * Replies true if there is an edit layer
     *
     * @return true if there is an edit layer
     */
    public boolean hasEditLayer() {
        if (getEditLayer() == null) return false;
        return true;
    }

    /**
     * Replies the current edit layer
     *
     * @return the current edit layer. null, if no current edit layer exists
     */
    public OsmDataLayer getEditLayer() {
        if (map == null) return null;
        if (map.mapView == null) return null;
        return map.mapView.getEditLayer();
    }

    /**
     * Replies the current data set.
     *
     * @return the current data set. null, if no current data set exists
     */
    public DataSet getCurrentDataSet() {
        if (!hasEditLayer()) return null;
        return getEditLayer().data;
    }

    /**
     * Returns the currently active  layer
     *
     * @return the currently active layer. null, if currently no active layer exists
     */
    public Layer getActiveLayer() {
        if (map == null) return null;
        if (map.mapView == null) return null;
        return map.mapView.getActiveLayer();
    }

    protected static final JPanel contentPanePrivate = new JPanel(new BorderLayout());

    public static void redirectToMainContentPane(JComponent source) {
        RedirectInputMap.redirect(source, contentPanePrivate);
    }

    public static void registerActionShortcut(Action action, Shortcut shortcut) {
        registerActionShortcut(action, shortcut.getKeyStroke());
    }

    public static void registerActionShortcut(Action action, KeyStroke keyStroke) {
        if (keyStroke == null)
            return;

        InputMap inputMap = contentPanePrivate.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        Object existing = inputMap.get(keyStroke);
        if (existing != null && !existing.equals(action)) {
            System.out.println(String.format("Keystroke %s is already assigned to %s, will be overridden by %s", keyStroke, existing, action));
        }
        inputMap.put(keyStroke, action);

        contentPanePrivate.getActionMap().put(action, action);
    }

    public static void unregisterActionShortcut(Shortcut shortcut) {
        contentPanePrivate.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).remove(shortcut.getKeyStroke());
    }

    public static void unregisterActionShortcut(JosmAction action) {
        unregisterActionShortcut(action, action.getShortcut());
    }

    public static void unregisterActionShortcut(Action action, Shortcut shortcut) {
        contentPanePrivate.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).remove(shortcut.getKeyStroke());
        contentPanePrivate.getActionMap().remove(action);
    }


    ///////////////////////////////////////////////////////////////////////////
    //  Implementation part
    ///////////////////////////////////////////////////////////////////////////

    public static final JPanel panel = new JPanel(new BorderLayout());

    protected static Rectangle bounds;
    protected static int windowState = JFrame.NORMAL;

    private final CommandQueueListener redoUndoListener = new CommandQueueListener(){
        public void commandChanged(final int queueSize, final int redoSize) {
            menu.undo.setEnabled(queueSize > 0);
            menu.redo.setEnabled(redoSize > 0);
        }
    };

    /**
     * Should be called before the main constructor to setup some parameter stuff
     * @param args The parsed argument list.
     */
    public static void preConstructorInit(Map<String, Collection<String>> args) {
        ProjectionPreference.setProjection();

        try {
            String defaultlaf = platform.getDefaultStyle();
            String laf = Main.pref.get("laf", defaultlaf);
            try {
                UIManager.setLookAndFeel(laf);
            }
            catch (final java.lang.ClassNotFoundException e) {
                System.out.println("Look and Feel not found: " + laf);
                Main.pref.put("laf", defaultlaf);
            }
            catch (final javax.swing.UnsupportedLookAndFeelException e) {
                System.out.println("Look and Feel not supported: " + laf);
                Main.pref.put("laf", defaultlaf);
            }
            toolbar = new ToolbarPreferences();
            contentPanePrivate.updateUI();
            panel.updateUI();
        } catch (final Exception e) {
            e.printStackTrace();
        }
        UIManager.put("OptionPane.okIcon", ImageProvider.get("ok"));
        UIManager.put("OptionPane.yesIcon", UIManager.get("OptionPane.okIcon"));
        UIManager.put("OptionPane.cancelIcon", ImageProvider.get("cancel"));
        UIManager.put("OptionPane.noIcon", UIManager.get("OptionPane.cancelIcon"));

        I18n.translateJavaInternalMessages();

        // init default coordinate format
        //
        try {
            //CoordinateFormat format = CoordinateFormat.valueOf(Main.pref.get("coordinates"));
            CoordinateFormat.setCoordinateFormat(CoordinateFormat.valueOf(Main.pref.get("coordinates")));
        } catch (IllegalArgumentException iae) {
            CoordinateFormat.setCoordinateFormat(CoordinateFormat.DECIMAL_DEGREES);
        }

        Dimension screenDimension = Toolkit.getDefaultToolkit().getScreenSize();
        String geometry = null;
        if (args.containsKey("geometry")) {
            geometry = args.get("geometry").iterator().next();
        } else {
            geometry = Main.pref.get("gui.geometry");
        }
        if (geometry.length() != 0) {
            final Matcher m = Pattern.compile("(\\d+)x(\\d+)(([+-])(\\d+)([+-])(\\d+))?").matcher(geometry);
            if (m.matches()) {
                int w = Integer.valueOf(m.group(1));
                int h = Integer.valueOf(m.group(2));
                int x = 0, y = 0;
                if (m.group(3) != null) {
                    x = Integer.valueOf(m.group(5));
                    y = Integer.valueOf(m.group(7));
                    if (m.group(4).equals("-")) {
                        x = screenDimension.width - x - w;
                    }
                    if (m.group(6).equals("-")) {
                        y = screenDimension.height - y - h;
                    }
                }
                // copied from WindowsGeometry.applySafe()
                if (x > Toolkit.getDefaultToolkit().getScreenSize().width - 10) {
                    x = 0;
                }
                if (y > Toolkit.getDefaultToolkit().getScreenSize().height - 10) {
                    y = 0;
                }
                bounds = new Rectangle(x,y,w,h);
                if(!Main.pref.get("gui.geometry").equals(geometry)) {
                    // remember this geometry
                    Main.pref.put("gui.geometry", geometry);
                }
            } else {
                System.out.println("Ignoring malformed geometry: "+geometry);
            }
        }
        if (bounds == null) {
            bounds = !args.containsKey("no-maximize") ? new Rectangle(0,0,screenDimension.width,screenDimension.height) : new Rectangle(1000,740);
        }
    }

    public void postConstructorProcessCmdLine(Map<String, Collection<String>> args) {
        if (args.containsKey("download")) {
            List<File> fileList = new ArrayList<File>();
            for (String s : args.get("download")) {
                File f = null;
                switch(paramType(s)) {
                case httpUrl:
                    downloadFromParamHttp(false, s);
                    break;
                case bounds:
                    downloadFromParamBounds(false, s);
                    break;
                case fileUrl:
                    try {
                        f = new File(new URI(s));
                    } catch (URISyntaxException e) {
                        JOptionPane.showMessageDialog(
                                Main.parent,
                                tr("Ignoring malformed file URL: \"{0}\"", s),
                                tr("Warning"),
                                JOptionPane.WARNING_MESSAGE
                                );
                    }
                    if (f!=null) {
                        fileList.add(f);
                    }
                    break;
                case fileName:
                    f = new File(s);
                    fileList.add(f);
                    break;
                }
            }
            if(!fileList.isEmpty())
            {
                OpenFileAction.openFiles(fileList, true);
            }
        }
        if (args.containsKey("downloadgps")) {
            for (String s : args.get("downloadgps")) {
                switch(paramType(s)) {
                case httpUrl:
                    downloadFromParamHttp(true, s);
                    break;
                case bounds:
                    downloadFromParamBounds(true, s);
                    break;
                case fileUrl:
                case fileName:
                    JOptionPane.showMessageDialog(
                            Main.parent,
                            tr("Parameter \"downloadgps\" does not accept file names or file URLs"),
                            tr("Warning"),
                            JOptionPane.WARNING_MESSAGE
                            );
                }
            }
        }
        if (args.containsKey("selection")) {
            for (String s : args.get("selection")) {
                SearchAction.search(s, SearchAction.SearchMode.add);
            }
        }
    }

    public static boolean saveUnsavedModifications() {
        if (map == null) return true;
        SaveLayersDialog dialog = new SaveLayersDialog(Main.parent);
        List<OsmDataLayer> layersWithUnmodifiedChanges = new ArrayList<OsmDataLayer>();
        for (OsmDataLayer l: Main.map.mapView.getLayersOfType(OsmDataLayer.class)) {
            if (l.requiresSaveToFile() || l.requiresUploadToServer()) {
                layersWithUnmodifiedChanges.add(l);
            }
        }
        dialog.prepareForSavingAndUpdatingLayersBeforeExit();
        if (!layersWithUnmodifiedChanges.isEmpty()) {
            dialog.getModel().populate(layersWithUnmodifiedChanges);
            dialog.setVisible(true);
            switch(dialog.getUserAction()) {
            case CANCEL: return false;
            case PROCEED: return true;
            default: return false;
            }
        }

        return true;
    }

    public static boolean exitJosm(boolean exit) {
        if (Main.saveUnsavedModifications()) {
            Main.saveGuiGeometry();
            // Remove all layers because somebody may rely on layerRemoved events (like AutosaveTask)
            if (Main.isDisplayingMapView()) {
                Collection<Layer> layers = new ArrayList<Layer>(Main.map.mapView.getAllLayers());
                for (Layer l: layers) {
                    Main.map.mapView.removeLayer(l);
                }
            }
            if (exit) {
                System.exit(0);
                return true;
            } else
                return true;
        } else
            return false;
    }

    /**
     * The type of a command line parameter, to be used in switch statements.
     * @see paramType
     */
    private enum DownloadParamType { httpUrl, fileUrl, bounds, fileName }

    /**
     * Guess the type of a parameter string specified on the command line with --download= or --downloadgps.
     * @param s A parameter string
     * @return The guessed parameter type
     */
    private DownloadParamType paramType(String s) {
        if(s.startsWith("http:")) return DownloadParamType.httpUrl;
        if(s.startsWith("file:")) return DownloadParamType.fileUrl;
        String coorPattern = "\\s*[0-9]+(\\.[0-9]+)?\\s*";
        if(s.matches(coorPattern+"(,"+coorPattern+"){3}")) return DownloadParamType.bounds;
        // everything else must be a file name
        return DownloadParamType.fileName;
    }

    /**
     * Download area specified on the command line as OSM URL.
     * @param rawGps Flag to download raw GPS tracks
     * @param s The URL parameter
     */
    private static void downloadFromParamHttp(final boolean rawGps, String s) {
        final Bounds b = OsmUrlToBounds.parse(s);
        if (b == null) {
            JOptionPane.showMessageDialog(
                    Main.parent,
                    tr("Ignoring malformed URL: \"{0}\"", s),
                    tr("Warning"),
                    JOptionPane.WARNING_MESSAGE
                    );
        } else {
            downloadFromParamBounds(rawGps, b);
        }
    }

    /**
     * Download area specified on the command line as bounds string.
     * @param rawGps Flag to download raw GPS tracks
     * @param s The bounds parameter
     */
    private static void downloadFromParamBounds(final boolean rawGps, String s) {
        final StringTokenizer st = new StringTokenizer(s, ",");
        if (st.countTokens() == 4) {
            Bounds b = new Bounds(
                    new LatLon(Double.parseDouble(st.nextToken()),Double.parseDouble(st.nextToken())),
                    new LatLon(Double.parseDouble(st.nextToken()),Double.parseDouble(st.nextToken()))
                    );
            downloadFromParamBounds(rawGps, b);
        }
    }

    /**
     * Download area specified as Bounds value.
     * @param rawGps Flag to download raw GPS tracks
     * @param b The bounds value
     * @see downloadFromParamBounds(final boolean rawGps, String s)
     * @see downloadFromParamHttp
     */
    private static void downloadFromParamBounds(final boolean rawGps, Bounds b) {
        DownloadTask task = rawGps ? new DownloadGpsTask() : new DownloadOsmTask();
        // asynchronously launch the download task ...
        Future<?> future = task.download(true, b, null);
        // ... and the continuation when the download is finished (this will wait for the download to finish)
        Main.worker.execute(new PostDownloadHandler(task, future));
    }

    public static void determinePlatformHook() {
        String os = System.getProperty("os.name");
        if (os == null) {
            System.err.println("Your operating system has no name, so I'm guessing its some kind of *nix.");
            platform = new PlatformHookUnixoid();
        } else if (os.toLowerCase().startsWith("windows")) {
            platform = new PlatformHookWindows();
        } else if (os.equals("Linux") || os.equals("Solaris") ||
                os.equals("SunOS") || os.equals("AIX") ||
                os.equals("FreeBSD") || os.equals("NetBSD") || os.equals("OpenBSD")) {
            platform = new PlatformHookUnixoid();
        } else if (os.toLowerCase().startsWith("mac os x")) {
            platform = new PlatformHookOsx();
        } else {
            System.err.println("I don't know your operating system '"+os+"', so I'm guessing its some kind of *nix.");
            platform = new PlatformHookUnixoid();
        }
    }

    static public void saveGuiGeometry() {
        // save the current window geometry and the width of the toggle dialog area
        String newGeometry = "";
        String newToggleDlgWidth = null;
        try {
            Dimension screenDimension = Toolkit.getDefaultToolkit().getScreenSize();
            int width = (int)bounds.getWidth();
            int height = (int)bounds.getHeight();
            int x = (int)bounds.getX();
            int y = (int)bounds.getY();
            if (width > screenDimension.width) {
                width = screenDimension.width;
            }
            if (height > screenDimension.height) {
                width = screenDimension.height;
            }
            if (x < 0) {
                x = 0;
            }
            if (y < 0) {
                y = 0;
            }
            newGeometry = width + "x" + height + "+" + x + "+" + y;

            if (map  != null) {
                newToggleDlgWidth = Integer.toString(map.getToggleDlgWidth());
                if (newToggleDlgWidth.equals(Integer.toString(MapFrame.DEF_TOGGLE_DLG_WIDTH))) {
                    newToggleDlgWidth = "";
                }
            }
        }
        catch (Exception e) {
            System.out.println("Failed to get GUI geometry: " + e);
            e.printStackTrace();
        }
        boolean maximized = (windowState & JFrame.MAXIMIZED_BOTH) != 0;
        // Main.debug("Main window: saving geometry \"" + newGeometry + "\" " + (maximized?"maximized":"normal"));
        pref.put("gui.maximized", maximized);
        pref.put("gui.geometry", newGeometry);
        if (newToggleDlgWidth != null) {
            pref.put("toggleDialogs.width", newToggleDlgWidth);
        }
    }
    private static class WindowPositionSizeListener extends WindowAdapter implements
    ComponentListener {

        @Override
        public void windowStateChanged(WindowEvent e) {
            Main.windowState = e.getNewState();
            // Main.debug("Main window state changed to " + Main.windowState);
        }

        public void componentHidden(ComponentEvent e) {
        }

        public void componentMoved(ComponentEvent e) {
            handleComponentEvent(e);
        }

        public void componentResized(ComponentEvent e) {
            handleComponentEvent(e);
        }

        public void componentShown(ComponentEvent e) {
        }

        private void handleComponentEvent(ComponentEvent e) {
            Component c = e.getComponent();
            if (c instanceof JFrame) {
                if (Main.windowState == JFrame.NORMAL) {
                    Main.bounds = ((JFrame) c).getBounds();
                    // Main.debug("Main window: new geometry " + Main.bounds);
                } else {
                    // Main.debug("Main window state is " + Main.windowState);
                }
            }
        }

    }
    public static void addListener() {
        parent.addComponentListener(new WindowPositionSizeListener());
        ((JFrame)parent).addWindowStateListener(new WindowPositionSizeListener());
    }

    public static void checkJava6() {
        String version = System.getProperty("java.version");
        if (version != null) {
            if (version.startsWith("1.6") || version.startsWith("6") ||
                    version.startsWith("1.7") || version.startsWith("7"))
                return;
            if (version.startsWith("1.5") || version.startsWith("5")) {
                JLabel ho = new JLabel("<html>"+
                        tr("<h2>JOSM requires Java version 6.</h2>"+
                                "Detected Java version: {0}.<br>"+
                                "You can <ul><li>update your Java (JRE) or</li>"+
                                "<li>use an earlier (Java 5 compatible) version of JOSM.</li></ul>"+
                                "More Info:", version)+"</html>");
                JTextArea link = new JTextArea("http://josm.openstreetmap.de/wiki/Help/SystemRequirements");
                link.setEditable(false);
                link.setBackground(panel.getBackground());
                JPanel panel = new JPanel(new GridBagLayout());
                GridBagConstraints gbc = new GridBagConstraints();
                gbc.gridwidth = GridBagConstraints.REMAINDER;
                gbc.anchor = GridBagConstraints.WEST;
                gbc.weightx = 1.0;
                panel.add(ho, gbc);
                panel.add(link, gbc);
                final String EXIT = tr("Exit JOSM");
                final String CONTINUE = tr("Continue, try anyway");
                int ret = JOptionPane.showOptionDialog(null, panel, tr("Error"), JOptionPane.YES_NO_OPTION, JOptionPane.ERROR_MESSAGE, null, new String[] {EXIT, CONTINUE}, EXIT);
                if (ret == 0) {
                    System.exit(0);
                }
                return;
            }
        }
        System.err.println("Error: Could not recognize Java Version: "+version);
    }

    /* ----------------------------------------------------------------------------------------- */
    /* projection handling  - Main is a registry for a single, global projection instance        */
    /*                                                                                           */
    /* TODO: For historical reasons the registry is implemented by Main. An alternative approach */
    /* would be a singleton org.openstreetmap.josm.data.projection.ProjectionRegistry class.     */
    /* ----------------------------------------------------------------------------------------- */
    /**
     * The projection method used.
     * use {@link #getProjection()} and {@link #setProjection(Projection)} for access.
     * Use {@link #setProjection(Projection)} in order to trigger a projection change event.
     */
    private static Projection proj;

    /**
     * Replies the current projection.
     *
     * @return
     */
    public static Projection getProjection() {
        return proj;
    }

    /**
     * Sets the current projection
     *
     * @param p the projection
     */
    public static void setProjection(Projection p) {
        CheckParameterUtil.ensureParameterNotNull(p);
        Projection oldValue = proj;
        proj = p;
        fireProjectionChanged(oldValue, proj);
    }

    /*
     * Keep WeakReferences to the listeners. This relieves clients from the burden of
     * explicitly removing the listeners and allows us to transparently register every
     * created dataset as projection change listener.
     */
    private static final ArrayList<WeakReference<ProjectionChangeListener>> listeners = new ArrayList<WeakReference<ProjectionChangeListener>>();

    private static void fireProjectionChanged(Projection oldValue, Projection newValue) {
        if (newValue == null ^ oldValue == null
                || (newValue != null && oldValue != null && !Utils.equal(newValue.toCode(), oldValue.toCode()))) {

            synchronized(Main.class) {
                Iterator<WeakReference<ProjectionChangeListener>> it = listeners.iterator();
                while(it.hasNext()){
                    WeakReference<ProjectionChangeListener> wr = it.next();
                    ProjectionChangeListener listener = wr.get();
                    if (listener == null) {
                        it.remove();
                        continue;
                    }
                    listener.projectionChanged(oldValue, newValue);
                }
            }
            if (newValue != null) {
                Bounds b = (Main.map != null && Main.map.mapView != null) ? Main.map.mapView.getRealBounds() : null;
                if (b != null){
                    Main.map.mapView.zoomTo(b);
                }
            }
            /* TODO - remove layers with fixed projection */
        }
    }

    /**
     * Register a projection change listener
     *
     * @param listener the listener. Ignored if null.
     */
    public static void addProjectionChangeListener(ProjectionChangeListener listener) {
        if (listener == null) return;
        synchronized (Main.class) {
            for (WeakReference<ProjectionChangeListener> wr : listeners) {
                // already registered ? => abort
                if (wr.get() == listener) return;
            }
            listeners.add(new WeakReference<ProjectionChangeListener>(listener));
        }
    }

    /**
     * Removes a projection change listener
     *
     * @param listener the listener. Ignored if null.
     */
    public static void removeProjectionChangeListener(ProjectionChangeListener listener) {
        if (listener == null) return;
        synchronized(Main.class){
            Iterator<WeakReference<ProjectionChangeListener>> it = listeners.iterator();
            while(it.hasNext()){
                WeakReference<ProjectionChangeListener> wr = it.next();
                // remove the listener - and any other listener which god garbage
                // collected in the meantime
                if (wr.get() == null || wr.get() == listener) {
                    it.remove();
                }
            }
        }
    }
}
