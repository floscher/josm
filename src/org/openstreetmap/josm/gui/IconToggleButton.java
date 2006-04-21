package org.openstreetmap.josm.gui;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.Action;
import javax.swing.JToggleButton;

/**
 * Just a toggle button, with smaller border and icon only to display in
 * MapFrame toolbars.
 *
 * @author imi
 */
public class IconToggleButton extends JToggleButton implements PropertyChangeListener {

	/**
	 * Construct the toggle button with the given action.
	 */
	public IconToggleButton(Action action) {
		super(action);
		setText(null);

		Object o = action.getValue(Action.SHORT_DESCRIPTION);
		if (o != null)
			setToolTipText(o.toString());
		
		action.addPropertyChangeListener(this);
	}

	public void propertyChange(PropertyChangeEvent evt) {
		if (evt.getPropertyName().equals("active"))
			setSelected((Boolean)evt.getNewValue());
	}
}
