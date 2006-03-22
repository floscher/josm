package org.openstreetmap.josm.gui.dialogs;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Vector;
import java.util.Map.Entry;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.command.ChangeKeyValueCommand;
import org.openstreetmap.josm.data.SelectionChangedListener;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.gui.ImageProvider;
import org.openstreetmap.josm.gui.MapFrame;
import org.openstreetmap.josm.gui.MapView;

/**
 * This dialog displays the properties of the current selected primitives.
 * 
 * If no object is selected, the dialog list is empty. 
 * If only one is selected, all properties of this object are selected.
 * If more than one object are selected, the sum of all properties are displayed. If the
 * different objects share the same property, the shared value is displayed. If they have
 * different values, all of them are put in a combo box and the string "&lt;different&gt;"
 * is displayed in italic.
 * 
 * Below the list, the user can click on an add, modify and delete property button to 
 * edit the table selection value.
 * 
 * The command is applied to all selected entries.
 * 
 * @author imi
 */
public class PropertiesDialog extends ToggleDialog implements SelectionChangedListener {

	/**
	 * Watches for double clicks and from editing or new property, depending on the
	 * location, the click was.
	 * @author imi
	 */
	public class DblClickWatch extends MouseAdapter {
		@Override
		public void mouseClicked(MouseEvent e) {
			if (e.getClickCount() < 2)
				return;
			if (e.getSource() instanceof JScrollPane)
				add();
			else {
				int row = propertyTable.rowAtPoint(e.getPoint());
				edit(row);
			}
		}
	}

	/**
	 * Edit the value in the table row
	 * @param row 	The row of the table, from which the value is edited. 
	 */
	void edit(int row) {
		String key = data.getValueAt(row, 0).toString();
		Collection<OsmPrimitive> sel = Main.main.ds.getSelected();
		String msg = "<html>This will change "+sel.size()+" object"+(sel.size()==1?"":"s")+".<br><br>"+
		"Please select a new value for '"+key+"'.<br>(Empty string deletes the key.)";
		final JComboBox combo = (JComboBox)data.getValueAt(row, 1);
		JPanel p = new JPanel(new BorderLayout());
		p.add(new JLabel(msg+"</html>"), BorderLayout.NORTH);
		p.add(combo, BorderLayout.CENTER);

		final JOptionPane optionPane = new JOptionPane(p, JOptionPane.QUESTION_MESSAGE, JOptionPane.OK_CANCEL_OPTION);
		final JDialog dlg = optionPane.createDialog(Main.main, "Change values?");
		dlg.addWindowFocusListener(new WindowFocusListener(){
			public void windowGainedFocus(WindowEvent e) {
				combo.requestFocusInWindow();
			}
			public void windowLostFocus(WindowEvent e) {
			}
		});
		combo.getEditor().addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				optionPane.setValue(JOptionPane.OK_OPTION);
				dlg.setVisible(false);
			}
		});
		String oldComboEntry = combo.getEditor().getItem().toString();
		dlg.setVisible(true);

		Object answer = optionPane.getValue();
		if (answer == null || answer == JOptionPane.UNINITIALIZED_VALUE ||
				(answer instanceof Integer && (Integer)answer != JOptionPane.OK_OPTION)) {
			combo.getEditor().setItem(oldComboEntry);
			return;
		}

		String value = combo.getEditor().getItem().toString();
		if (value.equals("<different>"))
			return;
		if (value.equals(""))
			value = null; // delete the key
		mv.editLayer().add(new ChangeKeyValueCommand(sel, key, value));

		if (value == null)
			selectionChanged(sel); // update whole table
		else
			PropertiesDialog.this.repaint(); // repaint is enough 
	}
	
	/**
	 * Open the add selection dialog and add a new key/value to the table (and to the
	 * dataset, of course).
	 */
	void add() {
		Collection<OsmPrimitive> sel = Main.main.ds.getSelected();
		
		JPanel p = new JPanel(new BorderLayout());
		p.add(new JLabel("<html>This will change "+sel.size()+" object"+(sel.size()==1?"":"s")+".<br><br>"+
		"Please select a key"), BorderLayout.NORTH);
		Vector<String> allKeys = new Vector<String>();
		for (OsmPrimitive osm : Main.main.ds.allNonDeletedPrimitives())
			allKeys.addAll(osm.keySet());
		for (Iterator<String> it = allKeys.iterator(); it.hasNext();) {
			String s = it.next();
			for (int i = 0; i < data.getRowCount(); ++i) {
				if (s.equals(data.getValueAt(i, 0))) {
					it.remove();
					break;
				}
			}
		}
		JComboBox keys = new JComboBox(allKeys);
		keys.setEditable(true);
		p.add(keys, BorderLayout.CENTER);
		
		JPanel p2 = new JPanel(new BorderLayout());
		p.add(p2, BorderLayout.SOUTH);
		p2.add(new JLabel("Please select a value"), BorderLayout.NORTH);
		JTextField values = new JTextField();
		p2.add(values, BorderLayout.CENTER);
		int answer = JOptionPane.showConfirmDialog(Main.main, p, 
				"Change values?", JOptionPane.OK_CANCEL_OPTION); 
		if (answer != JOptionPane.OK_OPTION)
			return;
		String key = keys.getEditor().getItem().toString();
		String value = values.getText();
		if (value.equals(""))
			return;
		mv.editLayer().add(new ChangeKeyValueCommand(sel, key, value));
		selectionChanged(sel); // update table
	}

	/**
	 * Delete the keys from the given row.
	 * @param row	The row, which key gets deleted from the dataset.
	 */
	private void delete(int row) {
		String key = data.getValueAt(row, 0).toString();
		Collection<OsmPrimitive> sel = Main.main.ds.getSelected();
		mv.editLayer().add(new ChangeKeyValueCommand(sel, key, null));
		selectionChanged(sel); // update table
	}
	
	/**
	 * The property data.
	 */
	private final DefaultTableModel data = new DefaultTableModel(){
		@Override
		public boolean isCellEditable(int row, int column) {
			return false;
		}
		@Override
		public Class<?> getColumnClass(int columnIndex) {
			return columnIndex == 1 ? JComboBox.class : String.class;
		}
	};
	/**
	 * The properties list.
	 */
	private final JTable propertyTable = new JTable(data);
	/**
	 * The map view this dialog operates on.
	 */
	private final MapView mv;
	
	/**
	 * Create a new PropertiesDialog
	 */
	public PropertiesDialog(MapFrame mapFrame) {
		super("Properties", "Properties Dialog", "properties", KeyEvent.VK_P, "Property for selected objects.");
		mv = mapFrame.mapView;

		setPreferredSize(new Dimension(320,150));
		
		data.setColumnIdentifiers(new String[]{"Key", "Value"});
		propertyTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		propertyTable.setDefaultRenderer(JComboBox.class, new DefaultTableCellRenderer(){
			@Override
			public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
				Component c = super.getTableCellRendererComponent(table, value, isSelected, false, row, column);
				if (c instanceof JLabel) {
					String str = ((JComboBox)value).getEditor().getItem().toString();
					((JLabel)c).setText(str);
					if (str.equals("<different>"))
						c.setFont(c.getFont().deriveFont(Font.ITALIC));
				}
				return c;
			}
		});
		propertyTable.setDefaultRenderer(String.class, new DefaultTableCellRenderer(){
			@Override
			public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
				return super.getTableCellRendererComponent(table, value, isSelected, false, row, column);
			}
		});
		propertyTable.addMouseListener(new DblClickWatch());

		JScrollPane scrollPane = new JScrollPane(propertyTable);
		scrollPane.addMouseListener(new DblClickWatch());
		add(scrollPane, BorderLayout.CENTER);
		
		JPanel buttonPanel = new JPanel(new GridLayout(1,3));
		ActionListener buttonAction = new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				int sel = propertyTable.getSelectedRow();
				if (e.getActionCommand().equals("Add"))
					add();
				else if (e.getActionCommand().equals("Edit")) {
					if (sel == -1)
						JOptionPane.showMessageDialog(Main.main, "Please select the row to edit.");
					else
						edit(sel);
				} else if (e.getActionCommand().equals("Delete")) {
					if (sel == -1)
						JOptionPane.showMessageDialog(Main.main, "Please select the row to delete.");
					else
						delete(sel);
				}
			}
		};
		buttonPanel.add(createButton("Add", "Add a new key/value pair to all objects", KeyEvent.VK_A, buttonAction));
		buttonPanel.add(createButton("Edit", "Edit the value of the selected key for all objects", KeyEvent.VK_E, buttonAction));
		buttonPanel.add(createButton("Delete", "Delete the selected key in all objects", KeyEvent.VK_D, buttonAction));
		add(buttonPanel, BorderLayout.SOUTH);
	}
	
	private JButton createButton(String name, String tooltip, int mnemonic, ActionListener actionListener) {
		JButton b = new JButton(name, ImageProvider.get("dialogs", name.toLowerCase()));
		b.setActionCommand(name);
		b.addActionListener(actionListener);
		b.setToolTipText(tooltip);
		//b.setMnemonic(mnemonic); TODO disabled until mapmodes have no Alt in their hotkey.
		return b;
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

	public void selectionChanged(Collection<OsmPrimitive> newSelection) {
		if (propertyTable.getCellEditor() != null)
			propertyTable.getCellEditor().cancelCellEditing();
		data.setRowCount(0);
		
		Map<String, Integer> valueCount = new HashMap<String, Integer>();
		TreeMap<String, Collection<String>> props = new TreeMap<String, Collection<String>>();
		for (OsmPrimitive osm : newSelection) {
			for (Entry<String, String> e : osm.entrySet()) {
				Collection<String> value = props.get(e.getKey());
				if (value == null) {
					value = new TreeSet<String>();
					props.put(e.getKey(), value);
				}
				value.add(e.getValue());
				valueCount.put(e.getKey(), valueCount.containsKey(e.getKey()) ? valueCount.get(e.getKey())+1 : 1);
			}
		}
		for (Entry<String, Collection<String>> e : props.entrySet()) {
			JComboBox value = new JComboBox(e.getValue().toArray());
			value.setEditable(true);
			value.getEditor().setItem(valueCount.get(e.getKey()) != newSelection.size() ? "<different>" : e.getValue().iterator().next());
			data.addRow(new Object[]{e.getKey(), value});
		}
	}
}
