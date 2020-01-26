// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.history;

import java.awt.Rectangle;

import javax.swing.JTable;
import javax.swing.ListSelectionModel;

/**
 * RelationMemberListViewer is a UI component which displays the  list of relation members of two
 * version of a {@link org.openstreetmap.josm.data.osm.Relation} in a {@link org.openstreetmap.josm.data.osm.history.History}.
 *
 * <ul>
 *   <li>on the left, it displays the list of relation members for the version at {@link PointInTimeType#REFERENCE_POINT_IN_TIME}</li>
 *   <li>on the right, it displays the list of relation members for the version at {@link PointInTimeType#CURRENT_POINT_IN_TIME}</li>
 * </ul>
 * @since 1709
 */
public class RelationMemberListViewer extends HistoryViewerPanel {

    @Override
    protected JTable buildTable(PointInTimeType pointInTimeType) {
        JTable table = new JTable(
                model.getRelationMemberTableModel(pointInTimeType),
                new RelationMemberTableColumnModel());
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        selectionSynchronizer.participateInSynchronizedSelection(table.getSelectionModel());
        table.getModel().addTableModelListener(e -> {
            Rectangle rect = table.getCellRect(((DiffTableModel) e.getSource()).getFirstChange(), 0, true);
            table.scrollRectToVisible(rect);
        });
        return table;
    }

    /**
     * Constructs a new {@code RelationMemberListViewer}.
     * @param model The history browsing model
     */
    public RelationMemberListViewer(HistoryBrowserModel model) {
        super(model);
    }
}
