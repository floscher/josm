// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.data;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Color;
import java.awt.Toolkit;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Reader;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.ResourceBundle;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JOptionPane;
import javax.swing.UIManager;
import javax.xml.XMLConstants;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.preferences.ColorProperty;
import org.openstreetmap.josm.io.MirroredInputStream;
import org.openstreetmap.josm.io.XmlWriter;
import org.openstreetmap.josm.tools.ColorHelper;
import org.openstreetmap.josm.tools.Utils;

/**
 * This class holds all preferences for JOSM.
 *
 * Other classes can register their beloved properties here. All properties will be
 * saved upon set-access.
 *
 * Each property is a key=setting pair, where key is a String and setting can be one of
 * 4 types:
 *     string, list, list of lists and list of maps.
 * In addition, each key has a unique default value that is set when the value is first
 * accessed using one of the get...() methods. You can use the same preference
 * key in different parts of the code, but the default value must be the same
 * everywhere. A default value of null means, the setting has been requested, but
 * no default value was set. This is used in advanced preferences to present a list
 * off all possible settings.
 *
 * At the moment, you cannot put the empty string for string properties.
 * put(key, "") means, the property is removed.
 *
 * @author imi
 */
public class Preferences {
    /**
     * Internal storage for the preference directory.
     * Do not access this variable directly!
     * @see #getPreferencesDirFile()
     */
    private File preferencesDirFile = null;
    /**
     * Internal storage for the cache directory.
     */
    private File cacheDirFile = null;

    /**
     * Map the property name to strings. Does not contain null or "" values.
     */
    protected final SortedMap<String, String> properties = new TreeMap<String, String>();
    /** Map of defaults, can contain null values */
    protected final SortedMap<String, String> defaults = new TreeMap<String, String>();
    protected final SortedMap<String, String> colornames = new TreeMap<String, String>();

    /** Mapping for list settings. Must not contain null values */
    protected final SortedMap<String, List<String>> collectionProperties = new TreeMap<String, List<String>>();
    /** Defaults, can contain null values */
    protected final SortedMap<String, List<String>> collectionDefaults = new TreeMap<String, List<String>>();

    protected final SortedMap<String, List<List<String>>> arrayProperties = new TreeMap<String, List<List<String>>>();
    protected final SortedMap<String, List<List<String>>> arrayDefaults = new TreeMap<String, List<List<String>>>();

    protected final SortedMap<String, List<Map<String,String>>> listOfStructsProperties = new TreeMap<String, List<Map<String,String>>>();
    protected final SortedMap<String, List<Map<String,String>>> listOfStructsDefaults = new TreeMap<String, List<Map<String,String>>>();

    /**
     * Interface for a preference value
     *
     * @param <T> the data type for the value
     */
    public interface Setting<T> {
        /**
         * Returns the value of this setting.
         *
         * @return the value of this setting
         */
        T getValue();

        /**
         * Enable usage of the visitor pattern.
         *
         * @param visitor the visitor
         */
        void visit(SettingVisitor visitor);

        /**
         * Returns a setting whose value is null.
         *
         * Cannot be static, because there is no static inheritance.
         * @return a Setting object that isn't null itself, but returns null
         * for {@link #getValue()}
         */
        Setting<T> getNullInstance();
    }

    /**
     * Base abstract class of all settings, holding the setting value.
     *
     * @param <T> The setting type
     */
    abstract public static class AbstractSetting<T> implements Setting<T> {
        private final T value;
        /**
         * Constructs a new {@code AbstractSetting} with the given value
         * @param value The setting value
         */
        public AbstractSetting(T value) {
            this.value = value;
        }
        @Override public T getValue() {
            return value;
        }
        @Override public String toString() {
            return value != null ? value.toString() : "null";
        }
    }

    /**
     * Setting containing a {@link String} value.
     */
    public static class StringSetting extends AbstractSetting<String> {
        /**
         * Constructs a new {@code StringSetting} with the given value
         * @param value The setting value
         */
        public StringSetting(String value) {
            super(value);
        }
        @Override public void visit(SettingVisitor visitor) {
            visitor.visit(this);
        }
        @Override public StringSetting getNullInstance() {
            return new StringSetting(null);
        }
    }

    /**
     * Setting containing a {@link List} of {@link String} values.
     */
    public static class ListSetting extends AbstractSetting<List<String>> {
        /**
         * Constructs a new {@code ListSetting} with the given value
         * @param value The setting value
         */
        public ListSetting(List<String> value) {
            super(value);
        }
        @Override public void visit(SettingVisitor visitor) {
            visitor.visit(this);
        }
        @Override public ListSetting getNullInstance() {
            return new ListSetting(null);
        }
    }

    /**
     * Setting containing a {@link List} of {@code List}s of {@link String} values.
     */
    public static class ListListSetting extends AbstractSetting<List<List<String>>> {
        /**
         * Constructs a new {@code ListListSetting} with the given value
         * @param value The setting value
         */
        public ListListSetting(List<List<String>> value) {
            super(value);
        }
        @Override public void visit(SettingVisitor visitor) {
            visitor.visit(this);
        }
        @Override public ListListSetting getNullInstance() {
            return new ListListSetting(null);
        }
    }

    /**
     * Setting containing a {@link List} of {@link Map}s of {@link String} values.
     */
    public static class MapListSetting extends AbstractSetting<List<Map<String, String>>> {
        /**
         * Constructs a new {@code MapListSetting} with the given value
         * @param value The setting value
         */
        public MapListSetting(List<Map<String, String>> value) {
            super(value);
        }
        @Override public void visit(SettingVisitor visitor) {
            visitor.visit(this);
        }
        @Override public MapListSetting getNullInstance() {
            return new MapListSetting(null);
        }
    }

    public interface SettingVisitor {
        void visit(StringSetting setting);
        void visit(ListSetting value);
        void visit(ListListSetting value);
        void visit(MapListSetting value);
    }

    public interface PreferenceChangeEvent<T> {
        String getKey();
        Setting<T> getOldValue();
        Setting<T> getNewValue();
    }

    public interface PreferenceChangedListener {
        void preferenceChanged(PreferenceChangeEvent e);
    }

    private static class DefaultPreferenceChangeEvent<T> implements PreferenceChangeEvent<T> {
        private final String key;
        private final Setting<T> oldValue;
        private final Setting<T> newValue;

        public DefaultPreferenceChangeEvent(String key, Setting<T> oldValue, Setting<T> newValue) {
            this.key = key;
            this.oldValue = oldValue;
            this.newValue = newValue;
        }

        public String getKey() {
            return key;
        }
        public Setting<T> getOldValue() {
            return oldValue;
        }
        public Setting<T> getNewValue() {
            return newValue;
        }
    }

    public interface ColorKey {
        String getColorName();
        String getSpecialName();
        Color getDefaultValue();
    }

    private final CopyOnWriteArrayList<PreferenceChangedListener> listeners = new CopyOnWriteArrayList<PreferenceChangedListener>();

    public void addPreferenceChangeListener(PreferenceChangedListener listener) {
        if (listener != null) {
            listeners.addIfAbsent(listener);
        }
    }

    public void removePreferenceChangeListener(PreferenceChangedListener listener) {
        listeners.remove(listener);
    }

    protected <T> void firePreferenceChanged(String key, Setting<T> oldValue, Setting<T> newValue) {
        PreferenceChangeEvent<T> evt = new DefaultPreferenceChangeEvent<T>(key, oldValue, newValue);
        for (PreferenceChangedListener l : listeners) {
            l.preferenceChanged(evt);
        }
    }

    /**
     * Returns the location of the user defined preferences directory
     * @return The location of the user defined preferences directory
     */
    public String getPreferencesDir() {
        final String path = getPreferencesDirFile().getPath();
        if (path.endsWith(File.separator))
            return path;
        return path + File.separator;
    }

    /**
     * Returns the user defined preferences directory
     * @return The user defined preferences directory
     */
    public File getPreferencesDirFile() {
        if (preferencesDirFile != null)
            return preferencesDirFile;
        String path;
        path = System.getProperty("josm.home");
        if (path != null) {
            preferencesDirFile = new File(path).getAbsoluteFile();
        } else {
            path = System.getenv("APPDATA");
            if (path != null) {
                preferencesDirFile = new File(path, "JOSM");
            } else {
                preferencesDirFile = new File(System.getProperty("user.home"), ".josm");
            }
        }
        return preferencesDirFile;
    }

    /**
     * Returns the user preferences file
     * @return The user preferences file
     */
    public File getPreferenceFile() {
        return new File(getPreferencesDirFile(), "preferences.xml");
    }

    /**
     * Returns the user plugin directory
     * @return The user plugin directory
     */
    public File getPluginsDirectory() {
        return new File(getPreferencesDirFile(), "plugins");
    }

    /**
     * Get the directory where cached content of any kind should be stored.
     *
     * If the directory doesn't exist on the file system, it will be created
     * by this method.
     *
     * @return the cache directory
     */
    public File getCacheDirectory() {
        if (cacheDirFile != null)
            return cacheDirFile;
        String path = System.getProperty("josm.cache");
        if (path != null) {
            cacheDirFile = new File(path).getAbsoluteFile();
        } else {
            path = get("cache.folder", null);
            if (path != null) {
                cacheDirFile = new File(path);
            } else {
                cacheDirFile = new File(getPreferencesDirFile(), "cache");
            }
        }
        if (!cacheDirFile.exists() && !cacheDirFile.mkdirs()) {
            System.err.println(tr("Warning: Failed to create missing cache directory: {0}", cacheDirFile.getAbsoluteFile()));
            JOptionPane.showMessageDialog(
                    Main.parent,
                    tr("<html>Failed to create missing cache directory: {0}</html>", cacheDirFile.getAbsoluteFile()),
                    tr("Error"),
                    JOptionPane.ERROR_MESSAGE
            );
        }
        return cacheDirFile;
    }

    /**
     * @return A list of all existing directories where resources could be stored.
     */
    public Collection<String> getAllPossiblePreferenceDirs() {
        LinkedList<String> locations = new LinkedList<String>();
        locations.add(getPreferencesDir());
        String s;
        if ((s = System.getenv("JOSM_RESOURCES")) != null) {
            if (!s.endsWith(File.separator)) {
                s = s + File.separator;
            }
            locations.add(s);
        }
        if ((s = System.getProperty("josm.resources")) != null) {
            if (!s.endsWith(File.separator)) {
                s = s + File.separator;
            }
            locations.add(s);
        }
        String appdata = System.getenv("APPDATA");
        if (System.getenv("ALLUSERSPROFILE") != null && appdata != null
                && appdata.lastIndexOf(File.separator) != -1) {
            appdata = appdata.substring(appdata.lastIndexOf(File.separator));
            locations.add(new File(new File(System.getenv("ALLUSERSPROFILE"),
                    appdata), "JOSM").getPath());
        }
        locations.add("/usr/local/share/josm/");
        locations.add("/usr/local/lib/josm/");
        locations.add("/usr/share/josm/");
        locations.add("/usr/lib/josm/");
        return locations;
    }

    /**
     * Get settings value for a certain key.
     * @param key the identifier for the setting
     * @return "" if there is nothing set for the preference key,
     *  the corresponding value otherwise. The result is not null.
     */
    synchronized public String get(final String key) {
        putDefault(key, null);
        if (!properties.containsKey(key))
            return "";
        return properties.get(key);
    }

    /**
     * Get settings value for a certain key and provide default a value.
     * @param key the identifier for the setting
     * @param def the default value. For each call of get() with a given key, the
     *  default value must be the same.
     * @return the corresponding value if the property has been set before,
     *  def otherwise
     */
    synchronized public String get(final String key, final String def) {
        putDefault(key, def);
        final String prop = properties.get(key);
        if (prop == null || prop.equals(""))
            return def;
        return prop;
    }

    synchronized public Map<String, String> getAllPrefix(final String prefix) {
        final Map<String,String> all = new TreeMap<String,String>();
        for (final Entry<String,String> e : properties.entrySet()) {
            if (e.getKey().startsWith(prefix)) {
                all.put(e.getKey(), e.getValue());
            }
        }
        return all;
    }

    synchronized public List<String> getAllPrefixCollectionKeys(final String prefix) {
        final List<String> all = new LinkedList<String>();
        for (final String e : collectionProperties.keySet()) {
            if (e.startsWith(prefix)) {
                all.add(e);
            }
        }
        return all;
    }

    synchronized private Map<String, String> getAllPrefixDefault(final String prefix) {
        final Map<String,String> all = new TreeMap<String,String>();
        for (final Entry<String,String> e : defaults.entrySet()) {
            if (e.getKey().startsWith(prefix)) {
                all.put(e.getKey(), e.getValue());
            }
        }
        return all;
    }

    synchronized public TreeMap<String, String> getAllColors() {
        final TreeMap<String,String> all = new TreeMap<String,String>();
        for (final Entry<String,String> e : defaults.entrySet()) {
            if (e.getKey().startsWith("color.") && e.getValue() != null) {
                all.put(e.getKey().substring(6), e.getValue());
            }
        }
        for (final Entry<String,String> e : properties.entrySet()) {
            if (e.getKey().startsWith("color.")) {
                all.put(e.getKey().substring(6), e.getValue());
            }
        }
        return all;
    }

    synchronized public Map<String, String> getDefaults() {
        return defaults;
    }

    synchronized public void putDefault(final String key, final String def) {
        if(!defaults.containsKey(key) || defaults.get(key) == null) {
            defaults.put(key, def);
        } else if(def != null && !defaults.get(key).equals(def)) {
            System.out.println("Defaults for " + key + " differ: " + def + " != " + defaults.get(key));
        }
    }

    synchronized public boolean getBoolean(final String key) {
        putDefault(key, null);
        return properties.containsKey(key) ? Boolean.parseBoolean(properties.get(key)) : false;
    }

    synchronized public boolean getBoolean(final String key, final boolean def) {
        putDefault(key, Boolean.toString(def));
        return properties.containsKey(key) ? Boolean.parseBoolean(properties.get(key)) : def;
    }

    synchronized public boolean getBoolean(final String key, final String specName, final boolean def) {
        putDefault(key, Boolean.toString(def));
        String skey = key+"."+specName;
        if(properties.containsKey(skey))
            return Boolean.parseBoolean(properties.get(skey));
        return properties.containsKey(key) ? Boolean.parseBoolean(properties.get(key)) : def;
    }

    /**
     * Set a value for a certain setting. The changed setting is saved
     * to the preference file immediately. Due to caching mechanisms on modern
     * operating systems and hardware, this shouldn't be a performance problem.
     * @param key the unique identifier for the setting
     * @param value the value of the setting. Can be null or "" which both removes
     *  the key-value entry.
     * @return if true, something has changed (i.e. value is different than before)
     */
    public boolean put(final String key, String value) {
        boolean changed = false;
        String oldValue = null;

        synchronized (this) {
            oldValue = properties.get(key);
            if(value != null && value.length() == 0) {
                value = null;
            }
            // value is the same as before - no need to save anything
            boolean equalValue = oldValue != null && oldValue.equals(value);
            // The setting was previously unset and we are supposed to put a
            // value that equals the default value. This is not necessary because
            // the default value is the same throughout josm. In addition we like
            // to have the possibility to change the default value from version
            // to version, which would not work if we wrote it to the preference file.
            boolean unsetIsDefault = oldValue == null && (value == null || value.equals(defaults.get(key)));

            if (!(equalValue || unsetIsDefault)) {
                if (value == null) {
                    properties.remove(key);
                } else {
                    properties.put(key, value);
                }
                try {
                    save();
                } catch(IOException e){
                    System.out.println(tr("Warning: failed to persist preferences to ''{0}''", getPreferenceFile().getAbsoluteFile()));
                }
                changed = true;
            }
        }
        if (changed) {
            // Call outside of synchronized section in case some listener wait for other thread that wait for preference lock
            firePreferenceChanged(key, new StringSetting(oldValue), new StringSetting(value));
        }
        return changed;
    }

    public boolean put(final String key, final boolean value) {
        return put(key, Boolean.toString(value));
    }

    public boolean putInteger(final String key, final Integer value) {
        return put(key, Integer.toString(value));
    }

    public boolean putDouble(final String key, final Double value) {
        return put(key, Double.toString(value));
    }

    public boolean putLong(final String key, final Long value) {
        return put(key, Long.toString(value));
    }

    /**
     * Called after every put. In case of a problem, do nothing but output the error
     * in log.
     */
    public void save() throws IOException {
        /* currently unused, but may help to fix configuration issues in future */
        putInteger("josm.version", Version.getInstance().getVersion());

        updateSystemProperties();
        if(Main.applet)
            return;

        File prefFile = getPreferenceFile();
        File backupFile = new File(prefFile + "_backup");

        // Backup old preferences if there are old preferences
        if (prefFile.exists()) {
            Utils.copyFile(prefFile, backupFile);
        }

        final PrintWriter out = new PrintWriter(new OutputStreamWriter(
                new FileOutputStream(prefFile + "_tmp"), "utf-8"), false);
        out.print(toXML(false));
        Utils.close(out);

        File tmpFile = new File(prefFile + "_tmp");
        Utils.copyFile(tmpFile, prefFile);
        tmpFile.delete();

        setCorrectPermissions(prefFile);
        setCorrectPermissions(backupFile);
    }


    private void setCorrectPermissions(File file) {
        file.setReadable(false, false);
        file.setWritable(false, false);
        file.setExecutable(false, false);
        file.setReadable(true, true);
        file.setWritable(true, true);
    }

    public void load() throws Exception {
        properties.clear();
        if (!Main.applet) {
            File pref = getPreferenceFile();
            BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(pref), "utf-8"));
            try {
                validateXML(in);
                Utils.close(in);
                in = new BufferedReader(new InputStreamReader(new FileInputStream(pref), "utf-8"));
                fromXML(in);
            } finally {
                Utils.close(in);
            }
        }
        updateSystemProperties();
        removeObsolete();
    }

    public void init(boolean reset){
        if(Main.applet)
            return;
        // get the preferences.
        File prefDir = getPreferencesDirFile();
        if (prefDir.exists()) {
            if(!prefDir.isDirectory()) {
                System.err.println(tr("Warning: Failed to initialize preferences. Preference directory ''{0}'' is not a directory.", prefDir.getAbsoluteFile()));
                JOptionPane.showMessageDialog(
                        Main.parent,
                        tr("<html>Failed to initialize preferences.<br>Preference directory ''{0}'' is not a directory.</html>", prefDir.getAbsoluteFile()),
                        tr("Error"),
                        JOptionPane.ERROR_MESSAGE
                );
                return;
            }
        } else {
            if (! prefDir.mkdirs()) {
                System.err.println(tr("Warning: Failed to initialize preferences. Failed to create missing preference directory: {0}", prefDir.getAbsoluteFile()));
                JOptionPane.showMessageDialog(
                        Main.parent,
                        tr("<html>Failed to initialize preferences.<br>Failed to create missing preference directory: {0}</html>",prefDir.getAbsoluteFile()),
                        tr("Error"),
                        JOptionPane.ERROR_MESSAGE
                );
                return;
            }
        }

        File preferenceFile = getPreferenceFile();
        try {
            if (!preferenceFile.exists()) {
                System.out.println(tr("Info: Missing preference file ''{0}''. Creating a default preference file.", preferenceFile.getAbsoluteFile()));
                resetToDefault();
                save();
            } else if (reset) {
                System.out.println(tr("Warning: Replacing existing preference file ''{0}'' with default preference file.", preferenceFile.getAbsoluteFile()));
                resetToDefault();
                save();
            }
        } catch(IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(
                    Main.parent,
                    tr("<html>Failed to initialize preferences.<br>Failed to reset preference file to default: {0}</html>",getPreferenceFile().getAbsoluteFile()),
                    tr("Error"),
                    JOptionPane.ERROR_MESSAGE
            );
            return;
        }
        try {
            load();
        } catch (Exception e) {
            e.printStackTrace();
            File backupFile = new File(prefDir,"preferences.xml.bak");
            JOptionPane.showMessageDialog(
                    Main.parent,
                    tr("<html>Preferences file had errors.<br> Making backup of old one to <br>{0}<br> and creating a new default preference file.</html>", backupFile.getAbsoluteFile()),
                    tr("Error"),
                    JOptionPane.ERROR_MESSAGE
            );
            Main.platform.rename(preferenceFile, backupFile);
            try {
                resetToDefault();
                save();
            } catch(IOException e1) {
                e1.printStackTrace();
                System.err.println(tr("Warning: Failed to initialize preferences. Failed to reset preference file to default: {0}", getPreferenceFile()));
            }
        }
    }

    public final void resetToDefault(){
        properties.clear();
    }

    /**
     * Convenience method for accessing colour preferences.
     *
     * @param colName name of the colour
     * @param def default value
     * @return a Color object for the configured colour, or the default value if none configured.
     */
    synchronized public Color getColor(String colName, Color def) {
        return getColor(colName, null, def);
    }

    synchronized public Color getUIColor(String colName) {
        return UIManager.getColor(colName);
    }

    /* only for preferences */
    synchronized public String getColorName(String o) {
        try
        {
            Matcher m = Pattern.compile("mappaint\\.(.+?)\\.(.+)").matcher(o);
            m.matches();
            return tr("Paint style {0}: {1}", tr(m.group(1)), tr(m.group(2)));
        }
        catch (Exception e) {}
        try
        {
            Matcher m = Pattern.compile("layer (.+)").matcher(o);
            m.matches();
            return tr("Layer: {0}", tr(m.group(1)));
        }
        catch (Exception e) {}
        return tr(colornames.containsKey(o) ? colornames.get(o) : o);
    }

    public Color getColor(ColorKey key) {
        return getColor(key.getColorName(), key.getSpecialName(), key.getDefaultValue());
    }

    /**
     * Convenience method for accessing colour preferences.
     *
     * @param colName name of the colour
     * @param specName name of the special colour settings
     * @param def default value
     * @return a Color object for the configured colour, or the default value if none configured.
     */
    synchronized public Color getColor(String colName, String specName, Color def) {
        String colKey = ColorProperty.getColorKey(colName);
        if(!colKey.equals(colName)) {
            colornames.put(colKey, colName);
        }
        putDefault("color."+colKey, ColorHelper.color2html(def));
        String colStr = specName != null ? get("color."+specName) : "";
        if(colStr.equals("")) {
            colStr = get("color."+colKey);
        }
        return colStr.equals("") ? def : ColorHelper.html2color(colStr);
    }

    synchronized public Color getDefaultColor(String colKey) {
        String colStr = defaults.get("color."+colKey);
        return colStr == null || "".equals(colStr) ? null : ColorHelper.html2color(colStr);
    }

    synchronized public boolean putColor(String colKey, Color val) {
        return put("color."+colKey, val != null ? ColorHelper.color2html(val) : null);
    }

    synchronized public int getInteger(String key, int def) {
        putDefault(key, Integer.toString(def));
        String v = get(key);
        if(v.isEmpty())
            return def;

        try {
            return Integer.parseInt(v);
        } catch(NumberFormatException e) {
            // fall out
        }
        return def;
    }

    synchronized public int getInteger(String key, String specName, int def) {
        putDefault(key, Integer.toString(def));
        String v = get(key+"."+specName);
        if(v.isEmpty())
            v = get(key);
        if(v.isEmpty())
            return def;

        try {
            return Integer.parseInt(v);
        } catch(NumberFormatException e) {
            // fall out
        }
        return def;
    }

    synchronized public long getLong(String key, long def) {
        putDefault(key, Long.toString(def));
        String v = get(key);
        if(null == v)
            return def;

        try {
            return Long.parseLong(v);
        } catch(NumberFormatException e) {
            // fall out
        }
        return def;
    }

    synchronized public double getDouble(String key, double def) {
        putDefault(key, Double.toString(def));
        String v = get(key);
        if(null == v)
            return def;

        try {
            return Double.parseDouble(v);
        } catch(NumberFormatException e) {
            // fall out
        }
        return def;
    }

    /**
     * Get a list of values for a certain key
     * @param key the identifier for the setting
     * @param def the default value.
     * @return the corresponding value if the property has been set before,
     *  def otherwise
     */
    public Collection<String> getCollection(String key, Collection<String> def) {
        putCollectionDefault(key, def == null ? null : new ArrayList<String>(def));
        Collection<String> prop = collectionProperties.get(key);
        if (prop != null)
            return prop;
        else
            return def;
    }

    /**
     * Get a list of values for a certain key
     * @param key the identifier for the setting
     * @return the corresponding value if the property has been set before,
     *  an empty Collection otherwise.
     */
    public Collection<String> getCollection(String key) {
        putCollectionDefault(key, null);
        Collection<String> prop = collectionProperties.get(key);
        if (prop != null)
            return prop;
        else
            return Collections.emptyList();
    }

    synchronized public void removeFromCollection(String key, String value) {
        List<String> a = new ArrayList<String>(getCollection(key, Collections.<String>emptyList()));
        a.remove(value);
        putCollection(key, a);
    }

    public boolean putCollection(String key, Collection<String> value) {
        List<String> oldValue = null;
        List<String> valueCopy = null;

        synchronized (this) {
            if (value == null) {
                oldValue = collectionProperties.remove(key);
                boolean changed = oldValue != null;
                changed |= properties.remove(key) != null;
                if (!changed) return false;
            } else {
                oldValue = collectionProperties.get(key);
                if (equalCollection(value, oldValue)) return false;
                Collection<String> defValue = collectionDefaults.get(key);
                if (oldValue == null && equalCollection(value, defValue)) return false;

                valueCopy = new ArrayList<String>(value);
                if (valueCopy.contains(null)) throw new RuntimeException("Error: Null as list element in preference setting (key '"+key+"')");
                collectionProperties.put(key, Collections.unmodifiableList(valueCopy));
            }
            try {
                save();
            } catch(IOException e){
                System.out.println(tr("Warning: failed to persist preferences to ''{0}''", getPreferenceFile().getAbsoluteFile()));
            }
        }
        // Call outside of synchronized section in case some listener wait for other thread that wait for preference lock
        firePreferenceChanged(key, new ListSetting(oldValue), new ListSetting(valueCopy));
        return true;
    }

    public static boolean equalCollection(Collection<String> a, Collection<String> b) {
        if (a == null) return b == null;
        if (b == null) return false;
        if (a.size() != b.size()) return false;
        Iterator<String> itA = a.iterator();
        Iterator<String> itB = b.iterator();
        while (itA.hasNext()) {
            String aStr = itA.next();
            String bStr = itB.next();
            if (!Utils.equal(aStr,bStr)) return false;
        }
        return true;
    }

    /**
     * Saves at most {@code maxsize} items of collection {@code val}.
     */
    public boolean putCollectionBounded(String key, int maxsize, Collection<String> val) {
        Collection<String> newCollection = new ArrayList<String>(Math.min(maxsize, val.size()));
        for (String i : val) {
            if (newCollection.size() >= maxsize) {
                break;
            }
            newCollection.add(i);
        }
        return putCollection(key, newCollection);
    }

    synchronized private void putCollectionDefault(String key, List<String> val) {
        collectionDefaults.put(key, val);
    }

    /**
     * Used to read a 2-dimensional array of strings from the preference file.
     * If not a single entry could be found, def is returned.
     */
    synchronized public Collection<Collection<String>> getArray(String key, Collection<Collection<String>> def) {
        if (def != null) {
            List<List<String>> defCopy = new ArrayList<List<String>>(def.size());
            for (Collection<String> lst : def) {
                defCopy.add(Collections.unmodifiableList(new ArrayList<String>(lst)));
            }
            putArrayDefault(key, Collections.unmodifiableList(defCopy));
        } else {
            putArrayDefault(key, null);
        }
        List<List<String>> prop = arrayProperties.get(key);
        if (prop != null) {
            @SuppressWarnings("unchecked")
            Collection<Collection<String>> prop_cast = (Collection) prop;
            return prop_cast;
        } else
            return def;
    }

    public Collection<Collection<String>> getArray(String key) {
        putArrayDefault(key, null);
        List<List<String>> prop = arrayProperties.get(key);
        if (prop != null) {
            @SuppressWarnings("unchecked")
            Collection<Collection<String>> prop_cast = (Collection) prop;
            return prop_cast;
        } else
            return Collections.emptyList();
    }

    public boolean putArray(String key, Collection<Collection<String>> value) {
        boolean changed = false;

        List<List<String>> oldValue = null;
        List<List<String>> valueCopy = null;

        synchronized (this) {
            oldValue = arrayProperties.get(key);
            if (value == null) {
                if (arrayProperties.remove(key) != null) return false;
            } else {
                if (equalArray(value, oldValue)) return false;

                List<List<String>> defValue = arrayDefaults.get(key);
                if (oldValue == null && equalArray(value, defValue)) return false;

                valueCopy = new ArrayList<List<String>>(value.size());
                if (valueCopy.contains(null)) throw new RuntimeException("Error: Null as list element in preference setting (key '"+key+"')");
                for (Collection<String> lst : value) {
                    List<String> lstCopy = new ArrayList<String>(lst);
                    if (lstCopy.contains(null)) throw new RuntimeException("Error: Null as inner list element in preference setting (key '"+key+"')");
                    valueCopy.add(Collections.unmodifiableList(lstCopy));
                }
                arrayProperties.put(key, Collections.unmodifiableList(valueCopy));
            }
            try {
                save();
            } catch(IOException e){
                System.out.println(tr("Warning: failed to persist preferences to ''{0}''", getPreferenceFile().getAbsoluteFile()));
            }
        }
        // Call outside of synchronized section in case some listener wait for other thread that wait for preference lock
        firePreferenceChanged(key, new ListListSetting(oldValue), new ListListSetting(valueCopy));
        return true;
    }

    public static boolean equalArray(Collection<Collection<String>> a, Collection<List<String>> b) {
        if (a == null) return b == null;
        if (b == null) return false;
        if (a.size() != b.size()) return false;
        Iterator<Collection<String>> itA = a.iterator();
        Iterator<List<String>> itB = b.iterator();
        while (itA.hasNext()) {
            if (!equalCollection(itA.next(), itB.next())) return false;
        }
        return true;
    }

    synchronized private void putArrayDefault(String key, List<List<String>> val) {
        arrayDefaults.put(key, val);
    }

    public Collection<Map<String, String>> getListOfStructs(String key, Collection<Map<String, String>> def) {
        if (def != null) {
            List<Map<String, String>> defCopy = new ArrayList<Map<String, String>>(def.size());
            for (Map<String, String> map : def) {
                defCopy.add(Collections.unmodifiableMap(new LinkedHashMap<String,String>(map)));
            }
            putListOfStructsDefault(key, Collections.unmodifiableList(defCopy));
        } else {
            putListOfStructsDefault(key, null);
        }
        Collection<Map<String, String>> prop = listOfStructsProperties.get(key);
        if (prop != null)
            return prop;
        else
            return def;
    }

    public boolean putListOfStructs(String key, Collection<Map<String, String>> value) {
        boolean changed = false;

        List<Map<String, String>> oldValue;
        List<Map<String, String>> valueCopy = null;

        synchronized (this) {
            oldValue = listOfStructsProperties.get(key);
            if (value == null) {
                if (listOfStructsProperties.remove(key) != null) return false;
            } else {
                if (equalListOfStructs(oldValue, value)) return false;

                List<Map<String, String>> defValue = listOfStructsDefaults.get(key);
                if (oldValue == null && equalListOfStructs(value, defValue)) return false;

                valueCopy = new ArrayList<Map<String, String>>(value.size());
                if (valueCopy.contains(null)) throw new RuntimeException("Error: Null as list element in preference setting (key '"+key+"')");
                for (Map<String, String> map : value) {
                    Map<String, String> mapCopy = new LinkedHashMap<String,String>(map);
                    if (mapCopy.keySet().contains(null)) throw new RuntimeException("Error: Null as map key in preference setting (key '"+key+"')");
                    if (mapCopy.values().contains(null)) throw new RuntimeException("Error: Null as map value in preference setting (key '"+key+"')");
                    valueCopy.add(Collections.unmodifiableMap(mapCopy));
                }
                listOfStructsProperties.put(key, Collections.unmodifiableList(valueCopy));
            }
            try {
                save();
            } catch(IOException e){
                System.out.println(tr("Warning: failed to persist preferences to ''{0}''", getPreferenceFile().getAbsoluteFile()));
            }
        }
        // Call outside of synchronized section in case some listener wait for other thread that wait for preference lock
        firePreferenceChanged(key, new MapListSetting(oldValue), new MapListSetting(valueCopy));
        return true;
    }

    public static boolean equalListOfStructs(Collection<Map<String, String>> a, Collection<Map<String, String>> b) {
        if (a == null) return b == null;
        if (b == null) return false;
        if (a.size() != b.size()) return false;
        Iterator<Map<String, String>> itA = a.iterator();
        Iterator<Map<String, String>> itB = b.iterator();
        while (itA.hasNext()) {
            if (!equalMap(itA.next(), itB.next())) return false;
        }
        return true;
    }

    private static boolean equalMap(Map<String, String> a, Map<String, String> b) {
        if (a == null) return b == null;
        if (b == null) return false;
        if (a.size() != b.size()) return false;
        for (Entry<String, String> e : a.entrySet()) {
            if (!Utils.equal(e.getValue(), b.get(e.getKey()))) return false;
        }
        return true;
    }

    synchronized private void putListOfStructsDefault(String key, List<Map<String, String>> val) {
        listOfStructsDefaults.put(key, val);
    }

    @Retention(RetentionPolicy.RUNTIME) public @interface pref { }
    @Retention(RetentionPolicy.RUNTIME) public @interface writeExplicitly { }

    /**
     * Get a list of hashes which are represented by a struct-like class.
     * Possible properties are given by fields of the class klass that have
     * the @pref annotation.
     * Default constructor is used to initialize the struct objects, properties
     * then override some of these default values.
     * @param key main preference key
     * @param klass The struct class
     * @return a list of objects of type T or an empty list if nothing was found
     */
    public <T> List<T> getListOfStructs(String key, Class<T> klass) {
        List<T> r = getListOfStructs(key, null, klass);
        if (r == null)
            return Collections.emptyList();
        else
            return r;
    }

    /**
     * same as above, but returns def if nothing was found
     */
    public <T> List<T> getListOfStructs(String key, Collection<T> def, Class<T> klass) {
        Collection<Map<String,String>> prop =
            getListOfStructs(key, def == null ? null : serializeListOfStructs(def, klass));
        if (prop == null)
            return def == null ? null : new ArrayList<T>(def);
        List<T> lst = new ArrayList<T>();
        for (Map<String,String> entries : prop) {
            T struct = deserializeStruct(entries, klass);
            lst.add(struct);
        }
        return lst;
    }

    /**
     * Save a list of hashes represented by a struct-like class.
     * Considers only fields that have the @pref annotation.
     * In addition it does not write fields with null values. (Thus they are cleared)
     * Default values are given by the field values after default constructor has
     * been called.
     * Fields equal to the default value are not written unless the field has
     * the @writeExplicitly annotation.
     * @param key main preference key
     * @param val the list that is supposed to be saved
     * @param klass The struct class
     * @return true if something has changed
     */
    public <T> boolean putListOfStructs(String key, Collection<T> val, Class<T> klass) {
        return putListOfStructs(key, serializeListOfStructs(val, klass));
    }

    private <T> Collection<Map<String,String>> serializeListOfStructs(Collection<T> l, Class<T> klass) {
        if (l == null)
            return null;
        Collection<Map<String,String>> vals = new ArrayList<Map<String,String>>();
        for (T struct : l) {
            if (struct == null) {
                continue;
            }
            vals.add(serializeStruct(struct, klass));
        }
        return vals;
    }

    public static <T> Map<String,String> serializeStruct(T struct, Class<T> klass) {
        T structPrototype;
        try {
            structPrototype = klass.newInstance();
        } catch (InstantiationException ex) {
            throw new RuntimeException(ex);
        } catch (IllegalAccessException ex) {
            throw new RuntimeException(ex);
        }

        Map<String,String> hash = new LinkedHashMap<String,String>();
        for (Field f : klass.getDeclaredFields()) {
            if (f.getAnnotation(pref.class) == null) {
                continue;
            }
            f.setAccessible(true);
            try {
                Object fieldValue = f.get(struct);
                Object defaultFieldValue = f.get(structPrototype);
                if (fieldValue != null) {
                    if (f.getAnnotation(writeExplicitly.class) != null || !Utils.equal(fieldValue, defaultFieldValue)) {
                        hash.put(f.getName().replace("_", "-"), fieldValue.toString());
                    }
                }
            } catch (IllegalArgumentException ex) {
                throw new RuntimeException();
            } catch (IllegalAccessException ex) {
                throw new RuntimeException();
            }
        }
        return hash;
    }

    public static <T> T deserializeStruct(Map<String,String> hash, Class<T> klass) {
        T struct = null;
        try {
            struct = klass.newInstance();
        } catch (InstantiationException ex) {
            throw new RuntimeException();
        } catch (IllegalAccessException ex) {
            throw new RuntimeException();
        }
        for (Entry<String,String> key_value : hash.entrySet()) {
            Object value = null;
            Field f;
            try {
                f = klass.getDeclaredField(key_value.getKey().replace("-", "_"));
            } catch (NoSuchFieldException ex) {
                continue;
            } catch (SecurityException ex) {
                throw new RuntimeException();
            }
            if (f.getAnnotation(pref.class) == null) {
                continue;
            }
            f.setAccessible(true);
            if (f.getType() == Boolean.class || f.getType() == boolean.class) {
                value = Boolean.parseBoolean(key_value.getValue());
            } else if (f.getType() == Integer.class || f.getType() == int.class) {
                try {
                    value = Integer.parseInt(key_value.getValue());
                } catch (NumberFormatException nfe) {
                    continue;
                }
            } else if (f.getType() == Double.class || f.getType() == double.class) {
                try {
                    value = Double.parseDouble(key_value.getValue());
                } catch (NumberFormatException nfe) {
                    continue;
                }
            } else  if (f.getType() == String.class) {
                value = key_value.getValue();
            } else
                throw new RuntimeException("unsupported preference primitive type");

            try {
                f.set(struct, value);
            } catch (IllegalArgumentException ex) {
                throw new AssertionError();
            } catch (IllegalAccessException ex) {
                throw new RuntimeException();
            }
        }
        return struct;
    }

    public boolean putSetting(final String key, Setting value) {
        if (value == null) return false;
        class PutVisitor implements SettingVisitor {
            public boolean changed;
            public void visit(StringSetting setting) {
                changed = put(key, setting.getValue());
            }
            public void visit(ListSetting setting) {
                changed = putCollection(key, setting.getValue());
            }
            public void visit(ListListSetting setting) {
                @SuppressWarnings("unchecked")
                boolean changed = putArray(key, (Collection) setting.getValue());
                this.changed = changed;
            }
            public void visit(MapListSetting setting) {
                changed = putListOfStructs(key, setting.getValue());
            }
        };
        PutVisitor putVisitor = new PutVisitor();
        value.visit(putVisitor);
        return putVisitor.changed;
    }

    public Map<String, Setting> getAllSettings() {
        Map<String, Setting> settings = new TreeMap<String, Setting>();

        for (Entry<String, String> e : properties.entrySet()) {
            settings.put(e.getKey(), new StringSetting(e.getValue()));
        }
        for (Entry<String, List<String>> e : collectionProperties.entrySet()) {
            settings.put(e.getKey(), new ListSetting(e.getValue()));
        }
        for (Entry<String, List<List<String>>> e : arrayProperties.entrySet()) {
            settings.put(e.getKey(), new ListListSetting(e.getValue()));
        }
        for (Entry<String, List<Map<String, String>>> e : listOfStructsProperties.entrySet()) {
            settings.put(e.getKey(), new MapListSetting(e.getValue()));
        }
        return settings;
    }

    public Map<String, Setting> getAllDefaults() {
        Map<String, Setting> allDefaults = new TreeMap<String, Setting>();

        for (Entry<String, String> e : defaults.entrySet()) {
            allDefaults.put(e.getKey(), new StringSetting(e.getValue()));
        }
        for (Entry<String, List<String>> e : collectionDefaults.entrySet()) {
            allDefaults.put(e.getKey(), new ListSetting(e.getValue()));
        }
        for (Entry<String, List<List<String>>> e : arrayDefaults.entrySet()) {
            allDefaults.put(e.getKey(), new ListListSetting(e.getValue()));
        }
        for (Entry<String, List<Map<String, String>>> e : listOfStructsDefaults.entrySet()) {
            allDefaults.put(e.getKey(), new MapListSetting(e.getValue()));
        }
        return allDefaults;
    }

    /**
     * Updates system properties with the current values in the preferences.
     *
     */
    public void updateSystemProperties() {
        if(getBoolean("prefer.ipv6", true)) {
            // never set this to false, only true!
            updateSystemProperty("java.net.preferIPv6Addresses", "true");
        }
        updateSystemProperty("http.agent", Version.getInstance().getAgentString());
        updateSystemProperty("user.language", get("language"));
        // Workaround to fix a Java bug.
        // Force AWT toolkit to update its internal preferences (fix #3645).
        // This ugly hack comes from Sun bug database: http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6292739
        try {
            Field field = Toolkit.class.getDeclaredField("resources");
            field.setAccessible(true);
            field.set(null, ResourceBundle.getBundle("sun.awt.resources.awt"));
        } catch (Exception e) {
            // Ignore all exceptions
        }
    }
    
    private void updateSystemProperty(String key, String value) {
        if (value != null) {
            System.setProperty(key, value);
        }
    }

    /**
     * The default plugin site
     */
    private final static String[] DEFAULT_PLUGIN_SITE = {
    "http://josm.openstreetmap.de/plugin%<?plugins=>"};

    /**
     * Replies the collection of plugin site URLs from where plugin lists can be downloaded
     */
    public Collection<String> getPluginSites() {
        return getCollection("pluginmanager.sites", Arrays.asList(DEFAULT_PLUGIN_SITE));
    }

    /**
     * Sets the collection of plugin site URLs.
     *
     * @param sites the site URLs
     */
    public void setPluginSites(Collection<String> sites) {
        putCollection("pluginmanager.sites", sites);
    }

    protected XMLStreamReader parser;

    public void validateXML(Reader in) throws Exception {
        SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        Schema schema = factory.newSchema(new StreamSource(new MirroredInputStream("resource://data/preferences.xsd")));
        Validator validator = schema.newValidator();
        validator.validate(new StreamSource(in));
    }

    public void fromXML(Reader in) throws XMLStreamException {
        XMLStreamReader parser = XMLInputFactory.newInstance().createXMLStreamReader(in);
        this.parser = parser;
        parse();
    }

    public void parse() throws XMLStreamException {
        int event = parser.getEventType();
        while (true) {
            if (event == XMLStreamConstants.START_ELEMENT) {
                parseRoot();
            } else if (event == XMLStreamConstants.END_ELEMENT) {
                return;
            }
            if (parser.hasNext()) {
                event = parser.next();
            } else {
                break;
            }
        }
        parser.close();
    }

    public void parseRoot() throws XMLStreamException {
        while (true) {
            int event = parser.next();
            if (event == XMLStreamConstants.START_ELEMENT) {
                if (parser.getLocalName().equals("tag")) {
                    properties.put(parser.getAttributeValue(null, "key"), parser.getAttributeValue(null, "value"));
                    jumpToEnd();
                } else if (parser.getLocalName().equals("list") ||
                        parser.getLocalName().equals("collection") ||
                        parser.getLocalName().equals("lists") ||
                        parser.getLocalName().equals("maps")
                ) {
                    parseToplevelList();
                } else {
                    throwException("Unexpected element: "+parser.getLocalName());
                }
            } else if (event == XMLStreamConstants.END_ELEMENT) {
                return;
            }
        }
    }

    private void jumpToEnd() throws XMLStreamException {
        while (true) {
            int event = parser.next();
            if (event == XMLStreamConstants.START_ELEMENT) {
                jumpToEnd();
            } else if (event == XMLStreamConstants.END_ELEMENT) {
                return;
            }
        }
    }

    protected void parseToplevelList() throws XMLStreamException {
        String key = parser.getAttributeValue(null, "key");
        String name = parser.getLocalName();

        List<String> entries = null;
        List<List<String>> lists = null;
        List<Map<String, String>> maps = null;
        while (true) {
            int event = parser.next();
            if (event == XMLStreamConstants.START_ELEMENT) {
                if (parser.getLocalName().equals("entry")) {
                    if (entries == null) {
                        entries = new ArrayList<String>();
                    }
                    entries.add(parser.getAttributeValue(null, "value"));
                    jumpToEnd();
                } else if (parser.getLocalName().equals("list")) {
                    if (lists == null) {
                        lists = new ArrayList<List<String>>();
                    }
                    lists.add(parseInnerList());
                } else if (parser.getLocalName().equals("map")) {
                    if (maps == null) {
                        maps = new ArrayList<Map<String, String>>();
                    }
                    maps.add(parseMap());
                } else {
                    throwException("Unexpected element: "+parser.getLocalName());
                }
            } else if (event == XMLStreamConstants.END_ELEMENT) {
                break;
            }
        }
        if (entries != null) {
            collectionProperties.put(key, Collections.unmodifiableList(entries));
        } else if (lists != null) {
            arrayProperties.put(key, Collections.unmodifiableList(lists));
        } else if (maps != null) {
            listOfStructsProperties.put(key, Collections.unmodifiableList(maps));
        } else {
            if (name.equals("lists")) {
                arrayProperties.put(key, Collections.<List<String>>emptyList());
            } else if (name.equals("maps")) {
                listOfStructsProperties.put(key, Collections.<Map<String, String>>emptyList());
            } else {
                collectionProperties.put(key, Collections.<String>emptyList());
            }
        }
    }

    protected List<String> parseInnerList() throws XMLStreamException {
        List<String> entries = new ArrayList<String>();
        while (true) {
            int event = parser.next();
            if (event == XMLStreamConstants.START_ELEMENT) {
                if (parser.getLocalName().equals("entry")) {
                    entries.add(parser.getAttributeValue(null, "value"));
                    jumpToEnd();
                } else {
                    throwException("Unexpected element: "+parser.getLocalName());
                }
            } else if (event == XMLStreamConstants.END_ELEMENT) {
                break;
            }
        }
        return Collections.unmodifiableList(entries);
    }

    protected Map<String, String> parseMap() throws XMLStreamException {
        Map<String, String> map = new LinkedHashMap<String, String>();
        while (true) {
            int event = parser.next();
            if (event == XMLStreamConstants.START_ELEMENT) {
                if (parser.getLocalName().equals("tag")) {
                    map.put(parser.getAttributeValue(null, "key"), parser.getAttributeValue(null, "value"));
                    jumpToEnd();
                } else {
                    throwException("Unexpected element: "+parser.getLocalName());
                }
            } else if (event == XMLStreamConstants.END_ELEMENT) {
                break;
            }
        }
        return Collections.unmodifiableMap(map);
    }

    protected void throwException(String msg) {
        throw new RuntimeException(msg + tr(" (at line {0}, column {1})", parser.getLocation().getLineNumber(), parser.getLocation().getColumnNumber()));
    }

    private class SettingToXml implements SettingVisitor {
        private StringBuilder b;
        private boolean noPassword;
        private String key;

        public SettingToXml(StringBuilder b, boolean noPassword) {
            this.b = b;
            this.noPassword = noPassword;
        }

        public void setKey(String key) {
            this.key = key;
        }

        public void visit(StringSetting setting) {
            if (noPassword && key.equals("osm-server.password"))
                return; // do not store plain password.
            String r = setting.getValue();
            String s = defaults.get(key);
            /* don't save default values */
            if(s == null || !s.equals(r)) {
                b.append("  <tag key='");
                b.append(XmlWriter.encode(key));
                b.append("' value='");
                b.append(XmlWriter.encode(setting.getValue()));
                b.append("'/>\n");
            }
        }

        public void visit(ListSetting setting) {
            b.append("  <list key='").append(XmlWriter.encode(key)).append("'>\n");
            for (String s : setting.getValue()) {
                b.append("    <entry value='").append(XmlWriter.encode(s)).append("'/>\n");
            }
            b.append("  </list>\n");
        }

        public void visit(ListListSetting setting) {
            b.append("  <lists key='").append(XmlWriter.encode(key)).append("'>\n");
            for (List<String> list : setting.getValue()) {
                b.append("    <list>\n");
                for (String s : list) {
                    b.append("      <entry value='").append(XmlWriter.encode(s)).append("'/>\n");
                }
                b.append("    </list>\n");
            }
            b.append("  </lists>\n");
        }

        public void visit(MapListSetting setting) {
            b.append("  <maps key='").append(XmlWriter.encode(key)).append("'>\n");
            for (Map<String, String> struct : setting.getValue()) {
                b.append("    <map>\n");
                for (Entry<String, String> e : struct.entrySet()) {
                    b.append("      <tag key='").append(XmlWriter.encode(e.getKey())).append("' value='").append(XmlWriter.encode(e.getValue())).append("'/>\n");
                }
                b.append("    </map>\n");
            }
            b.append("  </maps>\n");
        }
    }

    public String toXML(boolean nopass) {
        StringBuilder b = new StringBuilder(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<preferences xmlns=\"http://josm.openstreetmap.de/preferences-1.0\" version=\""+
                Version.getInstance().getVersion() + "\">\n");
        SettingToXml toXml = new SettingToXml(b, nopass);
        Map<String, Setting> settings = new TreeMap<String, Setting>();

        for (Entry<String, String> e : properties.entrySet()) {
            settings.put(e.getKey(), new StringSetting(e.getValue()));
        }
        for (Entry<String, List<String>> e : collectionProperties.entrySet()) {
            settings.put(e.getKey(), new ListSetting(e.getValue()));
        }
        for (Entry<String, List<List<String>>> e : arrayProperties.entrySet()) {
            settings.put(e.getKey(), new ListListSetting(e.getValue()));
        }
        for (Entry<String, List<Map<String, String>>> e : listOfStructsProperties.entrySet()) {
            settings.put(e.getKey(), new MapListSetting(e.getValue()));
        }
        for (Entry<String, Setting> e : settings.entrySet()) {
            toXml.setKey(e.getKey());
            e.getValue().visit(toXml);
        }
        b.append("</preferences>\n");
        return b.toString();
    }

    /**
     * Removes obsolete preference settings. If you throw out a once-used preference
     * setting, add it to the list here with an expiry date (written as comment). If you
     * see something with an expiry date in the past, remove it from the list.
     */
    public void removeObsolete() {
        String[] obsolete = {
                "color.Imagery fade",              // 08/2012 - wrong property caused by #6723, can be removed mid-2013
        };
        for (String key : obsolete) {
            boolean removed = false;
            if(properties.containsKey(key)) { properties.remove(key); removed = true; }
            if(collectionProperties.containsKey(key)) { collectionProperties.remove(key); removed = true; }
            if(arrayProperties.containsKey(key)) { arrayProperties.remove(key); removed = true; }
            if(listOfStructsProperties.containsKey(key)) { listOfStructsProperties.remove(key); removed = true; }
            if(removed)
                System.out.println(tr("Preference setting {0} has been removed since it is no longer used.", key));
        }
    }

    public static boolean isEqual(Setting a, Setting b) {
        if (a==null && b==null) return true;
        if (a==null) return false;
        if (b==null) return false;
        if (a==b) return true;
        
        if (a instanceof StringSetting) 
            return (a.getValue().equals(b.getValue()));
        if (a instanceof ListSetting) {
            @SuppressWarnings("unchecked") Collection<String> aValue = (Collection) a.getValue();
            @SuppressWarnings("unchecked") Collection<String> bValue = (Collection) b.getValue();
            return equalCollection(aValue, bValue);
        }
        if (a instanceof ListListSetting) {
            @SuppressWarnings("unchecked") Collection<Collection<String>> aValue = (Collection) a.getValue();
            @SuppressWarnings("unchecked") Collection<List<String>> bValue = (Collection) b.getValue();
            return equalArray(aValue, bValue);
        }
        if (a instanceof MapListSetting) {
            @SuppressWarnings("unchecked") Collection<Map<String, String>> aValue = (Collection) a.getValue();
            @SuppressWarnings("unchecked") Collection<Map<String, String>> bValue = (Collection) b.getValue();
            return equalListOfStructs(aValue, bValue);
        }
        return a.equals(b);
    }

}
