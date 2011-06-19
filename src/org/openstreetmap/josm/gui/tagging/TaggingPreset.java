// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.gui.tagging;

import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trc;
import static org.openstreetmap.josm.tools.I18n.trn;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListCellRenderer;
import javax.swing.ListModel;
import javax.swing.SwingUtilities;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.command.ChangePropertyCommand;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.command.SequenceCommand;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.OsmUtils;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.Tag;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.preferences.BooleanProperty;
import org.openstreetmap.josm.gui.ExtendedDialog;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.gui.QuadStateCheckBox;
import org.openstreetmap.josm.gui.dialogs.relation.RelationEditor;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.gui.preferences.SourceEntry;
import org.openstreetmap.josm.gui.preferences.TaggingPresetPreference.PresetPrefMigration;
import org.openstreetmap.josm.gui.tagging.ac.AutoCompletingTextField;
import org.openstreetmap.josm.gui.tagging.ac.AutoCompletionItemPritority;
import org.openstreetmap.josm.gui.tagging.ac.AutoCompletionList;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.gui.widgets.HtmlPanel;
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
public class TaggingPreset extends AbstractAction implements MapView.LayerChangeListener {

    public enum PresetType {
        NODE("Mf_node"), WAY("Mf_way"), RELATION("Mf_relation"), CLOSEDWAY("Mf_closedway");

        private final String iconName;

        PresetType(String iconName) {
            this.iconName = iconName;
        }

        public String getIconName() {
            return iconName;
        }

        public String getName() {
            return name().toLowerCase();
        }
    }

    public static final int DIALOG_ANSWER_APPLY = 1;
    public static final int DIALOG_ANSWER_NEW_RELATION = 2;
    public static final int DIALOG_ANSWER_CANCEL = 3;

    public TaggingPresetMenu group = null;
    public String name;
    public String name_context;
    public String locale_name;
    public final static String OPTIONAL_TOOLTIP_TEXT = "Optional tooltip text";
    private static File zipIcons = null;
    private static final BooleanProperty PROP_FILL_DEFAULT = new BooleanProperty("taggingpreset.fill-default-for-tagged-primitives", false);

    public static abstract class Item {
        protected void initAutoCompletionField(AutoCompletingTextField field, String key) {
            OsmDataLayer layer = Main.main.getEditLayer();
            if (layer == null) return;
            AutoCompletionList list  = new AutoCompletionList();
            Main.main.getEditLayer().data.getAutoCompletionManager().populateWithTagValues(list, key);
            field.setAutoCompletionList(list);
        }

        abstract boolean addToPanel(JPanel p, Collection<OsmPrimitive> sel);
        abstract void addCommands(List<Tag> changedTags);
        boolean requestFocusInWindow() {return false;}
    }

    public static class Usage {
        TreeSet<String> values;
        boolean hadKeys = false;
        boolean hadEmpty = false;
        public boolean hasUniqueValue() {
            return values.size() == 1 && !hadEmpty;
        }

        public boolean unused() {
            return values.size() == 0;
        }
        public String getFirst() {
            return values.first();
        }

        public boolean hadKeys() {
            return hadKeys;
        }
    }

    public static final String DIFFERENT = tr("<different>");

    static Usage determineTextUsage(Collection<OsmPrimitive> sel, String key) {
        Usage returnValue = new Usage();
        returnValue.values = new TreeSet<String>();
        for (OsmPrimitive s : sel) {
            String v = s.get(key);
            if (v != null) {
                returnValue.values.add(v);
            } else {
                returnValue.hadEmpty = true;
            }
            if(s.hasKeys()) {
                returnValue.hadKeys = true;
            }
        }
        return returnValue;
    }

    static Usage determineBooleanUsage(Collection<OsmPrimitive> sel, String key) {

        Usage returnValue = new Usage();
        returnValue.values = new TreeSet<String>();
        for (OsmPrimitive s : sel) {
            String booleanValue = OsmUtils.getNamedOsmBoolean(s.get(key));
            if (booleanValue != null) {
                returnValue.values.add(booleanValue);
            }
        }
        return returnValue;
    }

    protected static class PresetListEntry {
        String value;
        String display_value;
        String short_description;

        public String getListDisplay() {
            if (value.equals(DIFFERENT))
                return "<b>"+DIFFERENT.replaceAll("<", "&lt;").replaceAll(">", "&gt;")+"</b>";

            if (value.equals(""))
                return "&nbsp;";

            StringBuilder res = new StringBuilder("<b>");
            if (display_value != null) {
                res.append(display_value);
            } else {
                res.append(value);
            }
            res.append("</b>");
            if (short_description != null) {
                // wrap in table to restrict the text width
                res.append("<br><table><td width='232'>(").append(short_description).append(")</td></table>");
            }
            return res.toString();
        }

        public PresetListEntry(String value) {
            this.value = value;
            this.display_value = value;
        }

        public PresetListEntry(String value, String display_value) {
            this.value = value;
            this.display_value = display_value;
        }

        // toString is mainly used to initialize the Editor
        @Override
        public String toString() {
            if (value.equals(DIFFERENT))
                return DIFFERENT;
            return display_value.replaceAll("<.*>", ""); // remove additional markup, e.g. <br>
        }
    }

    public static class Text extends Item {

        public String key;
        public String text;
        public String locale_text;
        public String text_context;
        public String default_;
        public String originalValue;
        public boolean use_last_as_default = false;
        public boolean delete_if_empty = false;
        public boolean required = false;

        private JComponent value;

        @Override public boolean addToPanel(JPanel p, Collection<OsmPrimitive> sel) {

            // find out if our key is already used in the selection.
            Usage usage = determineTextUsage(sel, key);
            AutoCompletingTextField textField = new AutoCompletingTextField();
            initAutoCompletionField(textField, key);
            if (usage.unused()){
                if (!usage.hadKeys() || PROP_FILL_DEFAULT.get()) {
                    // selected osm primitives are untagged or filling default values feature is enabled
                    if (use_last_as_default && lastValue.containsKey(key)) {
                        textField.setText(lastValue.get(key));
                    } else {
                        textField.setText(default_);
                    }
                } else {
                    // selected osm primitives are tagged and filling default values feature is disabled
                    textField.setText("");
                }
                value = textField;
                originalValue = null;
            } else if (usage.hasUniqueValue()) {
                // all objects use the same value
                textField.setText(usage.getFirst());
                value = textField;
                originalValue = usage.getFirst();
            } else {
                // the objects have different values
                JComboBox comboBox = new JComboBox(usage.values.toArray());
                comboBox.setEditable(true);
                comboBox.setEditor(textField);
                comboBox.getEditor().setItem(DIFFERENT);
                value=comboBox;
                originalValue = DIFFERENT;
            }
            if(locale_text == null) {
                if (text != null) {
                    if(text_context != null) {
                        locale_text = trc(text_context, text);
                    } else {
                        locale_text = tr(text);
                    }
                }
            }
            p.add(new JLabel(locale_text+":"), GBC.std().insets(0,0,10,0));
            p.add(value, GBC.eol().fill(GBC.HORIZONTAL));
            return true;
        }

        @Override public void addCommands(List<Tag> changedTags) {

            // return if unchanged
            String v = (value instanceof JComboBox) ?
                    ((JComboBox)value).getEditor().getItem().toString() :
                        ((JTextField)value).getText();

                    if (use_last_as_default) {
                        lastValue.put(key, v);
                    }
                    if (v.equals(originalValue) || (originalValue == null && v.length() == 0)) return;

                    if (delete_if_empty && v.length() == 0) {
                        v = null;
                    }
                    changedTags.add(new Tag(key, v));
        }
        @Override boolean requestFocusInWindow() {return value.requestFocusInWindow();}
    }

    public static class Check extends Item {

        public String key;
        public String text;
        public String text_context;
        public String locale_text;
        public String value_on = OsmUtils.trueval;
        public String value_off = OsmUtils.falseval;
        public boolean default_ = false; // only used for tagless objects
        public boolean use_last_as_default = false;
        public boolean required = false;

        private QuadStateCheckBox check;
        private QuadStateCheckBox.State initialState;
        private boolean def;

        @Override public boolean addToPanel(JPanel p, Collection<OsmPrimitive> sel) {

            // find out if our key is already used in the selection.
            Usage usage = determineBooleanUsage(sel, key);
            def = default_;

            if(locale_text == null) {
                if(text_context != null) {
                    locale_text = trc(text_context, text);
                } else {
                    locale_text = tr(text);
                }
            }

            String oneValue = null;
            for (String s : usage.values) {
                oneValue = s;
            }
            if (usage.values.size() < 2 && (oneValue == null || value_on.equals(oneValue) || value_off.equals(oneValue))) {
                if (def && !PROP_FILL_DEFAULT.get()) {
                    // default is set and filling default values feature is disabled - check if all primitives are untagged
                    for (OsmPrimitive s : sel)
                        if(s.hasKeys()) {
                            def = false;
                        }
                }

                // all selected objects share the same value which is either true or false or unset,
                // we can display a standard check box.
                initialState = value_on.equals(oneValue) ?
                        QuadStateCheckBox.State.SELECTED :
                            value_off.equals(oneValue) ?
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
            return true;
        }

        @Override public void addCommands(List<Tag> changedTags) {
            // if the user hasn't changed anything, don't create a command.
            if (check.getState() == initialState && !def) return;

            // otherwise change things according to the selected value.
            changedTags.add(new Tag(key,
                    check.getState() == QuadStateCheckBox.State.SELECTED ? value_on :
                        check.getState() == QuadStateCheckBox.State.NOT_SELECTED ? value_off :
                            null));
        }
        @Override boolean requestFocusInWindow() {return check.requestFocusInWindow();}
    }

    public static class Combo extends Item {

        public String key;
        public String text;
        public String text_context;
        public String locale_text;
        public String values;
        public String values_context;
        public String display_values;
        public String locale_display_values;
        public String short_descriptions;
        public String locale_short_descriptions;
        public String default_;
        public boolean delete_if_empty = false;
        public boolean editable = true;
        public boolean use_last_as_default = false;
        public boolean required = false;

        private List<String> short_description_list;
        private JComboBox combo;
        private Map<String, PresetListEntry> lhm;
        private Usage usage;
        private PresetListEntry originalValue;

        @Override public boolean addToPanel(JPanel p, Collection<OsmPrimitive> sel) {

            // find out if our key is already used in the selection.
            usage = determineTextUsage(sel, key);
            String def = default_;

            String[] value_array = splitEscaped(',', values);
            String[] display_array;
            String[] short_descriptions_array = null;

            if(locale_display_values != null) {
                display_array = splitEscaped(',', locale_display_values);
            } else if (display_values != null) {
                display_array = splitEscaped(',', display_values);
            } else {
                display_array = value_array;
            }

            if (locale_short_descriptions != null) {
                short_descriptions_array = splitEscaped(',', locale_short_descriptions);
            } else if (short_descriptions != null) {
                short_descriptions_array = splitEscaped(',', short_descriptions);
            } else if (short_description_list != null) {
                short_descriptions_array = short_description_list.toArray(new String[0]);
            }

            if (use_last_as_default && def == null && lastValue.containsKey(key)) {
                def = lastValue.get(key);
            }

            if (display_array.length != value_array.length) {
                System.err.println(tr("Broken tagging preset \"{0}-{1}\" - number of items in ''display_values'' must be the same as in ''values''", key, text));
                display_array = value_array;
            }

            if (short_descriptions_array != null && short_descriptions_array.length != value_array.length) {
                System.err.println(tr("Broken tagging preset \"{0}-{1}\" - number of items in ''short_descriptions'' must be the same as in ''values''", key, text));
                short_descriptions_array = null;
            }

            lhm = new LinkedHashMap<String, PresetListEntry>();
            if (!usage.hasUniqueValue() && !usage.unused()) {
                lhm.put(DIFFERENT, new PresetListEntry(DIFFERENT));
            }
            for (int i=0; i<value_array.length; i++) {
                PresetListEntry e = new PresetListEntry(value_array[i]);
                e.display_value = (locale_display_values == null)
                ? (values_context == null ? tr(display_array[i])
                        : trc(values_context, display_array[i])) : display_array[i];
                if (short_descriptions_array != null) {
                    e.short_description = locale_short_descriptions == null ? tr(short_descriptions_array[i])
                            : short_descriptions_array[i];
                }
                lhm.put(value_array[i], e);
            }
            if(!usage.unused()){
                for (String s : usage.values) {
                    if (!lhm.containsKey(s)) {
                        lhm.put(s, new PresetListEntry(s));
                    }
                }
            }
            if (def != null && !lhm.containsKey(def)) {
                lhm.put(def, new PresetListEntry(def));
            }
            lhm.put("", new PresetListEntry(""));

            combo = new JComboBox(lhm.values().toArray());
            combo.setRenderer(new PresetComboListCellRenderer());
            combo.setEditable(editable);
            combo.setMaximumRowCount(13);
            AutoCompletingTextField tf = new AutoCompletingTextField();
            initAutoCompletionField(tf, key);
            tf.getAutoCompletionList().add(Arrays.asList(display_array), AutoCompletionItemPritority.IS_IN_STANDARD);
            combo.setEditor(tf);

            if (usage.hasUniqueValue()) {
                // all items have the same value (and there were no unset items)
                originalValue=lhm.get(usage.getFirst());
                combo.setSelectedItem(originalValue);
            }
            else if (def != null && usage.unused()) {
                // default is set and all items were unset
                if (!usage.hadKeys() || PROP_FILL_DEFAULT.get()) {
                    // selected osm primitives are untagged or filling default feature is enabled
                    combo.setSelectedItem(def);
                } else {
                    // selected osm primitives are tagged and filling default feature is disabled
                    combo.setSelectedItem("");
                }
                originalValue=lhm.get(DIFFERENT);
            }
            else if (usage.unused()) {
                // all items were unset (and so is default)
                originalValue=lhm.get("");
                combo.setSelectedItem(originalValue);
            }
            else {
                originalValue=lhm.get(DIFFERENT);
                combo.setSelectedItem(originalValue);
            }

            if(locale_text == null) {
                if(text_context != null) {
                    locale_text = trc(text_context, text);
                } else {
                    locale_text = tr(text);
                }
            }
            p.add(new JLabel(locale_text+":"), GBC.std().insets(0,0,10,0));
            p.add(combo, GBC.eol().fill(GBC.HORIZONTAL));
            return true;
        }


        private static class PresetComboListCellRenderer implements ListCellRenderer {

            HtmlPanel lbl;
            JComponent dummy = new JComponent() {};

            public PresetComboListCellRenderer() {
                lbl = new HtmlPanel();
            }

            public Component getListCellRendererComponent(
                    JList list,
                    Object value,
                    int index,
                    boolean isSelected,
                    boolean cellHasFocus)
            {
                if (isSelected) {
                    lbl.setBackground(list.getSelectionBackground());
                    lbl.setForeground(list.getSelectionForeground());
                } else {
                    lbl.setBackground(list.getBackground());
                    lbl.setForeground(list.getForeground());
                }

                PresetListEntry item = (PresetListEntry) value;
                String s = item.getListDisplay();
                lbl.setText(s);
                // We do not want the editor to have the maximum height of all
                // entries. Return a dummy with bogus height.
                if (index == -1) {
                    dummy.setPreferredSize(new Dimension(lbl.getPreferredSize().width, 10));
                    return dummy;
                }
                return lbl;
            }
        }

        @Override public void addCommands(List<Tag> changedTags) {
            Object obj = combo.getSelectedItem();
            String display = (obj == null) ? null : obj.toString();
            String value = null;
            if(display == null && combo.isEditable()) {
                display = combo.getEditor().getItem().toString();
            }

            if (display != null)
            {
                for (String key : lhm.keySet()) {
                    String k = lhm.get(key).toString();
                    if (k != null && k.equals(display)) {
                        value=key;
                    }
                }
                if(value == null) {
                    value = display;
                }
            } else {
                value = "";
            }

            // no change if same as before
            if (originalValue == null) {
                if (value.length() == 0)
                    return;
            } else if (value.equals(originalValue.toString()))
                return;

            if (delete_if_empty && value.length() == 0) {
                value = null;
            }
            if (use_last_as_default) {
                lastValue.put(key, value);
            }
            changedTags.add(new Tag(key, value));
        }

        public void setShort_description(String s) {
            if (short_description_list == null) {
                short_description_list = new ArrayList<String>();
            }
            short_description_list.add(tr(s));
        }

        @Override boolean requestFocusInWindow() {return combo.requestFocusInWindow();}
    }

    /**
     * Class that allows list values to be assigned and retrived as a comma-delimited
     * string.
     */
    public static class ConcatenatingJList extends JList {
        private String delimiter;
        public ConcatenatingJList(String del, Object[] o) {
            super(o);
            delimiter = del;
        }
        public void setSelectedItem(Object o) {
            if (o == null) {
                clearSelection();
            } else {
                String s = o.toString();
                HashSet<String> parts = new HashSet<String>(Arrays.asList(s.split(delimiter)));
                ListModel lm = getModel();
                int[] intParts = new int[lm.getSize()];
                int j = 0;
                for (int i = 0; i < lm.getSize(); i++) {
                    if (parts.contains((((PresetListEntry)lm.getElementAt(i)).value))) {
                        intParts[j++]=i;
                    }
                }
                setSelectedIndices(Arrays.copyOf(intParts, j));
                // check if we have acutally managed to represent the full
                // value with our presets. if not, cop out; we will not offer
                // a selection list that threatens to ruin the value.
                setEnabled(s.equals(getSelectedItem()));
            }
        }
        public String getSelectedItem() {
            ListModel lm = getModel();
            int[] si = getSelectedIndices();
            StringBuilder builder = new StringBuilder();
            for (int i=0; i<si.length; i++) {
                if (i>0) {
                    builder.append(delimiter);
                }
                builder.append(((PresetListEntry)lm.getElementAt(si[i])).value);
            }
            return builder.toString();
        }
    }

    public static class MultiSelect extends Item {

        public String key;
        public String text;
        public String text_context;
        public String locale_text;
        public String values;
        public String values_context;
        public String display_values;
        public String locale_display_values;
        public String short_descriptions;
        public String locale_short_descriptions;
        public String default_;
        public String delimiter = ";";
        public boolean delete_if_empty = false;
        public boolean use_last_as_default = false;
        public boolean required = false;
        public long rows = -1;

        private List<String> short_description_list;
        private ConcatenatingJList list;
        private Map<String, PresetListEntry> lhm;
        private Usage usage;
        private String originalValue;

        @Override public boolean addToPanel(JPanel p, Collection<OsmPrimitive> sel) {

            // find out if our key is already used in the selection.
            usage = determineTextUsage(sel, key);
            String def = default_;

            char delChar = ';';
            if (delimiter.length() > 0) {
                delChar = delimiter.charAt(0);
            }

            String[] value_array = splitEscaped(delChar, values);
            String[] display_array;
            String[] short_descriptions_array = null;

            if (locale_display_values != null) {
                display_array = splitEscaped(delChar, locale_display_values);
            } else if (display_values != null) {
                display_array = splitEscaped(delChar, display_values);
            } else {
                display_array = value_array;
            }

            if (locale_short_descriptions != null) {
                short_descriptions_array = splitEscaped(delChar, locale_short_descriptions);
            } else if (short_descriptions != null) {
                short_descriptions_array = splitEscaped(delChar, short_descriptions);
            } else if (short_description_list != null) {
                short_descriptions_array = short_description_list.toArray(new String[0]);
            }

            if (use_last_as_default && def == null && lastValue.containsKey(key)) {
                def = lastValue.get(key);
            }

            if (display_array.length != value_array.length) {
                System.err.println(tr("Broken tagging preset \"{0}-{1}\" - number of items in ''display_values'' must be the same as in ''values''", key, text));
                display_array = value_array;
            }

            if (short_descriptions_array != null && short_descriptions_array.length != value_array.length) {
                System.err.println(tr("Broken tagging preset \"{0}-{1}\" - number of items in ''short_descriptions'' must be the same as in ''values''", key, text));
                short_descriptions_array = null;
            }

            lhm = new LinkedHashMap<String, PresetListEntry>();
            if (!usage.hasUniqueValue() && !usage.unused()) {
                lhm.put(DIFFERENT, new PresetListEntry(DIFFERENT));
            }
            for (int i=0; i<value_array.length; i++) {
                PresetListEntry e = new PresetListEntry(value_array[i]);
                e.display_value = (locale_display_values == null)
                ? (values_context == null ? tr(display_array[i])
                        : trc(values_context, display_array[i])) : display_array[i];
                if (short_descriptions_array != null) {
                    e.short_description = locale_short_descriptions == null ? tr(short_descriptions_array[i])
                            : short_descriptions_array[i];
                }
                lhm.put(value_array[i], e);
            }

            list = new ConcatenatingJList(delimiter, lhm.values().toArray());
            PresetListCellRenderer renderer = new PresetListCellRenderer();
            list.setCellRenderer(renderer);

            if (usage.hasUniqueValue() && !usage.unused()) {
                originalValue=usage.getFirst();
            }
            else if (def != null && !usage.hadKeys()) {
                originalValue=def;
            }
            else if (usage.unused()) {
                originalValue=null;
            }
            else {
                originalValue=DIFFERENT;
            }
            list.setSelectedItem(originalValue);

            if (locale_text == null) {
                if(text_context != null) {
                    locale_text = trc(text_context, text);
                } else {
                    locale_text = tr(text);
                }
            }
            p.add(new JLabel(locale_text+":"), GBC.std().insets(0,0,10,0));
            JScrollPane sp = new JScrollPane(list);
            // if a number of rows has been specified in the preset,
            // modify preferred height of scroll pane to match that row count.
            if (rows != -1)
            {
                double height = renderer.getListCellRendererComponent(list, 
                    new PresetListEntry("x"), 0, false, false).getPreferredSize().getHeight() * rows;
                sp.setPreferredSize(new Dimension((int) sp.getPreferredSize().getWidth(), (int) height));
            }
            p.add(sp, GBC.eol().fill(GBC.HORIZONTAL));
            return true;
        }

        private static class PresetListCellRenderer implements ListCellRenderer {

            HtmlPanel lbl;
            JComponent dummy = new JComponent() {};

            public PresetListCellRenderer() {
                lbl = new HtmlPanel();
            }

            public Component getListCellRendererComponent(
                    JList list,
                    Object value,
                    int index,
                    boolean isSelected,
                    boolean cellHasFocus)
            {
                if (isSelected) {
                    lbl.setBackground(list.getSelectionBackground());
                    lbl.setForeground(list.getSelectionForeground());
                } else {
                    lbl.setBackground(list.getBackground());
                    lbl.setForeground(list.getForeground());
                }

                PresetListEntry item = (PresetListEntry) value;
                String s = item.getListDisplay();
                lbl.setText(s);
                lbl.setEnabled(list.isEnabled());
                // We do not want the editor to have the maximum height of all
                // entries. Return a dummy with bogus height.
                if (index == -1) {
                    dummy.setPreferredSize(new Dimension(lbl.getPreferredSize().width, 10));
                    return dummy;
                }
                return lbl;
            }
        }

        @Override public void addCommands(List<Tag> changedTags) {
            Object obj = list.getSelectedItem();
            String display = (obj == null) ? null : obj.toString();
            String value = null;

            if (display != null)
            {
                for (String key : lhm.keySet()) {
                    String k = lhm.get(key).toString();
                    if (k != null && k.equals(display)) {
                        value=key;
                    }
                }
                if (value == null) {
                    value = display;
                }
            } else {
                value = "";
            }

            // no change if same as before
            if (originalValue == null) {
                if (value.length() == 0)
                    return;
            } else if (value.equals(originalValue.toString()))
                return;

            if (delete_if_empty && value.length() == 0) {
                value = null;
            }
            if (use_last_as_default) {
                lastValue.put(key, value);
            }
            changedTags.add(new Tag(key, value));
        }

        public void setShort_description(String s) {
            if (short_description_list == null) {
                short_description_list = new ArrayList<String>();
            }
            short_description_list.add(tr(s));
        }

        @Override boolean requestFocusInWindow() {return list.requestFocusInWindow();}
    }

    /**
     * allow escaped comma in comma separated list:
     * "A\, B\, C,one\, two" --> ["A, B, C", "one, two"]
     * @param delimiter the delimiter, e.g. a comma. separates the entries and
     *      must be escaped within one entry
     * @param s the string
     */
    private static String[] splitEscaped(char delemiter, String s) {
        List<String> result = new ArrayList<String>();
        boolean backslash = false;
        StringBuilder item = new StringBuilder();
        for (int i=0; i<s.length(); i++) {
            char ch = s.charAt(i);
            if (backslash) {
                item.append(ch);
                backslash = false;
            } else if (ch == '\\') {
                backslash = true;
            } else if (ch == delemiter) {
                result.add(item.toString());
                item.setLength(0);
            } else {
                item.append(ch);
            }
        }
        if (item.length() > 0) {
            result.add(item.toString());
        }
        return result.toArray(new String[result.size()]);
    }

    public static class Label extends Item {
        public String text;
        public String text_context;
        public String locale_text;

        @Override public boolean addToPanel(JPanel p, Collection<OsmPrimitive> sel) {
            if(locale_text == null) {
                if(text_context != null) {
                    locale_text = trc(text_context, text);
                } else {
                    locale_text = tr(text);
                }
            }
            p.add(new JLabel(locale_text), GBC.eol());
            return false;
        }
        @Override public void addCommands(List<Tag> changedTags) {}
    }

    public static class Link extends Item {
        public String href;
        public String text;
        public String text_context;
        public String locale_text;
        public String locale_href;

        @Override public boolean addToPanel(JPanel p, Collection<OsmPrimitive> sel) {
            if(locale_text == null) {
                if(text == null) {
                    locale_text = tr("More information about this feature");
                } else if(text_context != null) {
                    locale_text = trc(text_context, text);
                } else {
                    locale_text = tr(text);
                }
            }
            String url = locale_href;
            if (url == null) {
                url = href;
            }
            if (url != null) {
                p.add(new UrlLabel(url, locale_text), GBC.eol().anchor(GBC.WEST));
            }
            return false;
        }
        @Override public void addCommands(List<Tag> changedTags) {}
    }

    public static class Role {
        public EnumSet<PresetType> types;
        public String key;
        public String text;
        public String text_context;
        public String locale_text;

        public boolean required=false;
        public long count = 0;

        public void setType(String types) throws SAXException {
            this.types = TaggingPreset.getType(types);
        }

        public void setRequisite(String str) throws SAXException {
            if("required".equals(str)) {
                required = true;
            } else if(!"optional".equals(str))
                throw new SAXException(tr("Unknown requisite: {0}", str));
        }

        /* return either argument, the highest possible value or the lowest
           allowed value */
        public long getValidCount(long c)
        {
            if(count > 0 && !required)
                return c != 0 ? count : 0;
            else if(count > 0)
                return count;
            else if(!required)
                return c != 0  ? c : 0;
            else
                return c != 0  ? c : 1;
        }
        public boolean addToPanel(JPanel p, Collection<OsmPrimitive> sel) {
            String cstring;
            if(count > 0 && !required) {
                cstring = "0,"+String.valueOf(count);
            } else if(count > 0) {
                cstring = String.valueOf(count);
            } else if(!required) {
                cstring = "0-...";
            } else {
                cstring = "1-...";
            }
            if(locale_text == null) {
                if (text != null) {
                    if(text_context != null) {
                        locale_text = trc(text_context, text);
                    } else {
                        locale_text = tr(text);
                    }
                }
            }
            p.add(new JLabel(locale_text+":"), GBC.std().insets(0,0,10,0));
            p.add(new JLabel(key), GBC.std().insets(0,0,10,0));
            p.add(new JLabel(cstring), types == null ? GBC.eol() : GBC.std().insets(0,0,10,0));
            if(types != null){
                JPanel pp = new JPanel();
                for(PresetType t : types) {
                    pp.add(new JLabel(ImageProvider.get(t.getIconName())));
                }
                p.add(pp, GBC.eol());
            }
            return true;
        }
    }

    public static class Roles extends Item {
        public List<Role> roles = new LinkedList<Role>();
        @Override public boolean addToPanel(JPanel p, Collection<OsmPrimitive> sel) {
            p.add(new JLabel(" "), GBC.eol()); // space
            if(roles.size() > 0)
            {
                JPanel proles = new JPanel(new GridBagLayout());
                proles.add(new JLabel(tr("Available roles")), GBC.std().insets(0,0,10,0));
                proles.add(new JLabel(tr("role")), GBC.std().insets(0,0,10,0));
                proles.add(new JLabel(tr("count")), GBC.std().insets(0,0,10,0));
                proles.add(new JLabel(tr("elements")), GBC.eol());
                for (Role i : roles) {
                    i.addToPanel(proles, sel);
                }
                p.add(proles, GBC.eol());
            }
            return false;
        }
        @Override public void addCommands(List<Tag> changedTags) {}
    }

    public static class Optional extends Item {
        // TODO: Draw a box around optional stuff
        @Override public boolean addToPanel(JPanel p, Collection<OsmPrimitive> sel) {
            p.add(new JLabel(" "), GBC.eol()); // space
            p.add(new JLabel(tr("Optional Attributes:")), GBC.eol());
            p.add(new JLabel(" "), GBC.eol()); // space
            return false;
        }
        @Override public void addCommands(List<Tag> changedTags) {}
    }

    public static class Space extends Item {
        @Override public boolean addToPanel(JPanel p, Collection<OsmPrimitive> sel) {
            p.add(new JLabel(" "), GBC.eol()); // space
            return false;
        }
        @Override public void addCommands(List<Tag> changedTags) {}
    }

    public static class Key extends Item {
        public String key;
        public String value;

        @Override public boolean addToPanel(JPanel p, Collection<OsmPrimitive> sel) { return false; }
        @Override public void addCommands(List<Tag> changedTags) {
            changedTags.add(new Tag(key, value != null && !value.equals("") ? value : null));
        }
    }

    /**
     * The types as preparsed collection.
     */
    public EnumSet<PresetType> types;
    public List<Item> data = new LinkedList<Item>();
    private static HashMap<String,String> lastValue = new HashMap<String,String>();

    /**
     * Create an empty tagging preset. This will not have any items and
     * will be an empty string as text. createPanel will return null.
     * Use this as default item for "do not select anything".
     */
    public TaggingPreset() {
        MapView.addLayerChangeListener(this);
        updateEnabledState();
    }

    /**
     * Change the display name without changing the toolbar value.
     */
    public void setDisplayName() {
        putValue(Action.NAME, getName());
        putValue("toolbar", "tagging_" + getRawName());
        putValue(OPTIONAL_TOOLTIP_TEXT, (group != null ?
            tr("Use preset ''{0}'' of group ''{1}''", getLocaleName(), group.getName()) :
            tr("Use preset ''{0}''", getLocaleName())));
   }

    public String getLocaleName() {
        if(locale_name == null) {
            if(name_context != null) {
                locale_name = trc(name_context, name);
            } else {
                locale_name = tr(name);
            }
        }
        return locale_name;
    }

    public String getName() {
        return group != null ? group.getName() + "/" + getLocaleName() : getLocaleName();
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
        Collection<String> s = Main.pref.getCollection("taggingpreset.icon.sources", null);
        ImageIcon icon = ImageProvider.getIfAvailable(s, "presets", null, iconName, zipIcons);
        if (icon == null)
        {
            System.out.println("Could not get presets icon " + iconName);
            icon = new ImageIcon(iconName);
        }
        if (Math.max(icon.getIconHeight(), icon.getIconWidth()) != 16) {
            icon = new ImageIcon(icon.getImage().getScaledInstance(16, 16, Image.SCALE_SMOOTH));
        }
        putValue(Action.SMALL_ICON, icon);
    }

    /**
     * Called from the XML parser to set the types this preset affects
     */

    static public EnumSet<PresetType> getType(String types) throws SAXException {
        EnumSet<PresetType> result = EnumSet.noneOf(PresetType.class);
        for (String type : Arrays.asList(types.split(","))) {
            try {
                PresetType presetType = PresetType.valueOf(type.toUpperCase());
                result.add(presetType);
            } catch (IllegalArgumentException e) {
                throw new SAXException(tr("Unknown type: {0}", type));
            }
        }
        return result;
    }

    public void setType(String types) throws SAXException {
        this.types = getType(types);
    }

    public static List<TaggingPreset> readAll(Reader in, boolean validate) throws SAXException {
        XmlObjectParser parser = new XmlObjectParser();
        parser.mapOnStart("item", TaggingPreset.class);
        parser.mapOnStart("separator", TaggingPresetSeparator.class);
        parser.mapBoth("group", TaggingPresetMenu.class);
        parser.map("text", Text.class);
        parser.map("link", Link.class);
        parser.mapOnStart("optional", Optional.class);
        parser.mapOnStart("roles", Roles.class);
        parser.map("role", Role.class);
        parser.map("check", Check.class);
        parser.map("combo", Combo.class);
        parser.map("multiselect", MultiSelect.class);
        parser.map("label", Label.class);
        parser.map("space", Space.class);
        parser.map("key", Key.class);
        LinkedList<TaggingPreset> all = new LinkedList<TaggingPreset>();
        TaggingPresetMenu lastmenu = null;
        Roles lastrole = null;

        if (validate) {
            parser.startWithValidation(in, "http://josm.openstreetmap.de/tagging-preset-1.0", "resource://data/tagging-preset.xsd");
        } else {
            parser.start(in);
        }
        while(parser.hasNext()) {
            Object o = parser.next();
            if (o instanceof TaggingPresetMenu) {
                TaggingPresetMenu tp = (TaggingPresetMenu) o;
                if(tp == lastmenu) {
                    lastmenu = tp.group;
                } else
                {
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
                if(all.size() != 0) {
                    if(o instanceof Roles) {
                        all.getLast().data.add((Item)o);
                        lastrole = (Roles) o;
                    }
                    else if(o instanceof Role) {
                        if(lastrole == null)
                            throw new SAXException(tr("Preset role element without parent"));
                        lastrole.roles.add((Role) o);
                    }
                    else {
                        all.getLast().data.add((Item)o);
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
                tp = TaggingPreset.readAll(new BufferedReader(r), validate);
            } finally {
                r.close();
            }
        } finally {
            s.close();
        }
        return tp;
    }

    public static Collection<TaggingPreset> readAll(Collection<String> sources, boolean validate) {
        LinkedList<TaggingPreset> allPresets = new LinkedList<TaggingPreset>();
        for(String source : sources)  {
            try {
                allPresets.addAll(TaggingPreset.readAll(source, validate));
            } catch (IOException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(
                        Main.parent,
                        tr("Could not read tagging preset source: {0}",source),
                        tr("Error"),
                        JOptionPane.ERROR_MESSAGE
                );
            } catch (SAXException e) {
                System.err.println(e.getMessage());
                System.err.println(source);
                e.printStackTrace();
                JOptionPane.showMessageDialog(
                        Main.parent,
                        tr("Error parsing {0}: ", source)+e.getMessage(),
                        tr("Error"),
                        JOptionPane.ERROR_MESSAGE
                );
            }
            zipIcons = null;
        }
        return allPresets;
    }

    public static LinkedList<String> getPresetSources() {
        LinkedList<String> sources = new LinkedList<String>();

        for (SourceEntry e : (new PresetPrefMigration()).get()) {
            sources.add(e.url);
        }

        return sources;
    }

    public static Collection<TaggingPreset> readFromPreferences(boolean validate) {
        return readAll(getPresetSources(), validate);
    }

    private static class PresetPanel extends JPanel {
        boolean hasElements = false;
        PresetPanel()
        {
            super(new GridBagLayout());
        }
    }

    public PresetPanel createPanel(Collection<OsmPrimitive> selected) {
        if (data == null)
            return null;
        PresetPanel p = new PresetPanel();
        LinkedList<Item> l = new LinkedList<Item>();
        if(types != null){
            JPanel pp = new JPanel();
            for(PresetType t : types){
                JLabel la = new JLabel(ImageProvider.get(t.getIconName()));
                la.setToolTipText(tr("Elements of type {0} are supported.", tr(t.getName())));
                pp.add(la);
            }
            p.add(pp, GBC.eol());
        }

        JPanel items = new JPanel(new GridBagLayout());
        for (Item i : data){
            if(i instanceof Link) {
                l.add(i);
            } else {
                if(i.addToPanel(items, selected)) {
                    p.hasElements = true;
                }
            }
        }
        p.add(items, GBC.eol().fill());
        if (selected.size() == 0 && !supportsRelation()) {
            GuiHelper.setEnabledRec(items, false);
        }

        for(Item link : l) {
            link.addToPanel(p, selected);
        }

        return p;
    }

    public boolean isShowable()
    {
        for(Item i : data)
        {
            if(!(i instanceof Optional || i instanceof Space || i instanceof Key))
                return true;
        }
        return false;
    }

    public void actionPerformed(ActionEvent e) {
        if (Main.main == null) return;
        if (Main.main.getCurrentDataSet() == null) return;

        Collection<OsmPrimitive> sel = Main.main.getCurrentDataSet().getSelected();
        int answer = showDialog(sel, supportsRelation());

        if (sel.size() != 0 && answer == DIALOG_ANSWER_APPLY) {
            Command cmd = createCommand(sel, getChangedTags());
            if (cmd != null) {
                Main.main.undoRedo.add(cmd);
            }
        } else if (answer == DIALOG_ANSWER_NEW_RELATION) {
            List<Command> cmds = new ArrayList<Command>(2);
            final Relation r = new Relation();
            final Collection<RelationMember> members = new HashSet<RelationMember>();
            for(Tag t : getChangedTags()) {
                r.put(t.getKey(), t.getValue());
            }
            for(OsmPrimitive osm : sel) {
                RelationMember rm = new RelationMember("", osm);
                r.addMember(rm);
                members.add(rm);
            }
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    RelationEditor.getEditor(Main.main.getEditLayer(), r, members).setVisible(true);
                }
            });
        }
        Main.main.getCurrentDataSet().setSelected(Main.main.getCurrentDataSet().getSelected()); // force update

    }

    public int showDialog(Collection<OsmPrimitive> selection, final boolean showNewRelation) {
        Collection<OsmPrimitive> sel = createSelection(selection);
        PresetPanel p = createPanel(sel);
        if (p == null)
            return DIALOG_ANSWER_CANCEL;

        int answer = 1;
        if (p.getComponentCount() != 0 && (sel.size() == 0 || p.hasElements)) {
            String title = trn("Change {0} object", "Change {0} objects", sel.size(), sel.size());
            if(sel.size() == 0) {
                if(originalSelectionEmpty) {
                    title = tr("Nothing selected!");
                } else {
                    title = tr("Selection unsuitable!");
                }
            }

            class PresetDialog extends ExtendedDialog {
                public PresetDialog(Component content, String title, boolean disableApply) {
                    super(Main.parent,
                            title,
                            showNewRelation?
                                    new String[] { tr("Apply Preset"), tr("New relation"), tr("Cancel") }:
                                        new String[] { tr("Apply Preset"), tr("Cancel") },
                                        true);
                    contentInsets = new Insets(10,5,0,5);
                    if (showNewRelation) {
                        setButtonIcons(new String[] {"ok.png", "dialogs/addrelation.png", "cancel.png" });
                    } else {
                        setButtonIcons(new String[] {"ok.png", "cancel.png" });
                    }
                    setContent(content);
                    setDefaultButton(1);
                    setupDialog();
                    buttons.get(0).setEnabled(!disableApply);
                    buttons.get(0).setToolTipText(title);
                    showDialog();
                }
            }

            answer = new PresetDialog(p, title, (sel.size() == 0)).getValue();
        }
        if (!showNewRelation && answer == 2)
            return DIALOG_ANSWER_CANCEL;
        else
            return answer;
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
                    if(!types.contains(PresetType.RELATION)) {
                        continue;
                    }
                }
                else if(osm instanceof Node)
                {
                    if(!types.contains(PresetType.NODE)) {
                        continue;
                    }
                }
                else if(osm instanceof Way)
                {
                    if(!types.contains(PresetType.WAY) &&
                            !(types.contains(PresetType.CLOSEDWAY) && ((Way)osm).isClosed())) {
                        continue;
                    }
                }
            }
            sel.add(osm);
        }
        return sel;
    }

    public List<Tag> getChangedTags() {
        List<Tag> result = new ArrayList<Tag>();
        for (Item i: data) {
            i.addCommands(result);
        }
        return result;
    }

    public static Command createCommand(Collection<OsmPrimitive> sel, List<Tag> changedTags) {
        List<Command> cmds = new ArrayList<Command>();
        for (Tag tag: changedTags) {
            if (!tag.getValue().isEmpty()) {
                cmds.add(new ChangePropertyCommand(sel, tag.getKey(), tag.getValue()));
            }
        }

        if (cmds.size() == 0)
            return null;
        else if (cmds.size() == 1)
            return cmds.get(0);
        else
            return new SequenceCommand(tr("Change Properties"), cmds);
    }

    private boolean supportsRelation() {
        return types == null || types.contains(PresetType.RELATION);
    }

    protected void updateEnabledState() {
        setEnabled(Main.main != null && Main.main.getCurrentDataSet() != null);
    }

    public void activeLayerChange(Layer oldLayer, Layer newLayer) {
        updateEnabledState();
    }

    public void layerAdded(Layer newLayer) {
        updateEnabledState();
    }

    public void layerRemoved(Layer oldLayer) {
        updateEnabledState();
    }

    @Override
    public String toString() {
        return (types == null?"":types) + " " + name;
    }
}
