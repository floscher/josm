// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.relation;

import java.awt.Component;
import java.util.logging.Logger;

import javax.swing.AbstractCellEditor;
import javax.swing.JTable;
import javax.swing.table.TableCellEditor;

import org.openstreetmap.josm.gui.dialogs.relation.ac.AutoCompletionCache;
import org.openstreetmap.josm.gui.dialogs.relation.ac.AutoCompletionItemPritority;
import org.openstreetmap.josm.gui.dialogs.relation.ac.AutoCompletionList;
import org.openstreetmap.josm.gui.dialogs.relation.ac.AutoCompletionListItem;

/**
 * This is the table cell editor for the tag editor dialog.
 * 
 */
@SuppressWarnings("serial")
public class TagCellEditor extends AbstractCellEditor implements TableCellEditor{

    /** the logger object */
    static private Logger logger = Logger.getLogger(TagCellEditor.class.getName());

    private TagFieldEditor editor = null;
    private TagModel currentTag = null;
    private TagEditorModel tagEditorModel = null;
    private int currentColumn = 0;

    /** the cache of auto completion items derived from the current JOSM data set */
    private AutoCompletionCache acCache = null;

    /** user input is matched against this list of auto completion items */
    private AutoCompletionList autoCompletionList = null;

    /**
     * constructor
     */
    public TagCellEditor() {
        editor = new TagFieldEditor();
        acCache = new AutoCompletionCache();
    }

    /**
     * initializes  the auto completion list when the table cell editor starts
     * to edit the key of a tag. In this case the auto completion list is
     * initialized with the set of standard key values and the set of current key
     * values from the the current JOSM data set. Keys already present in the
     * current tag model are removed from the auto completion list.
     * 
     * @param model  the tag editor model
     * @param currentTag  the current tag
     */
    protected void initAutoCompletionListForKeys(TagEditorModel model, TagModel currentTag) {

        if (autoCompletionList == null) {
            logger.warning("autoCompletionList is null. Make sure an instance of AutoCompletionList is injected into TableCellEditor.");
            return;
        }

        autoCompletionList.clear();

        // add the list of keys in the current data set
        //
        for (String key : acCache.getKeys()) {
            autoCompletionList.add(
                    new AutoCompletionListItem(key, AutoCompletionItemPritority.IS_IN_DATASET)
            );
        }

        // remove the keys already present in the current tag model
        //
        for (String key : model.getKeys()) {
            if (! key.equals(currentTag.getName())) {
                autoCompletionList.remove(key);
            }
        }
        autoCompletionList.fireTableDataChanged();
    }


    /**
     * initializes the auto completion list when the cell editor starts to edit
     * a tag value. In this case the auto completion list is initialized with the
     * set of standard values for a given key and the set of values present in the
     * current data set for the given key.
     * 
     * @param forKey the key
     */
    protected void initAutoCompletionListForValues(String forKey) {

        if (autoCompletionList == null) {
            logger.warning("autoCompletionList is null. Make sure an instance of AutoCompletionList is injected into TableCellEditor.");
            return;
        }
        autoCompletionList.clear();

        for (String value : acCache.getValues(forKey)) {
            autoCompletionList.add(
                    new AutoCompletionListItem(value, AutoCompletionItemPritority.IS_IN_DATASET)
            );
        }
    }


    /**
     * replies the table cell editor
     */
    public Component getTableCellEditorComponent(JTable table,
            Object value, boolean isSelected, int row, int column) {
        currentTag = (TagModel) value;

        if (column == 0) {
            editor.setText(currentTag.getName());
            currentColumn = 0;
            TagEditorModel model = (TagEditorModel)table.getModel();
            initAutoCompletionListForKeys(model, currentTag);
            return editor;
        } else if (column == 1) {

            if (currentTag.getValueCount() == 0) {
                editor.setText("");
            } else if (currentTag.getValueCount() == 1) {
                editor.setText(currentTag.getValues().get(0));
            } else {
                editor.setText("");
            }
            currentColumn = 1;
            initAutoCompletionListForValues(currentTag.getName());
            return editor;
        } else {
            logger.warning("column this table cell editor is requested for is out of range. column=" + column);
            return null;
        }
    }

    public Object getCellEditorValue() {
        return editor.getText();
    }

    @Override
    public void cancelCellEditing() {
        super.cancelCellEditing();
    }

    @Override
    public boolean stopCellEditing() {
        if (tagEditorModel == null) {
            logger.warning("no tag editor model set. Can't update edited values. Please set tag editor model first");
            return super.stopCellEditing();
        }

        if (currentColumn == 0) {
            tagEditorModel.updateTagName(currentTag, editor.getText());
        } else if (currentColumn == 1){
            if (currentTag.getValueCount() > 1 && ! editor.getText().equals("")) {
                tagEditorModel.updateTagValue(currentTag, editor.getText());
            } else if (currentTag.getValueCount() <= 1) {
                tagEditorModel.updateTagValue(currentTag, editor.getText());
            }
        }

        return super.stopCellEditing();
    }

    /**
     * replies the {@link AutoCompletionList} this table cell editor synchronizes with
     * 
     * @return the auto completion list
     */
    public AutoCompletionList getAutoCompletionList() {
        return autoCompletionList;
    }

    /**
     * sets the {@link AutoCompletionList} this table cell editor synchronizes with
     * @param autoCompletionList the auto completion list
     */
    public void setAutoCompletionList(AutoCompletionList autoCompletionList) {
        this.autoCompletionList = autoCompletionList;
        editor.setAutoCompletionList(autoCompletionList);
    }

    public void setAutoCompletionCache(AutoCompletionCache acCache) {
        this.acCache = acCache;
    }

    public void autoCompletionItemSelected(String item) {
        editor.setText(item);
        editor.selectAll();
        editor.requestFocus();
    }

    public TagFieldEditor getEditor() {
        return editor;
    }

    /**
     * sets the tag editor model
     * 
     * @param tagEditorModel  the tag editor model
     */
    public void setTagEditorModel(TagEditorModel tagEditorModel) {
        this.tagEditorModel = tagEditorModel;
    }
}
