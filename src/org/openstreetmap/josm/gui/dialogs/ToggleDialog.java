package org.openstreetmap.josm.gui.dialogs;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.KeyStroke;

import org.openstreetmap.josm.actions.JosmAction;

/**
 * This class is a toggle dialog that can be turned on and off. It is attached
 * to a ButtonModel.
 * 
 * @author imi
 */
public class ToggleDialog extends JPanel {

	/**
	 * The action to toggle this dialog.
	 */
	public JosmAction action;
	
	/**
	 * Create a new ToggleDialog.
	 * @param title The title of the dialog.
	 */
	public ToggleDialog(String title, String name, String iconName, String tooltip, String shortCutName, int shortCut) {
		action = new JosmAction(name, "dialogs/"+iconName, tooltip, "Alt-"+shortCutName, KeyStroke.getKeyStroke(shortCut, KeyEvent.ALT_MASK)){
			public void actionPerformed(ActionEvent e) {
				boolean show = !isVisible();
				if (e.getSource() instanceof AbstractButton)
					show = ((AbstractButton)e.getSource()).isSelected();
				setVisible(show);
			}
		};
		
		setLayout(new BorderLayout());
		add(new JLabel(title), BorderLayout.NORTH);
		setVisible(false);
		setBorder(BorderFactory.createEtchedBorder());
	}
}
