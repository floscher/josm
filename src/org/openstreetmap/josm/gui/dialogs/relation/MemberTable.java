// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.relation;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Container;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Arrays;
import java.util.Collection;

import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JPopupMenu;
import javax.swing.JTable;
import javax.swing.JViewport;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.AutoScaleAction;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.gui.MapView.LayerChangeListener;
import org.openstreetmap.josm.gui.dialogs.relation.WayConnectionType.Direction;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;

public class MemberTable extends JTable implements IMemberModelListener {

    /**
     * the data layer in whose context relation members are edited in this table
     */
    protected OsmDataLayer layer;

    /** the popup menu */
    protected JPopupMenu popupMenu;
    private ZoomToAction zoomToAction;
    private ZoomToGapAction zoomToGap;

    /**
     * constructor
     *
     * @param model
     * @param columnModel
     */
    public MemberTable(OsmDataLayer layer, MemberTableModel model) {
        super(model, new MemberTableColumnModel(layer.data), model.getSelectionModel());
        this.layer = layer;
        model.addMemberModelListener(this);
        init();

    }

    /**
     * initialize the table
     */
    protected void init() {
        MemberRoleCellEditor ce = (MemberRoleCellEditor)getColumnModel().getColumn(0).getCellEditor();  
        setRowHeight(ce.getEditor().getPreferredSize().height);
        setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);

        // make ENTER behave like TAB
        //
        getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(
                KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0, false), "selectNextColumnCell");

        // install custom navigation actions
        //
        getActionMap().put("selectNextColumnCell", new SelectNextColumnCellAction());
        getActionMap().put("selectPreviousColumnCell", new SelectPreviousColumnCellAction());

        addMouseListener(new PopupListener());
        addMouseListener(new DblClickHandler());
    }

    @Override
    public Dimension getPreferredSize(){
        Container c = getParent();
        while(c != null && ! (c instanceof JViewport)) {
            c = c.getParent();
        }
        if (c != null) {
            Dimension d = super.getPreferredSize();
            d.width = c.getSize().width;
            return d;
        }
        return super.getPreferredSize();
    }

    public void makeMemberVisible(int index) {
        scrollRectToVisible(getCellRect(index, 0, true));
    }

    /**
     * Action to be run when the user navigates to the next cell in the table, for instance by
     * pressing TAB or ENTER. The action alters the standard navigation path from cell to cell: <ul>
     * <li>it jumps over cells in the first column</li> <li>it automatically add a new empty row
     * when the user leaves the last cell in the table</li> <ul>
     *
     *
     */
    class SelectNextColumnCellAction extends AbstractAction {
        public void actionPerformed(ActionEvent e) {
            run();
        }

        public void run() {
            int col = getSelectedColumn();
            int row = getSelectedRow();
            if (getCellEditor() != null) {
                getCellEditor().stopCellEditing();
            }

            if (col == 0 && row < getRowCount() - 1) {
                row++;
            } else if (row < getRowCount() - 1) {
                col = 0;
                row++;
            }
            changeSelection(row, col, false, false);
        }
    }

    /**
     * Action to be run when the user navigates to the previous cell in the table, for instance by
     * pressing Shift-TAB
     *
     */
    private class SelectPreviousColumnCellAction extends AbstractAction {

        public void actionPerformed(ActionEvent e) {
            int col = getSelectedColumn();
            int row = getSelectedRow();
            if (getCellEditor() != null) {
                getCellEditor().stopCellEditing();
            }

            if (col <= 0 && row <= 0) {
                // change nothing
            } else if (row > 0) {
                col = 0;
                row--;
            }
            changeSelection(row, col, false, false);
        }
    }

    /**
     * Replies the popup menu for this table
     *
     * @return the popup menu
     */
    protected JPopupMenu getPopUpMenu() {
        if (popupMenu == null) {
            popupMenu = new JPopupMenu();
            zoomToAction = new ZoomToAction();
            MapView.addLayerChangeListener(zoomToAction);
            getSelectionModel().addListSelectionListener(zoomToAction);
            popupMenu.add(zoomToAction);
            zoomToGap = new ZoomToGapAction();
            MapView.addLayerChangeListener(zoomToGap);
            getSelectionModel().addListSelectionListener(zoomToGap);
            popupMenu.add(zoomToGap);
            popupMenu.addSeparator();
            popupMenu.add(new SelectPreviousGapAction());
            popupMenu.add(new SelectNextGapAction());
        }
        return popupMenu;
    }

    public void unlinkAsListener() {
        MapView.removeLayerChangeListener(zoomToAction);
        MapView.removeLayerChangeListener(zoomToGap);
    }

    class PopupListener extends MouseAdapter {
        @Override
        public void mousePressed(MouseEvent e) {
            showPopup(e);
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            showPopup(e);
        }

        private void showPopup(MouseEvent e) {
            if (e.isPopupTrigger()) {
                getPopUpMenu().show(e.getComponent(), e.getX(), e.getY());
            }
        }
    }

    private class ZoomToAction extends AbstractAction implements LayerChangeListener, ListSelectionListener {
        public ZoomToAction() {
            putValue(NAME, tr("Zoom to"));
            putValue(SHORT_DESCRIPTION, tr("Zoom to the object the first selected member refers to"));
            updateEnabledState();
        }

        public void actionPerformed(ActionEvent e) {
            if (! isEnabled())
                return;
            int rows[] = getSelectedRows();
            if (rows == null || rows.length == 0)
                return;
            int row = rows[0];
            OsmPrimitive primitive = getMemberTableModel().getReferredPrimitive(row);
            layer.data.setSelected(primitive);
            AutoScaleAction.autoScale("selection");
        }

        protected void updateEnabledState() {
            if (Main.main == null || Main.main.getEditLayer() != layer) {
                setEnabled(false);
                putValue(SHORT_DESCRIPTION, tr("Zooming disabled because layer of this relation is not active"));
                return;
            }
            if (getSelectedRowCount() == 0) {
                setEnabled(false);
                putValue(SHORT_DESCRIPTION, tr("Zooming disabled because there is no selected member"));
                return;
            }
            setEnabled(true);
            putValue(SHORT_DESCRIPTION, tr("Zoom to the object the first selected member refers to"));
        }

        public void valueChanged(ListSelectionEvent e) {
            updateEnabledState();
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
    }

    private class SelectPreviousGapAction extends AbstractAction {

        public SelectPreviousGapAction() {
            putValue(NAME, tr("Select previous Gap"));
            putValue(SHORT_DESCRIPTION, tr("Select the previous relation member which gives rise to a gap"));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            int i = getSelectedRow() - 1;
            while (i >= 0 && getMemberTableModel().getWayConnection(i).linkPrev) {
                i--;
            }
            if (i >= 0) {
                getSelectionModel().setSelectionInterval(i, i);
            }
        }
    }

    private class SelectNextGapAction extends AbstractAction {

        public SelectNextGapAction() {
            putValue(NAME, tr("Select next Gap"));
            putValue(SHORT_DESCRIPTION, tr("Select the next relation member which gives rise to a gap"));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            int i = getSelectedRow() + 1;
            while (i < getRowCount() && getMemberTableModel().getWayConnection(i).linkNext) {
                i++;
            }
            if (i < getRowCount()) {
                getSelectionModel().setSelectionInterval(i, i);
            }
        }
    }

    private class ZoomToGapAction extends AbstractAction implements LayerChangeListener, ListSelectionListener {

        public ZoomToGapAction() {
            putValue(NAME, tr("Zoom to Gap"));
            putValue(SHORT_DESCRIPTION, tr("Zoom to the gap in the way sequence"));
            updateEnabledState();
        }

        private WayConnectionType getConnectionType() {
            return getMemberTableModel().getWayConnection(getSelectedRows()[0]);
        }

        private final Collection<Direction> connectionTypesOfInterest = Arrays.asList(WayConnectionType.Direction.FORWARD, WayConnectionType.Direction.BACKWARD);

        private boolean hasGap() {
            WayConnectionType connectionType = getConnectionType();
            return connectionTypesOfInterest.contains(connectionType.direction)
                    && !(connectionType.linkNext && connectionType.linkPrev);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            WayConnectionType connectionType = getConnectionType();
            Way way = (Way) getMemberTableModel().getReferredPrimitive(getSelectedRows()[0]);
            if (!connectionType.linkPrev) {
                layer.data.setSelected(WayConnectionType.Direction.FORWARD.equals(connectionType.direction)
                        ? way.firstNode() : way.lastNode());
                AutoScaleAction.autoScale("selection");
            } else if (!connectionType.linkNext) {
                layer.data.setSelected(WayConnectionType.Direction.FORWARD.equals(connectionType.direction)
                        ? way.lastNode() : way.firstNode());
                AutoScaleAction.autoScale("selection");
            }
        }

        private void updateEnabledState() {
            setEnabled(Main.main != null
                    && Main.main.getEditLayer() == layer
                    && getSelectedRowCount() == 1
                    && hasGap());
        }

        @Override
        public void valueChanged(ListSelectionEvent e) {
            updateEnabledState();
        }

        @Override
        public void activeLayerChange(Layer oldLayer, Layer newLayer) {
            updateEnabledState();
        }

        @Override
        public void layerAdded(Layer newLayer) {
            updateEnabledState();
        }

        @Override
        public void layerRemoved(Layer oldLayer) {
            updateEnabledState();
        }
    }

    protected MemberTableModel getMemberTableModel() {
        return (MemberTableModel) getModel();
    }

    private class DblClickHandler extends MouseAdapter {

        protected void setSelection(MouseEvent e) {
            int row = rowAtPoint(e.getPoint());
            if (row < 0) return;
            OsmPrimitive primitive = getMemberTableModel().getReferredPrimitive(row);
            getMemberTableModel().getLayer().data.setSelected(primitive.getPrimitiveId());
        }

        protected void addSelection(MouseEvent e) {
            int row = rowAtPoint(e.getPoint());
            if (row < 0) return;
            OsmPrimitive primitive = getMemberTableModel().getReferredPrimitive(row);
            getMemberTableModel().getSelectionModel().addSelectionInterval(row, row);
            getMemberTableModel().getLayer().data.addSelected(primitive.getPrimitiveId());

        }

        @Override
        public void mouseClicked(MouseEvent e) {
            if (SwingUtilities.isLeftMouseButton(e) && e.getClickCount() > 1) {
                if (e.isControlDown()) {
                    addSelection(e);
                } else {
                    setSelection(e);
                }
            }
        }
    }
}
