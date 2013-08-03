// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.gui;

import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trn;

import java.awt.Image;
import java.awt.Toolkit;
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.swing.JFrame;
import javax.swing.RepaintManager;
import javax.swing.SwingUtilities;

import gnu.getopt.Getopt;
import gnu.getopt.LongOpt;

import org.jdesktop.swinghelper.debug.CheckThreadViolationRepaintManager;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.AutosaveTask;
import org.openstreetmap.josm.data.CustomConfigurator;
import org.openstreetmap.josm.data.Preferences;
import org.openstreetmap.josm.data.Version;
import org.openstreetmap.josm.gui.download.DownloadDialog;
import org.openstreetmap.josm.gui.preferences.server.OAuthAccessTokenHolder;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.io.DefaultProxySelector;
import org.openstreetmap.josm.io.auth.CredentialsManager;
import org.openstreetmap.josm.io.auth.DefaultAuthenticator;
import org.openstreetmap.josm.io.remotecontrol.RemoteControl;
import org.openstreetmap.josm.plugins.PluginHandler;
import org.openstreetmap.josm.plugins.PluginInformation;
import org.openstreetmap.josm.tools.BugReportExceptionHandler;
import org.openstreetmap.josm.tools.I18n;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.Utils;

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
     * Constructs a main frame, ready sized and operating. Does not display the frame.
     * @param mainFrame The main JFrame of the application
     */
    public MainApplication(JFrame mainFrame) {
        addListener();
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
        System.out.println(tr("Java OpenStreetMap Editor")+" ["
                +Version.getInstance().getAgentString()+"]\n\n"+
                tr("usage")+":\n"+
                "\tjava -jar josm.jar <options>...\n\n"+
                tr("options")+":\n"+
                "\t--help|-h                                 "+tr("Show this help")+"\n"+
                "\t--geometry=widthxheight(+|-)x(+|-)y       "+tr("Standard unix geometry argument")+"\n"+
                "\t[--download=]minlat,minlon,maxlat,maxlon  "+tr("Download the bounding box")+"\n"+
                "\t[--download=]<URL>                        "+tr("Download the location at the URL (with lat=x&lon=y&zoom=z)")+"\n"+
                "\t[--download=]<filename>                   "+tr("Open a file (any file type that can be opened with File/Open)")+"\n"+
                "\t--downloadgps=minlat,minlon,maxlat,maxlon "+tr("Download the bounding box as raw GPS")+"\n"+
                "\t--downloadgps=<URL>                       "+tr("Download the location at the URL (with lat=x&lon=y&zoom=z) as raw GPS")+"\n"+
                "\t--selection=<searchstring>                "+tr("Select with the given search")+"\n"+
                "\t--[no-]maximize                           "+tr("Launch in maximized mode")+"\n"+
                "\t--reset-preferences                       "+tr("Reset the preferences to default")+"\n\n"+
                "\t--load-preferences=<url-to-xml>           "+tr("Changes preferences according to the XML file")+"\n\n"+
                "\t--set=<key>=<value>                       "+tr("Set preference key to value")+"\n\n"+
                "\t--language=<language>                     "+tr("Set the language")+"\n\n"+
                "\t--version                                 "+tr("Displays the JOSM version and exits")+"\n\n"+
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

    public enum Option {
        HELP(false),
        VERSION(false),
        LANGUAGE(true),
        RESET_PREFERENCES(false),
        LOAD_PREFERENCES(true),
        SET(true),
        GEOMETRY(true),
        NO_MAXIMIZE(false),
        MAXIMIZE(false),
        DOWNLOAD(true),
        DOWNLOADGPS(true),
        SELECTION(true);

        private String name;
        private boolean requiresArgument;

        private Option(boolean requiresArgument) {
            this.name = name().toLowerCase().replace("_", "-");
            this.requiresArgument = requiresArgument;
        }

        public String getName() {
            return name;
        }

        public boolean requiresArgument() {
            return requiresArgument;
        }

        public static Map<Option, Collection<String>> fromStringMap(Map<String, Collection<String>> opts) {
            Map<Option, Collection<String>> res = new HashMap<Option, Collection<String>>();
            for (Map.Entry<String, Collection<String>> e : opts.entrySet()) {
                Option o = Option.valueOf(e.getKey().toUpperCase().replace("-", "_"));
                if (o != null) {
                    res.put(o, e.getValue());
                }
            }
            return res;
        }
    }

    private static Map<Option, Collection<String>> buildCommandLineArgumentMap(String[] args) {

        List<LongOpt> los = new ArrayList<LongOpt>();
        for (Option o : Option.values()) {
            los.add(new LongOpt(o.getName(), o.requiresArgument() ? LongOpt.REQUIRED_ARGUMENT : LongOpt.NO_ARGUMENT, null, 0));
        }

        Getopt g = new Getopt("JOSM", args, "hv", los.toArray(new LongOpt[los.size()]));

        Map<Option, Collection<String>> argMap = new HashMap<Option, Collection<String>>();

        int c;
        while ((c = g.getopt()) != -1 ) {
            Option opt = null;
            switch (c) {
                case 'h':
                    opt = Option.HELP;
                    break;
                case 'v':
                    opt = Option.VERSION;
                    break;
                case 0:
                    opt = Option.values()[g.getLongind()];
                    break;
            }
            if (opt != null) {
                Collection<String> values = argMap.get(opt);
                if (values == null) {
                    values = new ArrayList<String>();
                    argMap.put(opt, values);
                }
                values.add(g.getOptarg());
            } else
                throw new IllegalArgumentException();
        }
        // positional arguments are a shortcut for the --download ... option
        for (int i = g.getOptind(); i < args.length; ++i) {
            Collection<String> values = argMap.get(Option.DOWNLOAD);
            if (values == null) {
                values = new ArrayList<String>();
                argMap.put(Option.DOWNLOAD, values);
            }
            values.add(args[i]);
        }

        return argMap;
    }

    /**
     * Main application Startup
     * @param argArray Command-line arguments
     */
    public static void main(final String[] argArray) {
        I18n.init();
        Main.checkJava6();

        // construct argument table
        Map<Option, Collection<String>> args = null;
        try {
            args = buildCommandLineArgumentMap(argArray);
        } catch (IllegalArgumentException e) {
            System.exit(1);
        }
        
        final boolean languageGiven = args.containsKey(Option.LANGUAGE);

        if (languageGiven) {
            I18n.set(args.get(Option.LANGUAGE).iterator().next());
        }

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

        Main.commandLineArgs = argArray;
        
        if (args.containsKey(Option.VERSION)) {
            System.out.println(Version.getInstance().getAgentString());
            System.exit(0);
        }

        Main.pref.init(args.containsKey(Option.RESET_PREFERENCES));

        if (!languageGiven) {
            I18n.set(Main.pref.get("language", null));
        }
        Main.pref.updateSystemProperties();

        final JFrame mainFrame = new JFrame(tr("Java OpenStreetMap Editor"));
        Main.parent = mainFrame;

        if (args.containsKey(Option.LOAD_PREFERENCES)) {
            CustomConfigurator.XMLCommandProcessor config = new CustomConfigurator.XMLCommandProcessor(Main.pref);
            for (String i : args.get(Option.LOAD_PREFERENCES)) {
                System.out.println("Reading preferences from " + i);
                try {
                    config.openAndReadXML(Utils.openURL(new URL(i)));
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            }
        }

        if (args.containsKey(Option.SET)) {
            for (String i : args.get(Option.SET)) {
                String[] kv = i.split("=", 2);
                Main.pref.put(kv[0], "null".equals(kv[1]) ? null : kv[1]);
            }
        }

        DefaultAuthenticator.createInstance();
        Authenticator.setDefault(DefaultAuthenticator.getInstance());
        ProxySelector.setDefault(new DefaultProxySelector(ProxySelector.getDefault()));
        OAuthAccessTokenHolder.getInstance().init(Main.pref, CredentialsManager.getInstance());

        // asking for help? show help and exit
        if (args.containsKey(Option.HELP)) {
            showHelp();
            System.exit(0);
        }

        final SplashScreen splash = new SplashScreen();
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
        final Main main = new MainApplication(mainFrame);

        monitor.indeterminateSubTask(tr("Loading plugins"));
        PluginHandler.loadLatePlugins(splash,pluginsToLoad,  monitor.createSubTaskMonitor(1, false));
        toolbar.refreshToolbarControl();

        GuiHelper.runInEDT(new Runnable() {
            @Override
            public void run() {
                splash.setVisible(false);
                splash.dispose();
                mainFrame.setVisible(true);
            }
        });

        Main.MasterWindowListener.setup();

        boolean maximized = Boolean.parseBoolean(Main.pref.get("gui.maximized"));
        if ((!args.containsKey(Option.NO_MAXIMIZE) && maximized) || args.containsKey(Option.MAXIMIZE)) {
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

        final Map<Option, Collection<String>> args_final = args;

        SwingUtilities.invokeLater(new Runnable() {
            @Override
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
                        dialog.setButtonIcons(new String[] {"ok", "cancel", "dialogs/delete"});
                        int selection = dialog.showDialog().getValue();
                        if (selection == 1) {
                            autosaveTask.recoverUnsavedLayers();
                        } else if (selection == 3) {
                            autosaveTask.dicardUnsavedLayers();
                        }
                    }
                    autosaveTask.schedule();
                }

                postConstructorProcessCmdLine(args_final);

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
