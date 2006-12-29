package org.openstreetmap.josm.gui.dialogs;

import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trn;
import static org.xnap.commons.i18n.I18n.marktr;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Vector;
import java.util.Map.Entry;

import javax.swing.DefaultComboBoxModel;
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
import org.openstreetmap.josm.command.ChangePropertyCommand;
import org.openstreetmap.josm.data.SelectionChangedListener;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.gui.MapFrame;
import org.openstreetmap.josm.gui.annotation.AnnotationCellRenderer;
import org.openstreetmap.josm.gui.annotation.AnnotationPreset;
import org.openstreetmap.josm.gui.annotation.ForwardActionListener;
import org.openstreetmap.josm.gui.preferences.AnnotationPresetPreference;
import org.openstreetmap.josm.tools.ImageProvider;

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
		@Override public void mouseClicked(MouseEvent e) {
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
		Collection<OsmPrimitive> sel = Main.ds.getSelected();
		if (sel.isEmpty()) {
			JOptionPane.showMessageDialog(Main.parent, tr("Please select the objects you want to change properties for."));
			return;
		}
		String msg = "<html>"+trn("This will change {0} object.", "This will change {0} objects.", sel.size(), sel.size())+"<br><br> "+tr("Please select a new value for \"{0}\".<br>(Empty string deletes the key.)", key)+"</html>";
		final JComboBox combo = (JComboBox)data.getValueAt(row, 1);
		JPanel p = new JPanel(new BorderLayout());
		p.add(new JLabel(msg), BorderLayout.NORTH);
		p.add(combo, BorderLayout.CENTER);

		final JOptionPane optionPane = new JOptionPane(p, JOptionPane.QUESTION_MESSAGE, JOptionPane.OK_CANCEL_OPTION){
			@Override public void selectInitialValue() {
				combo.requestFocusInWindow();
				combo.getEditor().selectAll();
			}
		};
		final JDialog dlg = optionPane.createDialog(Main.parent, tr("Change values?"));
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
		if (value.equals(tr("<different>")))
			return;
		if (value.equals(""))
			value = null; // delete the key
		Main.main.editLayer().add(new ChangePropertyCommand(sel, key, value));

		if (value == null)
			selectionChanged(sel); // update whole table

		Main.parent.repaint(); // repaint all - drawing could have been changed
	}

	/**
	 * Open the add selection dialog and add a new key/value to the table (and to the
	 * dataset, of course).
	 */
	void add() {
		Collection<OsmPrimitive> sel = Main.ds.getSelected();
		if (sel.isEmpty()) {
			JOptionPane.showMessageDialog(Main.parent, tr("Please select objects for which you want to change properties."));
			return;
		}

		JPanel p = new JPanel(new BorderLayout());
		p.add(new JLabel("<html>"+trn("This will change {0} object.","This will change {0} objects.", sel.size(),sel.size())+"<br><br>"+tr("Please select a key")),
				BorderLayout.NORTH);
		TreeSet<String> allKeys = new TreeSet<String>();
		for (OsmPrimitive osm : Main.ds.allNonDeletedPrimitives())
			allKeys.addAll(osm.keySet());
		for (int i = 0; i < data.getRowCount(); ++i)
			allKeys.remove(data.getValueAt(i, 0));
		final JComboBox keys = new JComboBox(new Vector<String>(allKeys));
		keys.setEditable(true);
		p.add(keys, BorderLayout.CENTER);

		JPanel p2 = new JPanel(new BorderLayout());
		p.add(p2, BorderLayout.SOUTH);
		p2.add(new JLabel(tr("Please select a value")), BorderLayout.NORTH);
		final JTextField values = new JTextField();
		p2.add(values, BorderLayout.CENTER);
		JOptionPane pane = new JOptionPane(p, JOptionPane.PLAIN_MESSAGE, JOptionPane.OK_CANCEL_OPTION){
			@Override public void selectInitialValue() {
				keys.requestFocusInWindow();
				keys.getEditor().selectAll();
			}
		};
		pane.createDialog(Main.parent, tr("Change values?")).setVisible(true);
		if (!Integer.valueOf(JOptionPane.OK_OPTION).equals(pane.getValue()))
			return;
		String key = keys.getEditor().getItem().toString();
		String value = values.getText();
		if (value.equals(""))
			return;
		Main.main.editLayer().add(new ChangePropertyCommand(sel, key, value));
		selectionChanged(sel); // update table
		Main.parent.repaint(); // repaint all - drawing could have been changed
	}

	/**
	 * Delete the keys from the given row.
	 * @param row	The row, which key gets deleted from the dataset.
	 */
	private void delete(int row) {
		String key = data.getValueAt(row, 0).toString();
		Collection<OsmPrimitive> sel = Main.ds.getSelected();
		Main.main.editLayer().add(new ChangePropertyCommand(sel, key, null));
		selectionChanged(sel); // update table
	}

	/**
	 * The property data.
	 */
	private final DefaultTableModel data = new DefaultTableModel(){
		@Override public boolean isCellEditable(int row, int column) {
			return false;
		}
		@Override public Class<?> getColumnClass(int columnIndex) {
			return columnIndex == 1 ? JComboBox.class : String.class;
		}
	};
	/**
	 * The properties list.
	 */
	private final JTable propertyTable = new JTable(data);
	public JComboBox annotationPresets = new JComboBox();


	/**
	 * Create a new PropertiesDialog
	 */
	public PropertiesDialog(MapFrame mapFrame) {
		super(tr("Properties"), "propertiesdialog", tr("Property for selected objects."), KeyEvent.VK_P, 150);

		if (AnnotationPresetPreference.annotationPresets.size() > 0) {
			Vector<ActionListener> allPresets = new Vector<ActionListener>();
			for (final AnnotationPreset p : AnnotationPresetPreference.annotationPresets)
				allPresets.add(new ForwardActionListener(this, p));

			allPresets.add(0, new ForwardActionListener(this, new AnnotationPreset()));
			annotationPresets.setModel(new DefaultComboBoxModel(allPresets));
			add(annotationPresets, BorderLayout.NORTH);
		}
		annotationPresets.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				AnnotationPreset preset = ((ForwardActionListener)annotationPresets.getSelectedItem()).preset;
				preset.actionPerformed(e);
			}
		});
		annotationPresets.setRenderer(new AnnotationCellRenderer());

		data.setColumnIdentifiers(new String[]{tr("Key"),tr("Value")});
		propertyTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		propertyTable.setDefaultRenderer(JComboBox.class, new DefaultTableCellRenderer(){
			@Override public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
				Component c = super.getTableCellRendererComponent(table, value, isSelected, false, row, column);
				if (c instanceof JLabel) {
					String str = ((JComboBox)value).getEditor().getItem().toString();
					((JLabel)c).setText(str);
					if (str.equals(tr("<different>")))
						c.setFont(c.getFont().deriveFont(Font.ITALIC));
				}
				return c;
			}
		});
		propertyTable.setDefaultRenderer(String.class, new DefaultTableCellRenderer(){
			@Override public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
				return super.getTableCellRendererComponent(table, value, isSelected, false, row, column);
			}
		});
		DblClickWatch dblClickWatch = new DblClickWatch();
		propertyTable.addMouseListener(dblClickWatch);
		JScrollPane scrollPane = new JScrollPane(propertyTable);
		scrollPane.addMouseListener(dblClickWatch);
		add(scrollPane, BorderLayout.CENTER);

		JPanel buttonPanel = new JPanel(new GridLayout(1,3));
		ActionListener buttonAction = new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				int sel = propertyTable.getSelectedRow();
				if (e.getActionCommand().equals("Add"))
					add();
				else if (e.getActionCommand().equals("Edit")) {
					if (sel == -1)
						JOptionPane.showMessageDialog(Main.parent, tr("Please select the row to edit."));
					else
						edit(sel);
				} else if (e.getActionCommand().equals("Delete")) {
					if (sel == -1)
						JOptionPane.showMessageDialog(Main.parent, tr("Please select the row to delete."));
					else
						delete(sel);
				}
			}
		};
		buttonPanel.add(createButton(marktr("Add"),tr("Add a new key/value pair to all objects"), KeyEvent.VK_A, buttonAction));
		buttonPanel.add(createButton(marktr("Edit"),tr( "Edit the value of the selected key for all objects"), KeyEvent.VK_E, buttonAction));
		buttonPanel.add(createButton(marktr("Delete"),tr("Delete the selected key in all objects"), KeyEvent.VK_D, buttonAction));
		add(buttonPanel, BorderLayout.SOUTH);
	}

	private JButton createButton(String name, String tooltip, int mnemonic, ActionListener actionListener) {
		JButton b = new JButton(tr(name), ImageProvider.get("dialogs", name.toLowerCase()));
		b.setActionCommand(name);
		b.addActionListener(actionListener);
		b.setToolTipText(tooltip);
		b.setMnemonic(mnemonic);
		b.putClientProperty("help", "Dialog/Properties/"+name);
		return b;
	}

	@Override public void setVisible(boolean b) {
		if (b) {
			Main.ds.addSelectionChangedListener(this);
			selectionChanged(Main.ds.getSelected());
		} else {
			Main.ds.removeSelectionChangedListener(this);
		}
		super.setVisible(b);
	}

	public void selectionChanged(Collection<? extends OsmPrimitive> newSelection) {
		if (propertyTable == null)
			return; // selection changed may be received in base class constructor before init
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
			value.getEditor().setItem(valueCount.get(e.getKey()) != newSelection.size() ? tr("<different>") : e.getValue().iterator().next());
			data.addRow(new Object[]{e.getKey(), value});
		}
	}
}
