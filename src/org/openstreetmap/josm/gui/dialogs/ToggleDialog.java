package org.openstreetmap.josm.gui.dialogs;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.JosmAction;
import org.openstreetmap.josm.actions.HelpAction.Helpful;

/**
 * This class is a toggle dialog that can be turned on and off. It is attached
 * to a ButtonModel.
 *
 * @author imi
 */
public class ToggleDialog extends JPanel implements Helpful {

	public final class ToggleDialogAction extends JosmAction {
		public final String prefname;
		public AbstractButton button;

		private ToggleDialogAction(String name, String iconName, String tooltip, int shortCut, int modifier, String prefname) {
			super(name, iconName, tooltip, shortCut, modifier, false);
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
	public final String prefName;

	public ToggleDialog(String name, String iconName, String tooltip, int shortCut, int preferredHeight) {
		this.prefName = iconName;
		setPreferredSize(new Dimension(330,preferredHeight));
		action = new ToggleDialogAction(name, "dialogs/"+iconName, tooltip, shortCut, KeyEvent.ALT_MASK, iconName);
		String helpId = "Dialog/"+getClass().getName().substring(getClass().getName().lastIndexOf('.')+1);
		action.putValue("help", helpId.substring(0, helpId.length()-6));
		setLayout(new BorderLayout());
		add(new JLabel(name), BorderLayout.NORTH);
		setVisible(false);
		setBorder(BorderFactory.createEtchedBorder());
	}

	public String helpTopic() {
		String help = getClass().getName();
		help = help.substring(help.lastIndexOf('.')+1, help.length()-6);
		return "Dialog/"+help;
	}
}
