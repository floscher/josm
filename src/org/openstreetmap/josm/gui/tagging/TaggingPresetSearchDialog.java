// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.tagging;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;

import javax.swing.AbstractListModel;
import javax.swing.Action;
import javax.swing.BoxLayout;
import javax.swing.DefaultListCellRenderer;
import javax.swing.Icon;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.SelectionChangedListener;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.preferences.BooleanProperty;
import org.openstreetmap.josm.gui.ExtendedDialog;
import org.openstreetmap.josm.gui.preferences.map.TaggingPresetPreference;
import org.openstreetmap.josm.gui.tagging.TaggingPreset.Item;
import org.openstreetmap.josm.gui.tagging.TaggingPreset.Key;
import org.openstreetmap.josm.gui.tagging.TaggingPreset.PresetType;
import org.openstreetmap.josm.gui.tagging.TaggingPreset.Role;
import org.openstreetmap.josm.gui.tagging.TaggingPreset.Roles;
import org.openstreetmap.josm.gui.widgets.JosmTextField;


public class TaggingPresetSearchDialog extends ExtendedDialog implements SelectionChangedListener {

    private static final int CLASSIFICATION_IN_FAVORITES = 300;
    private static final int CLASSIFICATION_NAME_MATCH = 300;
    private static final int CLASSIFICATION_GROUP_MATCH = 200;
    private static final int CLASSIFICATION_TAGS_MATCH = 100;

    private static final BooleanProperty SEARCH_IN_TAGS = new BooleanProperty("taggingpreset.dialog.search-in-tags", true);
    private static final BooleanProperty ONLY_APPLICABLE  = new BooleanProperty("taggingpreset.dialog.only-applicable-to-selection", true);

    private static class ResultListCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected,
                boolean cellHasFocus) {
            JLabel result = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            TaggingPreset tp = (TaggingPreset)value;
            result.setText(tp.getName());
            result.setIcon((Icon) tp.getValue(Action.SMALL_ICON));
            return result;
        }
    }

    private static class ResultListModel extends AbstractListModel {

        private List<PresetClasification> presets = new ArrayList<PresetClasification>();

        public void setPresets(List<PresetClasification> presets) {
            this.presets = presets;
            fireContentsChanged(this, 0, Integer.MAX_VALUE);
        }

        public List<PresetClasification> getPresets() {
            return presets;
        }

        @Override
        public Object getElementAt(int index) {
            return presets.get(index).preset;
        }

        @Override
        public int getSize() {
            return presets.size();
        }

    }

    private static class PresetClasification implements Comparable<PresetClasification> {
        public final TaggingPreset preset;
        public int classification;
        public int favoriteIndex;
        private final Collection<String> groups = new HashSet<String>();
        private final Collection<String> names = new HashSet<String>();
        private final Collection<String> tags = new HashSet<String>();

        PresetClasification(TaggingPreset preset) {
            this.preset = preset;
            TaggingPreset group = preset.group;
            while (group != null) {
                for (String word: group.getLocaleName().toLowerCase().split("\\s")) {
                    groups.add(word);
                }
                group = group.group;
            }
            for (String word: preset.getLocaleName().toLowerCase().split("\\s")) {
                names.add(word);
            }
            for (Item item: preset.data) {
                if (item instanceof TaggingPreset.KeyedItem) {
                    tags.add(((TaggingPreset.KeyedItem) item).key);
                    // Should combo values also be added?
                    if (item instanceof Key && ((Key) item).value != null) {
                        tags.add(((Key) item).value);
                    }
                } else if (item instanceof Roles) {
                    for (Role role : ((Roles) item).roles) {
                        tags.add(role.key);
                    }
                }
            }
        }

        private int isMatching(Collection<String> values, String[] searchString) {
            int sum = 0;
            for (String word: searchString) {
                boolean found = false;
                boolean foundFirst = false;
                for (String value: values) {
                    int index = value.indexOf(word);
                    if (index == 0) {
                        foundFirst = true;
                        break;
                    } else if (index > 0) {
                        found = true;
                    }
                }
                if (foundFirst) {
                    sum += 2;
                } else if (found) {
                    sum += 1;
                } else
                    return 0;
            }
            return sum;
        }

        int isMatchingGroup(String[] words) {
            return isMatching(groups, words);
        }

        int isMatchingName(String[] words) {
            return isMatching(names, words);
        }

        int isMatchingTags(String[] words) {
            return isMatching(tags, words);
        }

        @Override
        public int compareTo(PresetClasification o) {
            int result = o.classification - classification;
            if (result == 0)
                return preset.getName().compareTo(o.preset.getName());
            else
                return result;
        }

        @Override
        public String toString() {
            return classification + " " + preset.toString();
        }
    }

    private static TaggingPresetSearchDialog instance;
    public static TaggingPresetSearchDialog getInstance() {
        if (instance == null) {
            instance = new TaggingPresetSearchDialog();
        }
        return instance;
    }

    private JosmTextField edSearchText;
    private JList lsResult;
    private JCheckBox ckOnlyApplicable;
    private JCheckBox ckSearchInTags;
    private final EnumSet<PresetType> typesInSelection = EnumSet.noneOf(PresetType.class);
    private boolean typesInSelectionDirty = true;
    private final List<PresetClasification> classifications = new ArrayList<PresetClasification>();
    private ResultListModel lsResultModel = new ResultListModel();

    private TaggingPresetSearchDialog() {
        super(Main.parent, tr("Presets"), new String[] {tr("Select"), tr("Cancel")});
        DataSet.addSelectionListener(this);

        for (TaggingPreset preset: TaggingPresetPreference.taggingPresets) {
            if (preset instanceof TaggingPresetSeparator || preset instanceof TaggingPresetMenu) {
                continue;
            }

            classifications.add(new PresetClasification(preset));
        }

        build();
        filterPresets();
    }

    @Override
    public void selectionChanged(Collection<? extends OsmPrimitive> newSelection) {
        typesInSelectionDirty = true;
    }

    @Override
    public ExtendedDialog showDialog() {

        ckOnlyApplicable.setEnabled(!getTypesInSelection().isEmpty());
        ckOnlyApplicable.setSelected(!getTypesInSelection().isEmpty() && ONLY_APPLICABLE.get());
        edSearchText.setText("");
        filterPresets();

        super.showDialog();
        lsResult.getSelectionModel().clearSelection();
        return this;
    }

    private void build() {
        JPanel content = new JPanel();
        content.setLayout(new BorderLayout());

        edSearchText = new JosmTextField();
        edSearchText.getDocument().addDocumentListener(new DocumentListener() {

            @Override
            public void removeUpdate(DocumentEvent e) {
                filterPresets();
            }

            @Override
            public void insertUpdate(DocumentEvent e) {
                filterPresets();

            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                filterPresets();

            }
        });
        edSearchText.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                switch (e.getKeyCode()) {
                case KeyEvent.VK_DOWN:
                    selectPreset(lsResult.getSelectedIndex() + 1);
                    break;
                case KeyEvent.VK_UP:
                    selectPreset(lsResult.getSelectedIndex() - 1);
                    break;
                case KeyEvent.VK_PAGE_DOWN:
                    selectPreset(lsResult.getSelectedIndex() + 10);
                    break;
                case KeyEvent.VK_PAGE_UP:
                    selectPreset(lsResult.getSelectedIndex() - 10);
                    break;
                case KeyEvent.VK_HOME:
                    selectPreset(0);
                    break;
                case KeyEvent.VK_END:
                    selectPreset(lsResultModel.getSize());
                    break;
                }
            }
        });
        content.add(edSearchText, BorderLayout.NORTH);

        lsResult = new JList();
        lsResult.setModel(lsResultModel);
        lsResult.setCellRenderer(new ResultListCellRenderer());
        lsResult.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount()>1) {
                    buttonAction(0, null);
                }
            }
        });
        content.add(new JScrollPane(lsResult), BorderLayout.CENTER);

        JPanel pnChecks = new JPanel();
        pnChecks.setLayout(new BoxLayout(pnChecks, BoxLayout.Y_AXIS));

        ckOnlyApplicable = new JCheckBox();
        ckOnlyApplicable.setText(tr("Show only applicable to selection"));
        pnChecks.add(ckOnlyApplicable);
        ckOnlyApplicable.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                filterPresets();
            }
        });

        ckSearchInTags = new JCheckBox();
        ckSearchInTags.setText(tr("Search in tags"));
        ckSearchInTags.setSelected(SEARCH_IN_TAGS.get());
        ckSearchInTags.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                filterPresets();
            }
        });
        pnChecks.add(ckSearchInTags);

        content.add(pnChecks, BorderLayout.SOUTH);

        content.setPreferredSize(new Dimension(400, 300));
        setContent(content);
    }

    private void selectPreset(int newIndex) {
        if (newIndex < 0) {
            newIndex = 0;
        }
        if (newIndex > lsResultModel.getSize() - 1) {
            newIndex = lsResultModel.getSize() - 1;
        }
        lsResult.setSelectedIndex(newIndex);
        lsResult.ensureIndexIsVisible(newIndex);
    }

    /**
     * Search expression can be in form: "group1/group2/name" where names can contain multiple words
     *
     * When groups are given,
     *
     *
     * @param text
     */
    private void filterPresets() {
        //TODO Save favorites to file
        String text = edSearchText.getText().toLowerCase();

        String[] groupWords;
        String[] nameWords;

        if (text.contains("/")) {
            groupWords = text.substring(0, text.lastIndexOf('/')).split("[\\s/]");
            nameWords = text.substring(text.indexOf('/') + 1).split("\\s");
        } else {
            groupWords = null;
            nameWords = text.split("\\s");
        }

        boolean onlyApplicable = ckOnlyApplicable.isSelected();
        boolean inTags = ckSearchInTags.isSelected();

        List<PresetClasification> result = new ArrayList<PresetClasification>();
        PRESET_LOOP:
            for (PresetClasification presetClasification: classifications) {
                TaggingPreset preset = presetClasification.preset;
                presetClasification.classification = 0;

                if (onlyApplicable && preset.types != null) {
                    boolean found = false;
                    for (PresetType type: preset.types) {
                        if (getTypesInSelection().contains(type)) {
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        continue;
                    }
                }



                if (groupWords != null && presetClasification.isMatchingGroup(groupWords) == 0) {
                    continue PRESET_LOOP;
                }

                int matchName = presetClasification.isMatchingName(nameWords);

                if (matchName == 0) {
                    if (groupWords == null) {
                        int groupMatch = presetClasification.isMatchingGroup(nameWords);
                        if (groupMatch > 0) {
                            presetClasification.classification = CLASSIFICATION_GROUP_MATCH + groupMatch;
                        }
                    }
                    if (presetClasification.classification == 0 && inTags) {
                        int tagsMatch = presetClasification.isMatchingTags(nameWords);
                        if (tagsMatch > 0) {
                            presetClasification.classification = CLASSIFICATION_TAGS_MATCH + tagsMatch;
                        }
                    }
                } else {
                    presetClasification.classification = CLASSIFICATION_NAME_MATCH + matchName;
                }

                if (presetClasification.classification > 0) {
                    presetClasification.classification += presetClasification.favoriteIndex;
                    result.add(presetClasification);
                }
            }

        Collections.sort(result);
        lsResultModel.setPresets(result);
        if (!buttons.isEmpty()) {
            buttons.get(0).setEnabled(!result.isEmpty());
        }
    }

    private EnumSet<PresetType> getTypesInSelection() {
        if (typesInSelectionDirty) {
            synchronized (typesInSelection) {
                typesInSelectionDirty = false;
                typesInSelection.clear();
                for (OsmPrimitive primitive : Main.main.getCurrentDataSet().getSelected()) {
                    if (primitive instanceof Node) {
                        typesInSelection.add(PresetType.NODE);
                    } else if (primitive instanceof Way) {
                        typesInSelection.add(PresetType.WAY);
                        if (((Way) primitive).isClosed()) {
                            typesInSelection.add(PresetType.CLOSEDWAY);
                        }
                    } else if (primitive instanceof Relation) {
                        typesInSelection.add(PresetType.RELATION);
                    }
                }
            }
        }
        return typesInSelection;
    }

    @Override
    protected void buttonAction(int buttonIndex, ActionEvent evt) {
        super.buttonAction(buttonIndex, evt);
        if (buttonIndex == 0) {
            int selectPreset = lsResult.getSelectedIndex();
            if (selectPreset == -1) {
                selectPreset = 0;
            }
            TaggingPreset preset = lsResultModel.getPresets().get(selectPreset).preset;
            for (PresetClasification pc: classifications) {
                if (pc.preset == preset) {
                    pc.favoriteIndex = CLASSIFICATION_IN_FAVORITES;
                } else if (pc.favoriteIndex > 0) {
                    pc.favoriteIndex--;
                }
            }
            preset.actionPerformed(null);
        }

        SEARCH_IN_TAGS.put(ckSearchInTags.isSelected());
        if (ckOnlyApplicable.isEnabled()) {
            ONLY_APPLICABLE.put(ckOnlyApplicable.isSelected());
        }
    }

}
