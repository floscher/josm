package org.openstreetmap.josm.actions;

import java.awt.AWTKeyStroke;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JLabel;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.gui.ImageProvider;

/**
 * Base class helper for all Actions in JOSM. Just to make the life easier.
 * @author imi
 */
abstract public class JosmAction extends AbstractAction {

	/**
	 * Construct the action.
	 * 
	 * @param name		Name of the action (entry name in menu)
	 * @param iconName	Name of the icon (without extension)
	 * @param desc		Short tooltip description
	 * @param mnemonic	If non-<code>null</code>, the Mnemonic in menu
	 * @param shortCut	If non-<code>null</code>, the shortcut keystroke
	 */
	public JosmAction(String name, String iconName, String desc, Integer mnemonic, AWTKeyStroke shortCut) {
		super(name, ImageProvider.get(iconName));
		putValue(SHORT_DESCRIPTION, desc);
		if (mnemonic != null)
			putValue(MNEMONIC_KEY, mnemonic);
		if (shortCut != null)
			putValue(ACCELERATOR_KEY, shortCut);
	}

	/**
	 * @return A dialog labeled "... Please Wait." where ... is the message parameter.
	 */
	protected JDialog createPleaseWaitDialog(String msg) {
		final JDialog pleaseWaitDlg = new JDialog(Main.main, true);
		pleaseWaitDlg.setUndecorated(true);
		JLabel l = new JLabel(msg+". Please Wait.");
		l.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createEtchedBorder(),
				BorderFactory.createEmptyBorder(20,20,20,20)));
		pleaseWaitDlg.getContentPane().add(l);
		pleaseWaitDlg.pack();
		pleaseWaitDlg.setLocation(Main.main.getWidth()/2-pleaseWaitDlg.getWidth()/2,
				Main.main.getHeight()/2-pleaseWaitDlg.getHeight()/2);
		pleaseWaitDlg.setResizable(false);
		pleaseWaitDlg.setAlwaysOnTop(true);
		return pleaseWaitDlg;
	}
}
