/**
 * 
 */
package org.openstreetmap.josm.gui.annotation;

import java.awt.Component;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;

import org.openstreetmap.josm.tools.ImageProvider;

final public class AnnotationCellRenderer extends DefaultListCellRenderer {
	@Override public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        AnnotationPreset a = (AnnotationPreset)value;
    	if (a == null || a.name == null)
        	return super.getListCellRendererComponent(list, "", index, false, false);
    	JComponent c = (JComponent)super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        JLabel l = new JLabel((a).name);
        l.setForeground(c.getForeground());
        l.setBackground(c.getBackground());
        l.setFont(c.getFont());
        l.setBorder(c.getBorder());
        if (a.types == null)
        	l.setIcon(ImageProvider.get("data", "empty"));
        else if (a.types.size() != 1)
        	l.setIcon(ImageProvider.get("data", "object"));
        else
        	l.setIcon(ImageProvider.get("data", a.types.iterator().next().getSimpleName().toLowerCase()));
        l.setOpaque(true);
        return l;
    }
}