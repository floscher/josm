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

	/**
	 * Create a new ToggleDialog.
	 * @param title The title of the dialog.
     * @param prefName Name of the base preference setting string (prefix)
	 */
	public ToggleDialog(String title, String name, String iconName, String tooltip, String shortCutName, int shortCut, final String prefName) {
		action = new ToggleDialogAction(name, "dialogs/"+iconName, tooltip, "Alt-"+shortCutName, KeyStroke.getKeyStroke(shortCut, KeyEvent.ALT_MASK), prefName);
		setLayout(new BorderLayout());
		add(new JLabel(title), BorderLayout.NORTH);
		setVisible(false);
		setBorder(BorderFactory.createEtchedBorder());
	}
}
