package org.openstreetmap.josm.gui;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.Action;
import javax.swing.JComponent;
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
	public IconToggleButton(JComponent acceleratorReceiver, Action action) {
		super(action);
		setText(null);
		
		// Tooltip
		String toolTipText = "";
		Object o = action.getValue(Action.LONG_DESCRIPTION);
		if (o != null)
			toolTipText += o.toString();
		o = action.getValue(Action.ACCELERATOR_KEY);
		if (o != null) {
			String ksName = o.toString();
			if (ksName.startsWith("pressed "))
				ksName = ksName.substring("pressed ".length());
			else if (ksName.startsWith("released "))
				ksName = ksName.substring("released ".length());
			toolTipText += " Shortcut: "+ksName;
		}
		setToolTipText(toolTipText);
		
		action.addPropertyChangeListener(this);
	}

	public void propertyChange(PropertyChangeEvent evt) {
		if (evt.getPropertyName() == "active")
			setSelected((Boolean)evt.getNewValue());
	}
}
