// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.history;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Point;
import java.awt.Rectangle;

import javax.swing.JTable;
import javax.swing.ListSelectionModel;

import org.openstreetmap.josm.data.osm.PrimitiveId;
import org.openstreetmap.josm.data.osm.RelationMemberData;
import org.openstreetmap.josm.data.osm.SimplePrimitiveId;
import org.openstreetmap.josm.gui.widgets.PopupMenuLauncher;

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
        final DiffTableModel tableModel = model.getRelationMemberTableModel(pointInTimeType);
        final RelationMemberTableColumnModel columnModel = new RelationMemberTableColumnModel();
        final JTable table = new JTable(tableModel, columnModel);
        tableModel.addTableModelListener(new ReversedChangeListener(
                table, columnModel, tr("The members of this relation are in reverse order")));
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        selectionSynchronizer.participateInSynchronizedSelection(table.getSelectionModel());
        table.getTableHeader().setReorderingAllowed(false);
        table.addMouseListener(new InternalPopupMenuLauncher());
        table.getModel().addTableModelListener(e -> {
            Rectangle rect = table.getCellRect(((DiffTableModel) e.getSource()).getFirstChange(), 0, true);
            table.scrollRectToVisible(rect);
        });
        table.addMouseListener(new ShowHistoryAction.DoubleClickAdapter(e -> {
            int row = table.rowAtPoint(e.getPoint());
            return primitiveIdAtRow(tableModel, row);
        }));
        return table;
    }

    /**
     * Constructs a new {@code RelationMemberListViewer}.
     * @param model The history browsing model
     */
    public RelationMemberListViewer(HistoryBrowserModel model) {
        super(model);
    }

    private static PrimitiveId primitiveIdAtRow(DiffTableModel model, int row) {
        if (row < 0)
            return null;
        RelationMemberData rm = (RelationMemberData) model.getValueAt(row, 0).value;
        if (rm == null)
            return null;
        return new SimplePrimitiveId(rm.getUniqueId(), rm.getType());
    }

    static class InternalPopupMenuLauncher extends PopupMenuLauncher {
        InternalPopupMenuLauncher() {
            super(new ListPopupMenu(tr("Zoom to member"), tr("Zoom to this member in the current data layer")));

        }

        @Override
        protected int checkTableSelection(JTable table, Point p) {
            int row = super.checkTableSelection(table, p);
            ((ListPopupMenu) menu).prepare(primitiveIdAtRow((DiffTableModel) table.getModel(), row));
            return row;
        }
    }

}
