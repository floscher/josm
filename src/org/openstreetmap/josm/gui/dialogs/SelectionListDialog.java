package org.openstreetmap.josm.gui.dialogs;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.Collection;

import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;

import org.openstreetmap.josm.data.SelectionChangedListener;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.visitor.SelectionComponentVisitor;
import org.openstreetmap.josm.gui.Main;

/**
 * A small tool dialog for displaying the current selection. The selection manager
 * respects clicks into the selection list. Ctrl-click will remove entries from
 * the list while single click will make the clicked entry the only selection.
 * 
 * @author imi
 */
public class SelectionListDialog extends ToggleDialog implements SelectionChangedListener {

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
	private final DataSet dataSet;
	
	/**
	 * Create a SelectionList dialog.
	 * @param dataSet The dataset this dialog operates on.
	 */
	public SelectionListDialog(DataSet dataSet) {
		super("Current Selection", "Selection List", "selectionlist", KeyEvent.VK_E, "Open a selection list window.");
		this.dataSet = dataSet;
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

		JButton button = new JButton("Select", new ImageIcon("images/mapmode/selection.png"));
		button.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				updateMap();
			}
		});
		getContentPane().add(button, BorderLayout.SOUTH);

		selectionChanged(dataSet.getSelected());
	}

	@Override
	public void setVisible(boolean b) {
		if (b) {
			dataSet.addSelectionChangedListener(this);
			selectionChanged(dataSet.getSelected());
		} else
			dataSet.removeSelectionChangedListener(this);
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
		dataSet.clearSelection();
		for (int i = 0; i < list.getSize(); ++i)
			if (displaylist.isSelectedIndex(i))
				((OsmPrimitive)list.get(i)).setSelected(true, dataSet);
		Main.main.getMapFrame().repaint();
	}
}
