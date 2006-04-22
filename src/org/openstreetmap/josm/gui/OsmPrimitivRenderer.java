package org.openstreetmap.josm.gui;

import java.awt.Component;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JLabel;
import javax.swing.JList;

import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.visitor.NameVisitor;

/**
 * Renderer that renders the objects from an OsmPrimitive as data.
 * @author imi
 */
public class OsmPrimitivRenderer extends DefaultListCellRenderer {

	private NameVisitor visitor = new NameVisitor();

	@Override public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
		Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
		if (c instanceof JLabel && value != null) {
			((OsmPrimitive)value).visit(visitor);
			((JLabel)c).setText(visitor.name);
			((JLabel)c).setIcon(visitor.icon);
		}
		return c;
	}
}
