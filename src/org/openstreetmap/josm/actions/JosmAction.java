package org.openstreetmap.josm.actions;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.tools.ImageProvider;

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
	 * Construct the action as menu action entry.
	 * 
	 * @param name		Name of the action (entry name in menu)
	 * @param iconName	Name of the icon (without extension)
	 * @param tooltip	Short tooltip description
	 * @param mnemonic	Mnemonic in the menu
	 */
	public JosmAction(String name, String iconName, String tooltip, int mnemonic) {
		super(name, ImageProvider.get(iconName));
		putValue(SHORT_DESCRIPTION, tooltip);
		putValue(MNEMONIC_KEY, mnemonic);
	}


	public JosmAction(String name, String iconName, String tooltip, String shortCutName, KeyStroke shortCut) {
		super(name, ImageProvider.get(iconName));
		putValue(SHORT_DESCRIPTION, "<html>"+tooltip+" <font size='-2'>"+shortCutName+"</font>&nbsp;</html>");
		Main.main.panel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(shortCut, name);
		Main.main.panel.getActionMap().put(name, this);
	}
}
