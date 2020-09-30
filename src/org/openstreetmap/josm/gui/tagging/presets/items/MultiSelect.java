// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.tagging.presets.items;

import java.awt.Dimension;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListCellRenderer;
import javax.swing.ListModel;

import org.openstreetmap.josm.data.osm.Tag;
import org.openstreetmap.josm.tools.GBC;

/**
 * Multi-select list type.
 */
public class MultiSelect extends ComboMultiSelect {

    /**
     * Number of rows to display (positive integer, optional).
     */
    public short rows; // NOSONAR

    protected ConcatenatingJList list;

    @Override
    protected void addToPanelAnchor(JPanel p, String def, boolean presetInitiallyMatches) {
        list = new ConcatenatingJList(delimiter, presetListEntries.toArray(new PresetListEntry[0]));
        component = list;
        ListCellRenderer<PresetListEntry> renderer = getListCellRenderer();
        list.setCellRenderer(renderer);
        list.setSelectedItem(getItemToSelect(def, presetInitiallyMatches, true));
        JScrollPane sp = new JScrollPane(list);
        // if a number of rows has been specified in the preset,
        // modify preferred height of scroll pane to match that row count.
        if (rows > 0) {
            double height = renderer.getListCellRendererComponent(list,
                    new PresetListEntry("x"), 0, false, false).getPreferredSize().getHeight() * rows;
            sp.setPreferredSize(new Dimension((int) sp.getPreferredSize().getWidth(), (int) height));
        }
        p.add(sp, GBC.eol().fill(GBC.HORIZONTAL));
    }

    @Override
    protected Object getSelectedItem() {
        return list.getSelectedItem();
    }

    @Override
    public void addCommands(List<Tag> changedTags) {
        // Do not create any commands if list has been disabled because of an unknown value (fix #8605)
        if (list.isEnabled()) {
            super.addCommands(changedTags);
        }
    }

    /**
     * Class that allows list values to be assigned and retrieved as a comma-delimited
     * string (extracted from TaggingPreset)
     */
    private static class ConcatenatingJList extends JList<PresetListEntry> {
        private final char delimiter;

        protected ConcatenatingJList(char del, PresetListEntry... o) {
            super(o);
            delimiter = del;
        }

        public void setSelectedItem(Object o) {
            if (o == null) {
                clearSelection();
            } else {
                String s = o.toString();
                Set<String> parts = new TreeSet<>(Arrays.asList(s.split(String.valueOf(delimiter), -1)));
                ListModel<PresetListEntry> lm = getModel();
                int[] intParts = new int[lm.getSize()];
                int j = 0;
                for (int i = 0; i < lm.getSize(); i++) {
                    final String value = lm.getElementAt(i).value;
                    if (parts.contains(value)) {
                        intParts[j++] = i;
                        parts.remove(value);
                    }
                }
                setSelectedIndices(Arrays.copyOf(intParts, j));
                // check if we have actually managed to represent the full
                // value with our presets. if not, cop out; we will not offer
                // a selection list that threatens to ruin the value.
                setEnabled(parts.isEmpty());
            }
        }

        public String getSelectedItem() {
            ListModel<PresetListEntry> lm = getModel();
            int[] si = getSelectedIndices();
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < si.length; i++) {
                if (i > 0) {
                    builder.append(delimiter);
                }
                builder.append(lm.getElementAt(si[i]).value);
            }
            return builder.toString();
        }
    }
}
