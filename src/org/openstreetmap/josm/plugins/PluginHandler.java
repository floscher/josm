//License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.plugins;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Font;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.UIManager;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.AboutAction;
import org.openstreetmap.josm.gui.MapFrame;
import org.openstreetmap.josm.gui.download.DownloadSelection;
import org.openstreetmap.josm.gui.ExtendedDialog; 
import org.openstreetmap.josm.gui.preferences.PreferenceSetting;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.ImageProvider;

public class PluginHandler {
    /**
     * All installed and loaded plugins (resp. their main classes)
     */
    public final static Collection<PluginProxy> pluginList = new LinkedList<PluginProxy>();
    /**
     * Load all plugins specified in preferences. If the parameter is
     * <code>true</code>, all early plugins are loaded (before constructor).
     */
    public static void loadPlugins(boolean early) {
        List<String> plugins = new LinkedList<String>();
        Collection<String> cp = Main.pref.getCollection("plugins", null);
        if (cp != null)
            plugins.addAll(cp);
        if (System.getProperty("josm.plugins") != null)
            plugins.addAll(Arrays.asList(System.getProperty("josm.plugins").split(",")));

        String [] oldplugins = new String[] {"mappaint", "unglueplugin",
        "lang-de", "lang-en_GB", "lang-fr", "lang-it", "lang-pl", "lang-ro",
        "lang-ru", "ewmsplugin", "ywms", "tways-0.2", "geotagged", "landsat",
        "namefinder", "waypoints", "slippy_map_chooser"};
        for (String p : oldplugins) {
            if (plugins.contains(p)) {
                plugins.remove(p);
                Main.pref.removeFromCollection("plugins", p);
                JOptionPane.showMessageDialog(Main.parent, tr("Warning - loading of {0} plugin was requested. This plugin is no longer required.", p));
            }
        }

        if (plugins.isEmpty())
            return;

        SortedMap<Integer, Collection<PluginInformation>> p = new TreeMap<Integer, Collection<PluginInformation>>();
        for (String pluginName : plugins) {
            PluginInformation info = PluginInformation.findPlugin(pluginName);
            if (info != null) {
                if (info.early != early)
                    continue;
                if (info.mainversion != null) {
                    int requiredJOSMVersion = 0;
                    try {
                        requiredJOSMVersion = Integer.parseInt(info.mainversion);
                    } catch(NumberFormatException e) {
                        e.printStackTrace();
                    }
                    if (requiredJOSMVersion > AboutAction.getVersionNumber()) {
                        JOptionPane.showMessageDialog(Main.parent, tr("Plugin requires JOSM update: {0}.", pluginName));
                        continue;
                    }
                }
                if(info.requires != null)
                {
                    String warn = null;
                    for(String n : info.requires.split(";"))
                    {
                        if(!plugins.contains(n))
                        { warn = n; break; }
                    }
                    if(warn != null)
                    {
                        JOptionPane.showMessageDialog(Main.parent,
                        tr("Plugin {0} is required by plugin {1} but was not found.",
                        warn, pluginName));
                        continue;
                    }
                }
                if (!p.containsKey(info.stage))
                    p.put(info.stage, new LinkedList<PluginInformation>());
                p.get(info.stage).add(info);
            } else {
                JOptionPane.showMessageDialog(Main.parent, tr("Plugin not found: {0}.", pluginName));
            }
        }

        if (!early) {
            long tim = System.currentTimeMillis();
            long last = Main.pref.getLong("pluginmanager.lastupdate", 0);
            Integer maxTime = Main.pref.getInteger("pluginmanager.warntime", 30);
            long d = (tim - last)/(24*60*60*1000l);
            if ((last <= 0) || (maxTime <= 0)) {
                Main.pref.put("pluginmanager.lastupdate",Long.toString(tim));
            } else if (d > maxTime) {
                JOptionPane.showMessageDialog(Main.parent,
                   "<html>" +
                   tr("Last plugin update more than {0} days ago.", d) +
                   "<br><em>" +
                   tr("(You can change the number of days after which this warning appears<br>by setting the config option 'pluginmanager.warntime'.)") +
                   "</html>");
            }
        }

        // iterate all plugins and collect all libraries of all plugins:
        List<URL> allPluginLibraries = new ArrayList<URL>();
        for (Collection<PluginInformation> c : p.values())
            for (PluginInformation info : c)
                allPluginLibraries.addAll(info.libraries);
        // create a classloader for all plugins:
        URL[] jarUrls = new URL[allPluginLibraries.size()];
        jarUrls = allPluginLibraries.toArray(jarUrls);
        URLClassLoader pluginClassLoader = new URLClassLoader(jarUrls, Main.class.getClassLoader());
        ImageProvider.sources.add(0, pluginClassLoader);

        for (Collection<PluginInformation> c : p.values()) {
            for (PluginInformation info : c) {
                try {
                    Class<?> klass = info.loadClass(pluginClassLoader);
                    if (klass != null) {
                        System.out.println("loading "+info.name);
                        pluginList.add(info.load(klass));
                    }
                } catch (Throwable e) {
                    e.printStackTrace();
                    
                    int result = new ExtendedDialog(Main.parent, 
                        tr("Disable plugin"), 
                        tr("Could not load plugin {0}. Delete from preferences?", info.name),
                        new String[] {tr("Disable plugin"), tr("Cancel")}, 
                        new String[] {"dialogs/delete.png", "cancel.png"}).getValue();  
                    
                    if(result == 1)
                    {
                        plugins.remove(info.name);
                        Main.pref.removeFromCollection("plugins", info.name);
                    }
                }
            }
        }
    }
    public static void setMapFrame(MapFrame old, MapFrame map) {
        for (PluginProxy plugin : pluginList)
            plugin.mapFrameInitialized(old, map);
    }

    public static Object getPlugin(String name) {
        for (PluginProxy plugin : pluginList)
            if(plugin.info.name.equals(name))
                return plugin.plugin;
        return null;
    }

    public static void addDownloadSelection(List<DownloadSelection> downloadSelections)
    {
        for (PluginProxy p : pluginList)
            p.addDownloadSelection(downloadSelections);
    }
    public static void getPreferenceSetting(Collection<PreferenceSetting> settings)
    {
        for (PluginProxy plugin : pluginList) {
            PreferenceSetting p = plugin.getPreferenceSetting();
            if (p != null)
                settings.add(p);
        }
    }

    public static void earlyCleanup()
    {
        if (!PluginDownloader.moveUpdatedPlugins()) {
            JOptionPane.showMessageDialog(null,
                    tr("Activating the updated plugins failed. Check if JOSM has the permission to overwrite the existing ones."),
                    tr("Plugins"), JOptionPane.ERROR_MESSAGE);
        }
    }
    public static Boolean checkException(Throwable e)
    {
        PluginProxy plugin = null;

        // Check for an explicit problem when calling a plugin function
        if (e instanceof PluginException)
            plugin = ((PluginException)e).plugin;

        if (plugin == null)
        {
            String name = null;
            /**
            * Analyze the stack of the argument and find a name of a plugin, if
            * some known problem pattern has been found.
            *
            * Note: This heuristic is not meant as discrimination against specific
            * plugins, but only to stop the flood of similar bug reports about plugins.
            * Of course, plugin writers are free to install their own version of
            * an exception handler with their email address listed to receive
            * bug reports ;-).
            */
            for (StackTraceElement element : e.getStackTrace()) {
                String c = element.getClassName();

                if (c.contains("wmsplugin.") || c.contains(".WMSLayer"))
                    name = "wmsplugin";
                if (c.contains("livegps."))
                    name = "livegps";
                if (c.startsWith("UtilsPlugin."))
                    name = "UtilsPlugin";

                if (c.startsWith("org.openstreetmap.josm.plugins.")) {
                    String p = c.substring("org.openstreetmap.josm.plugins.".length());
                    if (p.indexOf('.') != -1 && p.matches("[a-z].*")) {
                        name = p.substring(0,p.indexOf('.'));
                    }
                }
                if(name != null)
                  break;
            }
            for (PluginProxy p : pluginList)
            {
                if (p.info.name.equals(name))
                {
                    plugin = p;
                    break;
                }
            }
        }

        if (plugin != null) {
            int answer = new ExtendedDialog(Main.parent, 
                tr("Disable plugin"), 
                tr("An unexpected exception occurred that may have come from the ''{0}'' plugin.", plugin.info.name)
                    + "\n"
                    + (plugin.info.author != null
                        ? tr("According to the information within the plugin, the author is {0}.", plugin.info.author)
                        : "")
                    + "\n"
                    + tr("Try updating to the newest version of this plugin before reporting a bug.")
                    + "\n"
                    + tr("Should the plugin be disabled?"),
                new String[] {tr("Disable plugin"), tr("Cancel")}, 
                new String[] {"dialogs/delete.png", "cancel.png"}).getValue();  
            if (answer == 1) {
                LinkedList<String> plugins = new LinkedList<String>(Arrays.asList(Main.pref.get("plugins").split(",")));
                if (plugins.contains(plugin.info.name)) {
                    while (plugins.remove(plugin.info.name)) {}
                    String p = "";
                    for (String s : plugins)
                        p += ","+s;
                    if (p.length() > 0)
                        p = p.substring(1);
                    Main.pref.put("plugins", p);
                    JOptionPane.showMessageDialog(Main.parent,
                    tr("The plugin has been removed from the configuration. Please restart JOSM to unload the plugin."));
                } else {
                    JOptionPane.showMessageDialog(Main.parent,
                    tr("The plugin could not be removed. Please tell the people you got JOSM from about the problem."));
                }
                return true;
            }
        }
        return false;
    }
    public static String getBugReportText()
    {
        String text = "";
        String pl = Main.pref.get("plugins");
        if(pl != null && pl.length() != 0)
            text += "Plugins: "+pl+"\n";
        for (final PluginProxy pp : pluginList) {
            text += "Plugin " + pp.info.name + (pp.info.version != null && !pp.info.version.equals("") ? " Version: "+pp.info.version+"\n" : "\n");
        }
        return text;
    }
    public static JPanel getInfoPanel()
    {
        JPanel pluginTab = new JPanel(new GridBagLayout());
        for (final PluginProxy p : pluginList) {
            String name = p.info.name + (p.info.version != null && !p.info.version.equals("") ? " Version: "+p.info.version : "");
            pluginTab.add(new JLabel(name), GBC.std());
            pluginTab.add(Box.createHorizontalGlue(), GBC.std().fill(GBC.HORIZONTAL));
            pluginTab.add(new JButton(new AbstractAction(tr("Information")){
                public void actionPerformed(ActionEvent event) {
                    StringBuilder b = new StringBuilder();
                    for (Entry<String,String> e : p.info.attr.entrySet()) {
                        b.append(e.getKey());
                        b.append(": ");
                        b.append(e.getValue());
                        b.append("\n");
                    }
                    JTextArea a = new JTextArea(10,40);
                    a.setEditable(false);
                    a.setText(b.toString());
                    JOptionPane.showMessageDialog(Main.parent, new JScrollPane(a));
                }
            }), GBC.eol());

            JTextArea description = new JTextArea((p.info.description==null? tr("no description available"):p.info.description));
            description.setEditable(false);
            description.setFont(new JLabel().getFont().deriveFont(Font.ITALIC));
            description.setLineWrap(true);
            description.setWrapStyleWord(true);
            description.setBorder(BorderFactory.createEmptyBorder(0,20,0,0));
            description.setBackground(UIManager.getColor("Panel.background"));

            pluginTab.add(description, GBC.eop().fill(GBC.HORIZONTAL));
        }
        return pluginTab;
    }
}
