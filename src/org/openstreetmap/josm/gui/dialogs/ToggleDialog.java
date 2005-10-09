package org.openstreetmap.josm.gui.dialogs;

import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.HashMap;
import java.util.Map;

import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.KeyStroke;

import org.openstreetmap.josm.gui.ImageProvider;
import org.openstreetmap.josm.gui.Main;
import org.openstreetmap.josm.gui.MapFrame;

/**
 * This class is a toggle dialog that can be turned on and off. It is attached
 * to a ButtonModel.
 * 
 * @author imi
 */
public class ToggleDialog extends JDialog implements Action {

	/**
	 * Create a new ToggleDialog.
	 * @param title The title of the dialog.
	 */
	public ToggleDialog(MapFrame mapFrame, String title, String name, String iconName, int mnemonic, String tooltip) {
		super(Main.main, title, false);
		putValue(SMALL_ICON, ImageProvider.get("dialogs", iconName));
		putValue(NAME, name);
		putValue(MNEMONIC_KEY, mnemonic);
		KeyStroke ks = KeyStroke.getKeyStroke(mnemonic,0);
		putValue(ACCELERATOR_KEY, ks);
		mapFrame.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(ks, this);
		mapFrame.getActionMap().put(this, this);
		putValue(LONG_DESCRIPTION, tooltip);
		mapFrame.addPropertyChangeListener("visible", new PropertyChangeListener(){
			public void propertyChange(PropertyChangeEvent evt) {
				if (evt.getNewValue() == Boolean.FALSE)
					setVisible(false);
			}
		});
	}

	/**
	 * Show this if not shown. Else request the focus.
	 */
	public void actionPerformed(ActionEvent e) {
		boolean show = !isVisible();
		if (e.getSource() instanceof AbstractButton)
			show = ((AbstractButton)e.getSource()).isSelected();
		setVisible(show);
	}

	// to satisfy Action interface

	private Map<String, Object> properties = new HashMap<String, Object>();
	public Object getValue(String key) {
		return properties.get(key);
	}
	public void putValue(String key, Object value) {
		properties.put(key, value);
	}
}
