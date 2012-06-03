// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.conflict.pair.relation;

import java.awt.Component;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;

import javax.swing.AbstractCellEditor;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.TableCellEditor;

import org.openstreetmap.josm.data.osm.RelationMember;

/**
 * {@link TableCellEditor} for the role column in a table for {@link RelationMember}s.
 *
 */
public class RelationMemberTableCellEditor extends AbstractCellEditor implements TableCellEditor{

    private final JTextField editor;

    public RelationMemberTableCellEditor() {
        editor = new JTextField();
        editor.addFocusListener(
                new FocusAdapter() {
                    @Override
                    public void focusGained(FocusEvent arg0) {
                        editor.selectAll();
                    }
                }
        );
    }

    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
        // Do not edit empty or incomplete members ! (fix #5374 and #6315)
        if (value == null)
            return null;

        RelationMember member = (RelationMember)value;

        editor.setText(member.getRole());
        editor.selectAll();
        return editor;
    }

    public Object getCellEditorValue() {
        return editor.getText();
    }

}
