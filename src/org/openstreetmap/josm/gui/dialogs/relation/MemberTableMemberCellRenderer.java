// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.relation;

import java.awt.Component;
import java.awt.Dimension;

import javax.swing.JTable;

import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.gui.DefaultNameFormatter;
import org.openstreetmap.josm.tools.ImageProvider;

/**
 * "Refers to" column in relation editor's member list.
 */
public class MemberTableMemberCellRenderer extends MemberTableCellRenderer {

    /**
     * Constructs a new {@code MemberTableMemberCellRenderer}.
     */
    public MemberTableMemberCellRenderer() {
        super();
    }

    protected void renderPrimitive(OsmPrimitive primitive, Dimension cellSize) {
        // Make icon the full height of the table cell. Icon background is square.
        setIcon(ImageProvider.getPadded(primitive, cellSize));
        setText(primitive.getDisplayName(DefaultNameFormatter.getInstance()));
        setToolTipText(DefaultNameFormatter.getInstance().buildDefaultToolTip(primitive));
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
            int row, int column) {

        reset();
        if (value == null)
            return this;

        Dimension cellSize = table.getCellRect(row, column, false).getSize();

        OsmPrimitive primitive = (OsmPrimitive) value;
        renderBackgroundForeground(getModel(table), primitive, isSelected);
        renderPrimitive(primitive, cellSize);
        return this;
    }
}
