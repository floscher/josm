// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.tagging;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import javax.swing.JOptionPane;

import org.xml.sax.SAXException;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.gui.preferences.SourceEntry;
import org.openstreetmap.josm.gui.preferences.map.TaggingPresetPreference;
import org.openstreetmap.josm.io.MirroredInputStream;
import org.openstreetmap.josm.tools.Utils;
import org.openstreetmap.josm.tools.XmlObjectParser;

import static org.openstreetmap.josm.tools.I18n.tr;

/**
 * The tagging presets reader.
 * @since 6068
 */
public final class TaggingPresetReader {

    /**
     * Constructs a new {@code TaggingPresetReader}.
     */
    public TaggingPresetReader() {
    }
    
    private static File zipIcons = null;
    
    public static LinkedList<String> getPresetSources() {
        LinkedList<String> sources = new LinkedList<String>();

        for (SourceEntry e : (new TaggingPresetPreference.PresetPrefHelper()).get()) {
            sources.add(e.url);
        }

        return sources;
    }
    
    public static List<TaggingPreset> readAll(Reader in, boolean validate) throws SAXException {
        XmlObjectParser parser = new XmlObjectParser();
        parser.mapOnStart("item", TaggingPreset.class);
        parser.mapOnStart("separator", TaggingPresetSeparator.class);
        parser.mapBoth("group", TaggingPresetMenu.class);
        parser.map("text", TaggingPresetItems.Text.class);
        parser.map("link", TaggingPresetItems.Link.class);
        parser.mapOnStart("optional", TaggingPresetItems.Optional.class);
        parser.mapOnStart("roles", TaggingPresetItems.Roles.class);
        parser.map("role", TaggingPresetItems.Role.class);
        parser.map("checkgroup", TaggingPresetItems.CheckGroup.class);
        parser.map("check", TaggingPresetItems.Check.class);
        parser.map("combo", TaggingPresetItems.Combo.class);
        parser.map("multiselect", TaggingPresetItems.MultiSelect.class);
        parser.map("label", TaggingPresetItems.Label.class);
        parser.map("space", TaggingPresetItems.Space.class);
        parser.map("key", TaggingPresetItems.Key.class);
        parser.map("list_entry", TaggingPresetItems.PresetListEntry.class);
        
        LinkedList<TaggingPreset> all = new LinkedList<TaggingPreset>();
        TaggingPresetMenu lastmenu = null;
        TaggingPresetItems.Roles lastrole = null;
        final List<TaggingPresetItems.Check> checks = new LinkedList<TaggingPresetItems.Check>();
        List<TaggingPresetItems.PresetListEntry> listEntries = new LinkedList<TaggingPresetItems.PresetListEntry>();

        if (validate) {
            parser.startWithValidation(in, "http://josm.openstreetmap.de/tagging-preset-1.0", "resource://data/tagging-preset.xsd");
        } else {
            parser.start(in);
        }
        while (parser.hasNext()) {
            Object o = parser.next();
            if (o instanceof TaggingPresetMenu) {
                TaggingPresetMenu tp = (TaggingPresetMenu) o;
                if (tp == lastmenu) {
                    lastmenu = tp.group;
                } else {
                    tp.group = lastmenu;
                    tp.setDisplayName();
                    lastmenu = tp;
                    all.add(tp);
                }
                lastrole = null;
            } else if (o instanceof TaggingPresetSeparator) {
                TaggingPresetSeparator tp = (TaggingPresetSeparator) o;
                tp.group = lastmenu;
                all.add(tp);
                lastrole = null;
            } else if (o instanceof TaggingPreset) {
                TaggingPreset tp = (TaggingPreset) o;
                tp.group = lastmenu;
                tp.setDisplayName();
                all.add(tp);
                lastrole = null;
            } else {
                if (!all.isEmpty()) {
                    if (o instanceof TaggingPresetItems.Roles) {
                        all.getLast().data.add((TaggingPresetItem) o);
                        if (all.getLast().roles != null) {
                            throw new SAXException(tr("Roles cannot appear more than once"));
                        }
                        all.getLast().roles = (TaggingPresetItems.Roles) o;
                        lastrole = (TaggingPresetItems.Roles) o;
                    } else if (o instanceof TaggingPresetItems.Role) {
                        if (lastrole == null)
                            throw new SAXException(tr("Preset role element without parent"));
                        lastrole.roles.add((TaggingPresetItems.Role) o);
                    } else if (o instanceof TaggingPresetItems.Check) {
                        checks.add((TaggingPresetItems.Check) o);
                    } else if (o instanceof TaggingPresetItems.PresetListEntry) {
                        listEntries.add((TaggingPresetItems.PresetListEntry) o);
                    } else if (o instanceof TaggingPresetItems.CheckGroup) {
                        all.getLast().data.add((TaggingPresetItem) o);
                        ((TaggingPresetItems.CheckGroup) o).checks.addAll(checks);
                        checks.clear();
                    } else {
                        if (!checks.isEmpty()) {
                            all.getLast().data.addAll(checks);
                            checks.clear();
                        }
                        all.getLast().data.add((TaggingPresetItem) o);
                        if (o instanceof TaggingPresetItems.ComboMultiSelect) {
                            ((TaggingPresetItems.ComboMultiSelect) o).addListEntries(listEntries);
                        } else if (o instanceof TaggingPresetItems.Key) {
                            if (((TaggingPresetItems.Key) o).value == null) {
                                ((TaggingPresetItems.Key) o).value = ""; // Fix #8530
                            }
                        }
                        listEntries = new LinkedList<TaggingPresetItems.PresetListEntry>();
                        lastrole = null;
                    }
                } else
                    throw new SAXException(tr("Preset sub element without parent"));
            }
        }
        return all;
    }
    
    public static Collection<TaggingPreset> readAll(String source, boolean validate) throws SAXException, IOException {
        Collection<TaggingPreset> tp;
        MirroredInputStream s = new MirroredInputStream(source);
        try {
            InputStream zip = s.getZipEntry("xml","preset");
            if(zip != null) {
                zipIcons = s.getFile();
            }
            InputStreamReader r;
            try {
                r = new InputStreamReader(zip == null ? s : zip, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                r = new InputStreamReader(zip == null ? s: zip);
            }
            try {
                tp = readAll(new BufferedReader(r), validate);
            } finally {
                Utils.close(r);
            }
        } finally {
            Utils.close(s);
        }
        return tp;
    }

    public static Collection<TaggingPreset> readAll(Collection<String> sources, boolean validate) {
        LinkedList<TaggingPreset> allPresets = new LinkedList<TaggingPreset>();
        for(String source : sources)  {
            try {
                allPresets.addAll(readAll(source, validate));
            } catch (IOException e) {
                System.err.println(e.getClass().getName()+": "+e.getMessage());
                System.err.println(source);
                JOptionPane.showMessageDialog(
                        Main.parent,
                        tr("Could not read tagging preset source: {0}",source),
                        tr("Error"),
                        JOptionPane.ERROR_MESSAGE
                        );
            } catch (SAXException e) {
                System.err.println(e.getClass().getName()+": "+e.getMessage());
                System.err.println(source);
                JOptionPane.showMessageDialog(
                        Main.parent,
                        tr("Error parsing {0}: ", source)+e.getMessage(),
                        tr("Error"),
                        JOptionPane.ERROR_MESSAGE
                        );
            }
        }
        return allPresets;
    }
    
    public static Collection<TaggingPreset> readFromPreferences(boolean validate) {
        return readAll(getPresetSources(), validate);
    }
    
    public static File getZipIcons() {
        return zipIcons;
    }
}
