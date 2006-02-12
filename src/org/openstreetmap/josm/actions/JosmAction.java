package org.openstreetmap.josm.actions;

import java.awt.AWTKeyStroke;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.gui.ImageProvider;

/**
 * Base class helper for all Actions in JOSM. Just to make the life easier.
 * @author imi
 */
abstract public class JosmAction extends AbstractAction {

	/**
	 * Instanced of this thread will display a "Please Wait" message in middle of JOSM
	 * to indicate a progress beeing executed.
	 *  
	 * @author Imi
	 */
	protected abstract class PleaseWaitRunnable implements Runnable {
		private String msg;
		private JDialog pleaseWaitDlg;
		/**
		 * Create the runnable object with a given message for the user.
		 */
		PleaseWaitRunnable(String msg) {
			this.msg = msg;
		}
		public final void run() {
			pleaseWaitDlg = new JDialog(Main.main, true);
			pleaseWaitDlg.setUndecorated(true);
			JLabel l = new JLabel(msg+". Please Wait.");
			l.setBorder(BorderFactory.createCompoundBorder(
					BorderFactory.createEtchedBorder(),
					BorderFactory.createEmptyBorder(20,20,20,20)));
			pleaseWaitDlg.getContentPane().add(l);
			pleaseWaitDlg.pack();
			pleaseWaitDlg.setLocation(Main.main.getX()+Main.main.getWidth()/2-pleaseWaitDlg.getWidth()/2,
					Main.main.getY()+Main.main.getHeight()/2-pleaseWaitDlg.getHeight()/2);
			pleaseWaitDlg.setResizable(false);
			SwingUtilities.invokeLater(new Runnable(){
				public void run() {
					pleaseWaitDlg.setVisible(true);
				}
			});
			try {
				realRun();
			} finally {
				closeDialog();
			}
		}
		public abstract void realRun();
		public void closeDialog() {
			pleaseWaitDlg.setVisible(false);
			pleaseWaitDlg.dispose();
		}
	}
	
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
}
