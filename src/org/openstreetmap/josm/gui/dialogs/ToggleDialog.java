package org.openstreetmap.josm.gui.dialogs;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.KeyStroke;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.JosmAction;
import org.openstreetmap.josm.tools.ShortCutLabel;

/**
 * This class is a toggle dialog that can be turned on and off. It is attached
 * to a ButtonModel.
 * 
 * @author imi
 */
public class ToggleDialog extends JPanel {

	public final class ToggleDialogAction extends JosmAction {
	    public final String prefname;
	    public AbstractButton button;

	    private ToggleDialogAction(String name, String iconName, String tooltip, String shortcut, KeyStroke cut, String prefname) {
		    super(name, iconName, tooltip, shortcut, cut);
		    this.prefname = prefname;
	    }

	    public void actionPerformed(ActionEvent e) {
	    	if (e != null && !(e.getSource() instanceof AbstractButton))
	    		button.setSelected(!button.isSelected());
	    	setVisible(button.isSelected());
	        Main.pref.put(prefname+".visible", button.isSelected());
	    }
    }

	/**
	 * The action to toggle this dialog.
	 */
	public ToggleDialogAction action;

	public ToggleDialog(String name, String iconName, String tooltip, int shortCut) {
		action = new ToggleDialogAction(name, "dialogs/"+iconName, tooltip, ShortCutLabel.name(shortCut, KeyEvent.ALT_MASK), KeyStroke.getKeyStroke(shortCut, KeyEvent.ALT_MASK), iconName);
		setLayout(new BorderLayout());
		add(new JLabel(name), BorderLayout.NORTH);
		setVisible(false);
		setBorder(BorderFactory.createEtchedBorder());
	}
}
