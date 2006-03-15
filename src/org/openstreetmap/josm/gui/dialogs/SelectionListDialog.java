package org.openstreetmap.josm.gui.dialogs;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Collection;
import java.util.Map.Entry;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.SelectionChangedListener;
import org.openstreetmap.josm.data.osm.Key;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.gui.ImageProvider;
import org.openstreetmap.josm.gui.MapFrame;
import org.openstreetmap.josm.gui.OsmPrimitivRenderer;

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
	 * Create a SelectionList dialog.
	 * @param mapView The mapView to get the dataset from.
	 */
	public SelectionListDialog(MapFrame mapFrame) {
		super("Current Selection", "Selection List", "selectionlist", KeyEvent.VK_E, "Open a selection list window.");
		setPreferredSize(new Dimension(320,150));
		displaylist.setCellRenderer(new OsmPrimitivRenderer());
		displaylist.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		displaylist.addMouseListener(new MouseAdapter(){
			@Override
			public void mouseClicked(MouseEvent e) {
				if (e.getClickCount() < 2)
					return;
				updateMap();
			}
		});

		add(new JScrollPane(displaylist), BorderLayout.CENTER);

		JPanel buttonPanel = new JPanel(new GridLayout(1,2));
		
		JButton button = new JButton("Select", ImageProvider.get("mapmode", "selection"));
		button.setToolTipText("Set the selected elements on the map to the selected items in the list above.");
		button.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				updateMap();
			}
		});
		buttonPanel.add(button);

		button = new JButton("Search", ImageProvider.get("dialogs", "search"));
		button.setToolTipText("Search for objects.");
		button.addActionListener(new ActionListener(){
			private String lastSearch = "";
			public void actionPerformed(ActionEvent e) {
				JLabel l = new JLabel("Please enter a search string.");
				l.setToolTipText("<html>Fulltext search.<ul>" +
						"<li>Baker Street  - search for everything with 'Baker Street' in any key or name.</li>" +
						"<li>name:Bak  - search for every object with key=name and 'Bak' anywhere in the value.</li>" +
						"<li>foot:  - search for everything with the key=foot set to any value." +
						"</ul></html>");
				lastSearch = (String)JOptionPane.showInputDialog(Main.main,l,"Search",JOptionPane.INFORMATION_MESSAGE,null,null,lastSearch);
				if (lastSearch == null)
					return;
				Main.main.ds.clearSelection();
				for (OsmPrimitive osm : Main.main.ds.allNonDeletedPrimitives()) {
					if (osm.keys != null) {
						for (Entry<Key, String> ent : osm.keys.entrySet()) {
							if (match(lastSearch, ent.getKey(), ent.getValue())) {
								osm.setSelected(true);
								break;
							}
						}
					}
				}
				selectionChanged(Main.main.ds.getSelected());
				Main.main.getMapFrame().repaint();
			}
			private boolean match(String search, Key key, String value) {
				int colon = search.indexOf(':');
				if (colon == -1)
					return key.name.indexOf(search) != -1 || value.indexOf(search) != -1;
				return key.name.equals(search.substring(0, colon)) && value.indexOf(search.substring(colon+1)) != -1;
			}
		});
		buttonPanel.add(button);
		
		add(buttonPanel, BorderLayout.SOUTH);
		selectionChanged(Main.main.ds.getSelected());
	}

	@Override
	public void setVisible(boolean b) {
		if (b) {
			Main.main.ds.addSelectionChangedListener(this);
			selectionChanged(Main.main.ds.getSelected());
		} else {
			Main.main.ds.removeSelectionChangedListener(this);
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
		Main.main.ds.clearSelection();
		for (int i = 0; i < list.getSize(); ++i)
			if (displaylist.isSelectedIndex(i))
				((OsmPrimitive)list.get(i)).setSelected(true);
		Main.main.getMapFrame().repaint();
	}
}
