// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.gui;

import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trn;

import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.net.Authenticator;
import java.net.ProxySelector;
import java.net.URL;
import java.security.AllPermission;
import java.security.CodeSource;
import java.security.PermissionCollection;
import java.security.Permissions;
import java.security.Policy;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.swing.JFrame;
import javax.swing.RepaintManager;
import javax.swing.SwingUtilities;

import org.jdesktop.swinghelper.debug.CheckThreadViolationRepaintManager;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.AutosaveTask;
import org.openstreetmap.josm.data.CustomConfigurator;
import org.openstreetmap.josm.data.Preferences;
import org.openstreetmap.josm.data.Version;
import org.openstreetmap.josm.gui.download.DownloadDialog;
import org.openstreetmap.josm.gui.preferences.server.OAuthAccessTokenHolder;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.io.DefaultProxySelector;
import org.openstreetmap.josm.io.auth.CredentialsManager;
import org.openstreetmap.josm.io.auth.DefaultAuthenticator;
import org.openstreetmap.josm.io.remotecontrol.RemoteControl;
import org.openstreetmap.josm.plugins.PluginHandler;
import org.openstreetmap.josm.plugins.PluginInformation;
import org.openstreetmap.josm.tools.BugReportExceptionHandler;
import org.openstreetmap.josm.tools.I18n;
import org.openstreetmap.josm.tools.ImageProvider;

/**
 * Main window class application.
 *
 * @author imi
 */
public class MainApplication extends Main {
    /**
     * Allow subclassing (see JOSM.java)
     */
    public MainApplication() {}

    /**
     * Construct an main frame, ready sized and operating. Does not
     * display the frame.
     */
    public MainApplication(JFrame mainFrame) {
        super();
        mainFrame.setContentPane(contentPanePrivate);
        mainFrame.setJMenuBar(menu);
        geometry.applySafe(mainFrame);
        LinkedList<Image> l = new LinkedList<Image>();
        l.add(ImageProvider.get("logo_16x16x32").getImage());
        l.add(ImageProvider.get("logo_16x16x8").getImage());
        l.add(ImageProvider.get("logo_32x32x32").getImage());
        l.add(ImageProvider.get("logo_32x32x8").getImage());
        l.add(ImageProvider.get("logo_48x48x32").getImage());
        l.add(ImageProvider.get("logo_48x48x8").getImage());
        l.add(ImageProvider.get("logo").getImage());
        mainFrame.setIconImages(l);
        mainFrame.addWindowListener(new WindowAdapter(){
            @Override public void windowClosing(final WindowEvent arg0) {
                Main.exitJosm(true);
            }
        });
        mainFrame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
    }

    /**
     * Displays help on the console
     *
     */
    public static void showHelp() {
        // TODO: put in a platformHook for system that have no console by default
        System.out.println(tr("Java OpenStreetMap Editor")+"\n\n"+
                tr("usage")+":\n"+
                "\tjava -jar josm.jar <options>...\n\n"+
                tr("options")+":\n"+
                "\t--help|-?|-h                              "+tr("Show this help")+"\n"+
                "\t--geometry=widthxheight(+|-)x(+|-)y       "+tr("Standard unix geometry argument")+"\n"+
                "\t[--download=]minlat,minlon,maxlat,maxlon  "+tr("Download the bounding box")+"\n"+
                "\t[--download=]<url>                        "+tr("Download the location at the url (with lat=x&lon=y&zoom=z)")+"\n"+
                "\t[--download=]<filename>                   "+tr("Open a file (any file type that can be opened with File/Open)")+"\n"+
                "\t--downloadgps=minlat,minlon,maxlat,maxlon "+tr("Download the bounding box as raw gps")+"\n"+
                "\t--downloadgps=<url>                       "+tr("Download the location at the url (with lat=x&lon=y&zoom=z) as raw gps")+"\n"+
                "\t--selection=<searchstring>                "+tr("Select with the given search")+"\n"+
                "\t--[no-]maximize                           "+tr("Launch in maximized mode")+"\n"+
                "\t--reset-preferences                       "+tr("Reset the preferences to default")+"\n\n"+
                "\t--load-preferences=<url-to-xml>           "+tr("Changes preferences according to the XML file")+"\n\n"+
                "\t--set=<key>=<value>                       "+tr("Set preference key to value")+"\n\n"+
                "\t--language=<language>                     "+tr("Set the language")+"\n\n"+
                tr("options provided as Java system properties")+":\n"+
                "\t-Djosm.home="+tr("/PATH/TO/JOSM/FOLDER/         ")+tr("Change the folder for all user settings")+"\n\n"+
                tr("note: For some tasks, JOSM needs a lot of memory. It can be necessary to add the following\n" +
                        "      Java option to specify the maximum size of allocated memory in megabytes")+":\n"+
                        "\t-Xmx...m\n\n"+
                        tr("examples")+":\n"+
                        "\tjava -jar josm.jar track1.gpx track2.gpx london.osm\n"+
                        "\tjava -jar josm.jar http://www.openstreetmap.org/index.html?lat=43.2&lon=11.1&zoom=13\n"+
                        "\tjava -jar josm.jar london.osm --selection=http://www.ostertag.name/osm/OSM_errors_node-duplicate.xml\n"+
                        "\tjava -jar josm.jar 43.2,11.1,43.4,11.4\n"+
                        "\tjava -Djosm.home=/home/user/.josm_dev -jar josm.jar\n"+
                        "\tjava -Xmx400m -jar josm.jar\n\n"+
                        tr("Parameters --download, --downloadgps, and --selection are processed in this order.")+"\n"+
                        tr("Make sure you load some data if you use --selection.")+"\n"
                );
    }

    private static Map<String, Collection<String>> buildCommandLineArgumentMap(String[] args) {
        Map<String, Collection<String>> argMap = new HashMap<String, Collection<String>>();
        for (String arg : args) {
            if ("-h".equals(arg) || "-?".equals(arg)) {
                arg = "--help";
            } else if ("-v".equals(arg)) {
                arg = "--version";
            }
            // handle simple arguments like file names, URLs, bounds
            if (!arg.startsWith("--")) {
                arg = "--download="+arg;
            }
            int i = arg.indexOf('=');
            String key = i == -1 ? arg.substring(2) : arg.substring(2,i);
            String value = i == -1 ? "" : arg.substring(i+1);
            Collection<String> v = argMap.get(key);
            if (v == null) {
                v = new LinkedList<String>();
            }
            v.add(value);
            argMap.put(key, v);
        }
        return argMap;
    }

    /**
     * Main application Startup
     */
    public static void main(final String[] argArray) {
        I18n.init();
        Main.checkJava6();
        Main.pref = new Preferences();

        Policy.setPolicy(new Policy() {
            // Permissions for plug-ins loaded when josm is started via webstart
            private PermissionCollection pc;

            {
                pc = new Permissions();
                pc.add(new AllPermission());
            }

            @Override
            public void refresh() { }

            @Override
            public PermissionCollection getPermissions(CodeSource codesource) {
                return pc;
            }
        });

        Thread.setDefaultUncaughtExceptionHandler(new BugReportExceptionHandler());
        // http://stuffthathappens.com/blog/2007/10/15/one-more-note-on-uncaught-exception-handlers/
        System.setProperty("sun.awt.exception.handler", BugReportExceptionHandler.class.getName());

        // initialize the platform hook, and
        Main.determinePlatformHook();
        // call the really early hook before we do anything else
        Main.platform.preStartupHook();

        // construct argument table
        final Map<String, Collection<String>> args = buildCommandLineArgumentMap(argArray);

        if (args.containsKey("version")) {
            System.out.println(Version.getInstance().getAgentString());
            System.exit(0);
        } else {
            System.out.println(Version.getInstance().getReleaseAttributes());
        }

        Main.pref.init(args.containsKey("reset-preferences"));

        // Check if passed as parameter
        if (args.containsKey("language")) {
            I18n.set(args.get("language").iterator().next());
        } else {
            I18n.set(Main.pref.get("language", null));
        }
        Main.pref.updateSystemProperties();

        JFrame mainFrame = new JFrame(tr("Java OpenStreetMap Editor"));
        Main.parent = mainFrame;

        if (args.containsKey("load-preferences")) {
            CustomConfigurator.XMLCommandProcessor config = new CustomConfigurator.XMLCommandProcessor(Main.pref);
            for (String i : args.get("load-preferences")) {
                System.out.println("Reading preferences from " + i);
                try {
                    URL url = new URL(i);
                    config.openAndReadXML(url.openStream());
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            }
        }

        if (args.containsKey("set")) {
            for (String i : args.get("set")) {
                String[] kv = i.split("=", 2);
                Main.pref.put(kv[0], "null".equals(kv[1]) ? null : kv[1]);
            }
        }

        DefaultAuthenticator.createInstance();
        Authenticator.setDefault(DefaultAuthenticator.getInstance());
        ProxySelector.setDefault(new DefaultProxySelector(ProxySelector.getDefault()));
        OAuthAccessTokenHolder.getInstance().init(Main.pref, CredentialsManager.getInstance());

        // asking for help? show help and exit
        if (args.containsKey("help")) {
            showHelp();
            System.exit(0);
        }

        SplashScreen splash = new SplashScreen();
        final ProgressMonitor monitor = splash.getProgressMonitor();
        monitor.beginTask(tr("Initializing"));
        splash.setVisible(Main.pref.getBoolean("draw.splashscreen", true));
        Main.setInitStatusListener(new InitStatusListener() {

            @Override
            public void updateStatus(String event) {
                monitor.indeterminateSubTask(event);
            }
        });

        List<PluginInformation> pluginsToLoad = PluginHandler.buildListOfPluginsToLoad(splash,monitor.createSubTaskMonitor(1, false));
        if (!pluginsToLoad.isEmpty() && PluginHandler.checkAndConfirmPluginUpdate(splash)) {
            monitor.subTask(tr("Updating plugins"));
            pluginsToLoad = PluginHandler.updatePlugins(splash,pluginsToLoad, monitor.createSubTaskMonitor(1, false));
        }

        monitor.indeterminateSubTask(tr("Installing updated plugins"));
        PluginHandler.installDownloadedPlugins(true);

        monitor.indeterminateSubTask(tr("Loading early plugins"));
        PluginHandler.loadEarlyPlugins(splash,pluginsToLoad, monitor.createSubTaskMonitor(1, false));

        monitor.indeterminateSubTask(tr("Setting defaults"));
        preConstructorInit(args);

        monitor.indeterminateSubTask(tr("Creating main GUI"));
        Main.addListener();
        final Main main = new MainApplication(mainFrame);

        monitor.indeterminateSubTask(tr("Loading plugins"));
        PluginHandler.loadLatePlugins(splash,pluginsToLoad,  monitor.createSubTaskMonitor(1, false));
        toolbar.refreshToolbarControl();
        splash.setVisible(false);
        splash.dispose();
        mainFrame.setVisible(true);

        boolean maximized = Boolean.parseBoolean(Main.pref.get("gui.maximized"));
        if ((!args.containsKey("no-maximize") && maximized) || args.containsKey("maximize")) {
            if (Toolkit.getDefaultToolkit().isFrameStateSupported(JFrame.MAXIMIZED_BOTH)) {
                // Main.debug("Main window maximized");
                Main.windowState = JFrame.MAXIMIZED_BOTH;
                mainFrame.setExtendedState(Main.windowState);
            } else {
                Main.debug("Main window: maximizing not supported");
            }
        } else {
            // Main.debug("Main window not maximized");
        }
        if(main.menu.fullscreenToggleAction != null) {
            main.menu.fullscreenToggleAction.initial();
        }

        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                if (AutosaveTask.PROP_AUTOSAVE_ENABLED.get()) {
                    AutosaveTask autosaveTask = new AutosaveTask();
                    List<File> unsavedLayerFiles = autosaveTask.getUnsavedLayersFiles();
                    if (!unsavedLayerFiles.isEmpty()) {
                        ExtendedDialog dialog = new ExtendedDialog(
                                Main.parent,
                                tr("Unsaved osm data"),
                                new String[] {tr("Restore"), tr("Cancel"), tr("Discard")}
                                );
                        dialog.setContent(
                                trn("JOSM found {0} unsaved osm data layer. ",
                                        "JOSM found {0} unsaved osm data layers. ", unsavedLayerFiles.size(), unsavedLayerFiles.size()) +
                                        tr("It looks like JOSM crashed last time. Would you like to restore the data?"));
                        dialog.setButtonIcons(new String[] {"ok", "cancel", "dialogs/remove"});
                        int selection = dialog.showDialog().getValue();
                        if (selection == 1) {
                            autosaveTask.recoverUnsavedLayers();
                        } else if (selection == 3) {
                            autosaveTask.dicardUnsavedLayers();
                        }
                    }
                    autosaveTask.schedule();
                }

                main.postConstructorProcessCmdLine(args);

                DownloadDialog.autostartIfNeeded();
            }
        });

        if (RemoteControl.PROP_REMOTECONTROL_ENABLED.get()) {
            RemoteControl.start();
        }

        if (Main.pref.getBoolean("debug.edt-checker.enable", Version.getInstance().isLocalBuild())) {
            // Repaint manager is registered so late for a reason - there is lots of violation during startup process but they don't seem to break anything and are difficult to fix
            System.out.println("Enabled EDT checker, wrongful access to gui from non EDT thread will be printed to console");
            RepaintManager.setCurrentManager(new CheckThreadViolationRepaintManager());
        }
    }
}
