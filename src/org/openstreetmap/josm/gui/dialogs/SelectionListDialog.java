package org.openstreetmap.josm.gui.dialogs;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.Collection;

import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;

import org.openstreetmap.josm.data.SelectionChangedListener;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.visitor.SelectionComponentVisitor;
import org.openstreetmap.josm.gui.ImageProvider;
import org.openstreetmap.josm.gui.Main;
import org.openstreetmap.josm.gui.MapFrame;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.gui.MapView.LayerChangeListener;
import org.openstreetmap.josm.gui.layer.Layer;

/**
 * A small tool dialog for displaying the current selection. The selection manager
 * respects clicks into the selection list. Ctrl-click will remove entries from
 * the list while single click will make the clicked entry the only selection.
 * 
 * @author imi
 */
public class SelectionListDialog extends ToggleDialog implements SelectionChangedListener, LayerChangeListener {

	/**
	 * The selection's list data.
	 */
	private final DefaultListModel list = new DefaultListModel();
	/**
	 * The display list.
	 */
	private JList displaylist = new JList(list);
	/**
	 * The dataset, all selections are part of.
	 */
	private final MapView mapView;
	
	/**
	 * Create a SelectionList dialog.
	 * @param mapView The mapView to get the dataset from.
	 */
	public SelectionListDialog(MapFrame mapFrame) {
		super(mapFrame, "Current Selection", "Selection List", "selectionlist", KeyEvent.VK_E, "Open a selection list window.");
		this.mapView = mapFrame.mapView;
		setLayout(new BorderLayout());
		setSize(300,400);
		displaylist.setCellRenderer(new DefaultListCellRenderer(){
			private SelectionComponentVisitor visitor = new SelectionComponentVisitor();
			@Override
			public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
				Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
				if (c instanceof JLabel && value != null) {
					((OsmPrimitive)value).visit(visitor);
					((JLabel)c).setText(visitor.name);
					((JLabel)c).setIcon(visitor.icon);
				}
				return c;
			}
		});
		displaylist.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

		getContentPane().add(new JScrollPane(displaylist), BorderLayout.CENTER);

		JButton button = new JButton("Select", ImageProvider.get("mapmode", "selection"));
		button.setToolTipText("Set the selected elements on the map to the selected items in the list above.");
		button.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				updateMap();
			}
		});
		getContentPane().add(button, BorderLayout.SOUTH);

		selectionChanged(mapView.getActiveDataSet().getSelected());
	}

	@Override
	public void setVisible(boolean b) {
		if (b) {
			mapView.addLayerChangeListener(this);
			mapView.getActiveDataSet().addSelectionChangedListener(this);
			selectionChanged(mapView.getActiveDataSet().getSelected());
		} else {
			mapView.removeLayerChangeListener(this);
			mapView.getActiveDataSet().removeSelectionChangedListener(this);
		}
		super.setVisible(b);
	}



	/**
	 * Called when the selection in the dataset changed.
	 * @param newSelection The new selection array.
	 */
	public void selectionChanged(Collection<OsmPrimitive> newSelection) {
		list.removeAllElements();
		list.setSize(newSelection.size());
		int i = 0;
		for (OsmPrimitive osm : newSelection)
			list.setElementAt(osm, i++);
	}

	/**
	 * Sets the selection of the map to the current selected items.
	 */
	public void updateMap() {
		DataSet ds = mapView.getActiveDataSet();
		ds.clearSelection();
		for (int i = 0; i < list.getSize(); ++i)
			if (displaylist.isSelectedIndex(i))
				((OsmPrimitive)list.get(i)).setSelected(true, ds);
		Main.main.getMapFrame().repaint();
	}

	public void activeLayerChange(Layer oldLayer, Layer newLayer) {
		DataSet ds = oldLayer.getDataSet();
		if (ds != null)
			ds.removeSelectionChangedListener(this);
		ds = newLayer.getDataSet();
		if (ds != null)
			ds.addSelectionChangedListener(this);
	}

	/**
	 * Does nothing. Only to satisfy LayerChangeListener
	 */
	public void layerAdded(Layer newLayer) {}
	/**
	 * Does nothing. Only to satisfy LayerChangeListener
	 */
	public void layerRemoved(Layer oldLayer) {}
}
