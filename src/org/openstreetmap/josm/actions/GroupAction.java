package org.openstreetmap.josm.actions;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import org.openstreetmap.josm.gui.IconToggleButton;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.ImageProvider.OverlayPosition;


public class GroupAction extends AbstractAction {

	protected final List<Action> actions = new ArrayList<Action>();
	private int current;

	protected void setCurrent(int current) {
		this.current = current;
		putValue(SMALL_ICON, ImageProvider.overlay((Icon)actions.get(current).getValue(SMALL_ICON), "right", OverlayPosition.SOUTHEAST));
		putValue(SHORT_DESCRIPTION, actions.get(current).getValue(SHORT_DESCRIPTION));
    }

	public void actionPerformed(ActionEvent e) {
		if (e.getSource() instanceof IconToggleButton && ((IconToggleButton)e.getSource()).groupbutton) {
			IconToggleButton b = (IconToggleButton)e.getSource();
			b.setSelected(!b.isSelected());
			openPopup(b);
		} else
			actions.get(current).actionPerformed(e);
    }

	private void openPopup(IconToggleButton b) {
		JPopupMenu popup = new JPopupMenu();
		for (int i = 0; i < actions.size(); ++i) {
			final int j = i;
			JMenuItem item = new JMenuItem(actions.get(i));
			item.addActionListener(new ActionListener(){
				public void actionPerformed(ActionEvent e) {
					setCurrent(j);
                }
			});
			popup.add(item);
		}
		popup.show(b, b.getWidth(), 0);
    }
}
