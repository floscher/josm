// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.gui.tagging;

import static org.openstreetmap.josm.tools.I18n.marktr;
import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trn;

import java.awt.Component;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.command.ChangePropertyCommand;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.command.SequenceCommand;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.OsmUtils;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.ExtendedDialog;
import org.openstreetmap.josm.gui.QuadStateCheckBox;
import org.openstreetmap.josm.io.MirroredInputStream;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.UrlLabel;
import org.openstreetmap.josm.tools.XmlObjectParser;
import org.xml.sax.SAXException;

/**
 * This class read encapsulate one tagging preset. A class method can
 * read in all predefined presets, either shipped with JOSM or that are
 * in the config directory.
 *
 * It is also able to construct dialogs out of preset definitions.
 */
public class TaggingPreset extends AbstractAction {

    public TaggingPresetMenu group = null;
    public String name;

    public static abstract class Item {
        public boolean focus = false;
        abstract void addToPanel(JPanel p, Collection<OsmPrimitive> sel);
        abstract void addCommands(Collection<OsmPrimitive> sel, List<Command> cmds);
        boolean requestFocusInWindow() {return false;}
    }

    public static class Usage {
        Set<String> values;
        Boolean hadKeys = false;
        Boolean hadEmpty = false;
        public Boolean allSimilar()
        {
            return values.size() == 1 && !hadEmpty;
        }
        public Boolean unused()
        {
            return values.size() == 0;
        }
        public String getFirst()
        {
            return (String)(values.toArray()[0]);
        }
        public Boolean hadKeys()
        {
            return hadKeys;
        }
    }

    public static final String DIFFERENT = tr("<different>");

    static Usage determineTextUsage(Collection<OsmPrimitive> sel, String key) {
        Usage returnValue = new Usage();
        returnValue.values = new HashSet<String>();
        for (OsmPrimitive s : sel) {
            String v = s.get(key);
            if (v != null)
                returnValue.values.add(v);
            else
                returnValue.hadEmpty = true;
            if(s.keys != null && s.keys.size() > 0)
                returnValue.hadKeys = true;
        }
        return returnValue;
    }

    static Usage determineBooleanUsage(Collection<OsmPrimitive> sel, String key) {

        Usage returnValue = new Usage();
        returnValue.values = new HashSet<String>();
        for (OsmPrimitive s : sel) {
            returnValue.values.add(OsmUtils.getNamedOsmBoolean(s.get(key)));
        }
        return returnValue;
    }

    public static class Text extends Item {

        public String key;
        public String text;
        public String locale_text;
        public String default_;
        public String originalValue;
        public boolean use_last_as_default = false;
        public boolean delete_if_empty = false;

        private JComponent value;

        @Override public void addToPanel(JPanel p, Collection<OsmPrimitive> sel) {

            // find out if our key is already used in the selection.
            Usage usage = determineTextUsage(sel, key);
            if (usage.unused())
            {
                value = new JTextField();
                if (use_last_as_default && lastValue.containsKey(key)) {
                    ((JTextField)value).setText(lastValue.get(key));
                } else {
                    ((JTextField)value).setText(default_);
                }
                originalValue = null;
            } else if (usage.allSimilar()) {
                // all objects use the same value
                value = new JTextField();
                for (String s : usage.values) ((JTextField) value).setText(s);
                originalValue = ((JTextField)value).getText();
            } else {
                // the objects have different values
                value = new JComboBox(usage.values.toArray());
                ((JComboBox)value).setEditable(true);
                ((JComboBox)value).getEditor().setItem(DIFFERENT);
                originalValue = DIFFERENT;
            }
            if(locale_text == null)
                locale_text = tr(text);
            p.add(new JLabel(locale_text+":"), GBC.std().insets(0,0,10,0));
            p.add(value, GBC.eol().fill(GBC.HORIZONTAL));
        }

        @Override public void addCommands(Collection<OsmPrimitive> sel, List<Command> cmds) {

            // return if unchanged
            String v = (value instanceof JComboBox) ?
                ((JComboBox)value).getEditor().getItem().toString() :
                ((JTextField)value).getText();

            if (use_last_as_default) lastValue.put(key, v);
            if (v.equals(originalValue) || (originalValue == null && v.length() == 0)) return;

            if (delete_if_empty && v.length() == 0)
                v = null;
            cmds.add(new ChangePropertyCommand(sel, key, v));
        }
        @Override boolean requestFocusInWindow() {return value.requestFocusInWindow();}
    }

    public static class Check extends Item {

        public String key;
        public String text;
        public String locale_text;
        public boolean default_ = false; // only used for tagless objects
        public boolean use_last_as_default = false;

        private QuadStateCheckBox check;
        private QuadStateCheckBox.State initialState;
        private boolean def;

        @Override public void addToPanel(JPanel p, Collection<OsmPrimitive> sel) {

            // find out if our key is already used in the selection.
            Usage usage = determineBooleanUsage(sel, key);
            def = default_;

            if(locale_text == null)
                locale_text = tr(text);

            String oneValue = null;
            for (String s : usage.values) oneValue = s;
            if (usage.values.size() < 2 && (oneValue == null || OsmUtils.trueval.equals(oneValue) || OsmUtils.falseval.equals(oneValue))) {
                if(def)
                {
                    for (OsmPrimitive s : sel)
                        if(s.keys != null && s.keys.size() > 0) def = false;
                }

                // all selected objects share the same value which is either true or false or unset,
                // we can display a standard check box.
                initialState = OsmUtils.trueval.equals(oneValue) ?
                            QuadStateCheckBox.State.SELECTED :
                            OsmUtils.falseval.equals(oneValue) ?
                            QuadStateCheckBox.State.NOT_SELECTED :
                            def ? QuadStateCheckBox.State.SELECTED
                            : QuadStateCheckBox.State.UNSET;
                check = new QuadStateCheckBox(locale_text, initialState,
                        new QuadStateCheckBox.State[] {
                        QuadStateCheckBox.State.SELECTED,
                        QuadStateCheckBox.State.NOT_SELECTED,
                        QuadStateCheckBox.State.UNSET });
            } else {
                def = false;
                // the objects have different values, or one or more objects have something
                // else than true/false. we display a quad-state check box
                // in "partial" state.
                initialState = QuadStateCheckBox.State.PARTIAL;
                check = new QuadStateCheckBox(locale_text, QuadStateCheckBox.State.PARTIAL,
                        new QuadStateCheckBox.State[] {
                        QuadStateCheckBox.State.PARTIAL,
                        QuadStateCheckBox.State.SELECTED,
                        QuadStateCheckBox.State.NOT_SELECTED,
                        QuadStateCheckBox.State.UNSET });
            }
            p.add(check, GBC.eol().fill(GBC.HORIZONTAL));
        }

        @Override public void addCommands(Collection<OsmPrimitive> sel, List<Command> cmds) {
            // if the user hasn't changed anything, don't create a command.
            if (check.getState() == initialState && !def) return;

            // otherwise change things according to the selected value.
            cmds.add(new ChangePropertyCommand(sel, key,
                    check.getState() == QuadStateCheckBox.State.SELECTED ? OsmUtils.trueval :
                    check.getState() == QuadStateCheckBox.State.NOT_SELECTED ? OsmUtils.falseval :
                    null));
        }
        @Override boolean requestFocusInWindow() {return check.requestFocusInWindow();}
    }

    public static class Combo extends Item {

        public String key;
        public String text;
        public String locale_text;
        public String values;
        public String display_values;
        public String locale_display_values;
        public String default_;
        public boolean delete_if_empty = false;
        public boolean editable = true;
        public boolean use_last_as_default = false;

        private JComboBox combo;
        private LinkedHashMap<String,String> lhm;
        private Usage usage;
        private String originalValue;

        @Override public void addToPanel(JPanel p, Collection<OsmPrimitive> sel) {

            // find out if our key is already used in the selection.
            usage = determineTextUsage(sel, key);

            String[] value_array = values.split(",");
            String[] display_array;
            if(locale_display_values != null)
                display_array = locale_display_values.split(",");
            else if(display_values != null)
                display_array = display_values.split(",");
            else
                display_array = value_array;

            lhm = new LinkedHashMap<String,String>();
            if (!usage.allSimilar() && !usage.unused())
            {
                lhm.put(DIFFERENT, DIFFERENT);
            }
            for (int i=0; i<value_array.length; i++) {
                lhm.put(value_array[i],
                (locale_display_values == null) ?
                tr(display_array[i]) : display_array[i]);
            }
            if(!usage.unused())
            {
                for (String s : usage.values) {
                    if (!lhm.containsKey(s)) lhm.put(s, s);
                }
            }
            if (default_ != null && !lhm.containsKey(default_)) lhm.put(default_, default_);
            if(!lhm.containsKey("")) lhm.put("", "");

            combo = new JComboBox(lhm.values().toArray());
            combo.setEditable(editable);
            if (usage.allSimilar() && !usage.unused())
            {
                originalValue=usage.getFirst();
                combo.setSelectedItem(lhm.get(originalValue));
            }
            // use default only in case it is a totally new entry
            else if(default_ != null && !usage.hadKeys())
            {
                combo.setSelectedItem(default_);
                originalValue=DIFFERENT;
            }
            else if(usage.unused())
            {
                combo.setSelectedItem("");
                originalValue="";
            }
            else
            {
                combo.setSelectedItem(DIFFERENT);
                originalValue=DIFFERENT;
            }

            if(locale_text == null)
                locale_text = tr(text);
            p.add(new JLabel(locale_text+":"), GBC.std().insets(0,0,10,0));
            p.add(combo, GBC.eol().fill(GBC.HORIZONTAL));
        }
        @Override public void addCommands(Collection<OsmPrimitive> sel, List<Command> cmds) {
            Object obj = combo.getSelectedItem();
            String display = (obj == null) ? null : obj.toString();
            String value = null;
            if(display == null && combo.isEditable())
                display = combo.getEditor().getItem().toString();

            if (display != null)
            {
                for (String key : lhm.keySet()) {
                    String k = lhm.get(key);
                    if (k != null && k.equals(display)) value=key;
                }
                if(value == null)
                    value = display;
            }
            else
                value = "";

            // no change if same as before
            if (value.equals(originalValue) || (originalValue == null && (value == null || value.length() == 0))) return;

            if (delete_if_empty && value != null && value.length() == 0)
                value = null;
            cmds.add(new ChangePropertyCommand(sel, key, value));
        }
        @Override boolean requestFocusInWindow() {return combo.requestFocusInWindow();}
    }

    public static class Label extends Item {
        public String text;
        public String locale_text;

        @Override public void addToPanel(JPanel p, Collection<OsmPrimitive> sel) {
            if(locale_text == null)
                locale_text = tr(text);
            p.add(new JLabel(locale_text), GBC.eol());
        }
        @Override public void addCommands(Collection<OsmPrimitive> sel, List<Command> cmds) {}
    }

    public static class Link extends Item {
        public String href;
        public String text;
        public String locale_text;
        public String locale_href;

        @Override public void addToPanel(JPanel p, Collection<OsmPrimitive> sel) {
            if(locale_text == null)
                locale_text = text == null ? tr("More information about this feature") : tr(text);
            String url = locale_href;
            if (url == null) {
                url = href;
            }
            if (url != null) {
                p.add(new UrlLabel(url, locale_text), GBC.eol().anchor(GBC.WEST));
            }
        }
        @Override public void addCommands(Collection<OsmPrimitive> sel, List<Command> cmds) {}
    }

    public static class Optional extends Item {
        // TODO: Draw a box around optional stuff
        @Override public void addToPanel(JPanel p, Collection<OsmPrimitive> sel) {
            p.add(new JLabel(" "), GBC.eol()); // space
            p.add(new JLabel(tr("Optional Attributes:")), GBC.eol());
            p.add(new JLabel(" "), GBC.eol()); // space
        }
        @Override public void addCommands(Collection<OsmPrimitive> sel, List<Command> cmds) {}
    }

    public static class Space extends Item {
        @Override public void addToPanel(JPanel p, Collection<OsmPrimitive> sel) {
            p.add(new JLabel(" "), GBC.eol()); // space
        }
        @Override public void addCommands(Collection<OsmPrimitive> sel, List<Command> cmds) {}
    }

    public static class Key extends Item {
        public String key;
        public String value;

        @Override public void addToPanel(JPanel p, Collection<OsmPrimitive> sel) { }
        @Override public void addCommands(Collection<OsmPrimitive> sel, List<Command> cmds) {
            cmds.add(new ChangePropertyCommand(sel, key, value != null && !value.equals("") ? value : null));
        }
    }

    /**
     * The types as preparsed collection.
     */
    public List<String> types;
    public List<Item> data = new LinkedList<Item>();
    private static HashMap<String,String> lastValue = new HashMap<String,String>();

    /**
     * Create an empty tagging preset. This will not have any items and
     * will be an empty string as text. createPanel will return null.
     * Use this as default item for "do not select anything".
     */
    public TaggingPreset() {}

    /**
     * Change the display name without changing the toolbar value.
     */
    public void setDisplayName() {
        putValue(Action.NAME, getName());
        putValue("toolbar", "tagging_" + getRawName());
        putValue(SHORT_DESCRIPTION, (group != null ?
        tr("Use preset ''{0}'' of group ''{1}''", tr(name), group.getName()) :
        tr("Use preset ''{0}''", tr(name))));
    }

    public String getName() {
        return group != null ? group.getName() + "/" + tr(name) : tr(name);
    }
    public String getRawName() {
        return group != null ? group.getRawName() + "/" + name : name;
    }
    /**
     * Called from the XML parser to set the icon
     *
     * FIXME for Java 1.6 - use 24x24 icons for LARGE_ICON_KEY (button bar)
     * and the 16x16 icons for SMALL_ICON.
     */
    public void setIcon(String iconName) {
        Collection<String> s = Main.pref.getCollection("taggingpreset.iconpaths", null);
        ImageIcon icon = ImageProvider.getIfAvailable(s, "presets", null, iconName);
        if (icon == null)
        {
            System.out.println("Could not get presets icon " + iconName);
            icon = new ImageIcon(iconName);
        }
        if (Math.max(icon.getIconHeight(), icon.getIconWidth()) != 16)
            icon = new ImageIcon(icon.getImage().getScaledInstance(16, 16, Image.SCALE_SMOOTH));
        putValue(Action.SMALL_ICON, icon);
    }

    /**
     * Called from the XML parser to set the types this preset affects
     */
    private static Collection<String> allowedtypes = Arrays.asList(new String[]
    {marktr("way"), marktr("node"), marktr("relation"), marktr("closedway")});
    public void setType(String types) throws SAXException {
        this.types = Arrays.asList(types.split(","));
        for (String type : this.types) {
            if(!allowedtypes.contains(type))
                throw new SAXException(tr("Unknown type: {0}", type));
        }
    }

    public static List<TaggingPreset> readAll(Reader in) throws SAXException {
        XmlObjectParser parser = new XmlObjectParser();
        parser.mapOnStart("item", TaggingPreset.class);
        parser.mapOnStart("separator", TaggingPresetSeparator.class);
        parser.mapBoth("group", TaggingPresetMenu.class);
        parser.map("text", Text.class);
        parser.map("link", Link.class);
        parser.mapOnStart("optional", Optional.class);
        parser.map("check", Check.class);
        parser.map("combo", Combo.class);
        parser.map("label", Label.class);
        parser.map("space", Space.class);
        parser.map("key", Key.class);
        LinkedList<TaggingPreset> all = new LinkedList<TaggingPreset>();
        TaggingPresetMenu lastmenu = null;
        parser.start(in);
        while(parser.hasNext()) {
            Object o = parser.next();
            if (o instanceof TaggingPresetMenu) {
                TaggingPresetMenu tp = (TaggingPresetMenu) o;
                if(tp == lastmenu)
                    lastmenu = tp.group;
                else
                {
                    tp.setDisplayName();
                    tp.group = lastmenu;
                    lastmenu = tp;
                    all.add(tp);
                    Main.toolbar.register(tp);

                }
            } else if (o instanceof TaggingPresetSeparator) {
                TaggingPresetSeparator tp = (TaggingPresetSeparator) o;
                tp.group = lastmenu;
                all.add(tp);
            } else if (o instanceof TaggingPreset) {
                TaggingPreset tp = (TaggingPreset) o;
                tp.group = lastmenu;
                tp.setDisplayName();
                all.add(tp);
                Main.toolbar.register(tp);
            } else
                all.getLast().data.add((Item)o);
        }
        return all;
    }

    public static Collection<TaggingPreset> readFromPreferences() {
        LinkedList<TaggingPreset> allPresets = new LinkedList<TaggingPreset>();
        String allTaggingPresets = Main.pref.get("taggingpreset.sources");

        if (Main.pref.getBoolean("taggingpreset.enable-defaults", true))
        {
            allTaggingPresets = "resource://presets/presets.xml"
            + (allTaggingPresets != null ? ";"+allTaggingPresets : "");
        }

        for(String source : allTaggingPresets.split(";"))
        {
            try {
                MirroredInputStream s = new MirroredInputStream(source);
                InputStreamReader r;
                try
                {
                    r = new InputStreamReader(s, "UTF-8");
                }
                catch (UnsupportedEncodingException e)
                {
                    r = new InputStreamReader(s);
                }
                allPresets.addAll(TaggingPreset.readAll(new BufferedReader(r)));
            } catch (IOException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(Main.parent, tr("Could not read tagging preset source: {0}",source));
            } catch (SAXException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(Main.parent, tr("Error parsing {0}: ", source)+e.getMessage());
            }
        }
        return allPresets;
    }

    public JPanel createPanel(Collection<OsmPrimitive> selected) {
        if (data == null)
            return null;
        JPanel p = new JPanel(new GridBagLayout());
        LinkedList<Item> l = new LinkedList<Item>();
        if(types != null)
        {
            JPanel pp = new JPanel();
            for(String t : types)
            {
                JLabel la = new JLabel(ImageProvider.get("Mf_" + t));
                la.setToolTipText(tr("Elements of type {0} are supported.", tr(t)));
                pp.add(la);
            }
            p.add(pp, GBC.eol());
        }

        for (Item i : data)
        {
            if(i instanceof Link)
                l.add(i);
            else
                i.addToPanel(p, selected);
        }
        for(Item link : l)
            link.addToPanel(p, selected);
        return p;
    }

    public void actionPerformed(ActionEvent e) {
        Collection<OsmPrimitive> sel = createSelection(Main.ds.getSelected());
        JPanel p = createPanel(sel);
        if (p == null)
            return;

        int answer = 1;
        if (p.getComponentCount() != 0) {
            String title = trn("Change {0} object", "Change {0} objects", sel.size(), sel.size());
            if(sel.size() == 0) {
                if(originalSelectionEmpty)
                    title = tr("Nothing selected!");
                else
                    title = tr("Selection unsuitable!");
            }

            class PresetDialog extends ExtendedDialog {
                public PresetDialog(Component content, String title, boolean disableApply) {
                    super(Main.parent,
                            title,
                            new String[] { tr("Apply Preset"), tr("Cancel") },
                            true);
                    contentConstraints = GBC.eol().fill().insets(5,10,5,0);
                    setupDialog(content, new String[] {"ok.png", "cancel.png" });
                    buttons.get(0).setEnabled(!disableApply);
                    buttons.get(0).setToolTipText(title);
                    setVisible(true);
                }
            }

            answer = new PresetDialog(p, title, (sel.size() == 0)).getValue();
        }
        if (sel.size() != 0 && answer == 1) {
            Command cmd = createCommand(sel);
            if (cmd != null)
                Main.main.undoRedo.add(cmd);
        }
        Main.ds.setSelected(Main.ds.getSelected()); // force update
    }

    /**
     * True whenever the original selection given into createSelection was empty
     */
    private boolean originalSelectionEmpty = false;

    /**
     * Removes all unsuitable OsmPrimitives from the given list
     * @param participants List of possibile OsmPrimitives to tag
     * @return Cleaned list with suitable OsmPrimitives only
     */
    private Collection<OsmPrimitive> createSelection(Collection<OsmPrimitive> participants) {
        originalSelectionEmpty = participants.size() == 0;
        Collection<OsmPrimitive> sel = new LinkedList<OsmPrimitive>();
        for (OsmPrimitive osm : participants)
        {
            if (types != null)
            {
                if(osm instanceof Relation)
                {
                    if(!types.contains("relation")) continue;
                }
                else if(osm instanceof Node)
                {
                    if(!types.contains("node")) continue;
                }
                else if(osm instanceof Way)
                {
                    if(!types.contains("way") &&
                    !(types.contains("closedway") && ((Way)osm).isClosed()))
                        continue;
                }
            }
            sel.add(osm);
        }
        return sel;
    }

    private Command createCommand(Collection<OsmPrimitive> sel) {
        List<Command> cmds = new LinkedList<Command>();
        for (Item i : data)
            i.addCommands(sel, cmds);
        if (cmds.size() == 0)
            return null;
        else if (cmds.size() == 1)
            return cmds.get(0);
        else
            return new SequenceCommand(tr("Change Properties"), cmds);
    }
}
