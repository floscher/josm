package org.openstreetmap.josm.gui.preferences;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Map;
import java.util.TreeMap;
import java.util.Vector;
import java.util.Map.Entry;

import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.TableCellRenderer;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.tools.ColorHelper;
import org.openstreetmap.josm.tools.GBC;

public class ColorPreference implements PreferenceSetting {

	private JTable colors;

	public void addGui(final PreferenceDialog gui) {
		Map<String,String> allColors = new TreeMap<String, String>(Main.pref.getAllPrefix("color."));

		Vector<Vector<Object>> rows = new Vector<Vector<Object>>();
		for (Entry<String,String> e : allColors.entrySet()) {
			Vector<Object> row = new Vector<Object>(2);
			row.add(e.getKey().substring("color.".length()));
			row.add(ColorHelper.html2color(e.getValue()));
			rows.add(row);
		}
		Vector<Object> cols = new Vector<Object>(2);
		cols.add(tr("Color"));
		cols.add(tr("Name"));
		colors = new JTable(rows, cols){
			@Override public boolean isCellEditable(int row, int column) {
				return false;
			}
		};
		colors.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		final TableCellRenderer oldColorsRenderer = colors.getDefaultRenderer(Object.class);
		colors.setDefaultRenderer(Object.class, new TableCellRenderer(){
			public Component getTableCellRendererComponent(JTable t, Object o, boolean selected, boolean focus, int row, int column) {
				if (column == 1) {
					JLabel l = new JLabel(ColorHelper.color2html((Color)o));
					l.setBackground((Color)o);
					l.setOpaque(true);
					return l;
				}
				return oldColorsRenderer.getTableCellRendererComponent(t,tr(o.toString()),selected,focus,row,column);
			}
		});
		colors.getColumnModel().getColumn(1).setWidth(100);

		JButton colorEdit = new JButton(tr("Choose"));
		colorEdit.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				if (colors.getSelectedRowCount() == 0) {
					JOptionPane.showMessageDialog(gui, tr("Please select a color."));
					return;
				}
				int sel = colors.getSelectedRow();
				JColorChooser chooser = new JColorChooser((Color)colors.getValueAt(sel, 1));
				int answer = JOptionPane.showConfirmDialog(gui, chooser, tr("Choose a color for {0}", colors.getValueAt(sel, 0)), JOptionPane.OK_CANCEL_OPTION);
				if (answer == JOptionPane.OK_OPTION)
					colors.setValueAt(chooser.getColor(), sel, 1);
			}
		});
		colors.setToolTipText(tr("Colors used by different objects in JOSM."));
		colors.setPreferredScrollableViewportSize(new Dimension(100,112));
		gui.display.add(new JLabel(tr("Colors")), GBC.eol());
		gui.display.add(new JScrollPane(colors), GBC.eol().fill(GBC.BOTH));
		gui.display.add(colorEdit, GBC.eol().anchor(GBC.EAST));
    }

	public void ok() {
		for (int i = 0; i < colors.getRowCount(); ++i) {
			String name = (String)colors.getValueAt(i, 0);
			Color col = (Color)colors.getValueAt(i, 1);
			Main.pref.put("color."+name, ColorHelper.color2html(col));
		}
    }
}
