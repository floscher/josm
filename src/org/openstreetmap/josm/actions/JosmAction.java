package org.openstreetmap.josm.actions;

import java.awt.EventQueue;
import java.io.FileNotFoundException;
import java.io.IOException;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.KeyStroke;

import org.jdom.JDOMException;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.tools.ImageProvider;
import org.xml.sax.SAXException;

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
		public final JDialog pleaseWaitDlg;
		private String errorMessage;
		/**
		 * Create the runnable object with a given message for the user.
		 */
		public PleaseWaitRunnable(String msg) {
			pleaseWaitDlg = new JDialog(Main.main, true);
			pleaseWaitDlg.setUndecorated(true);
			JLabel l = new JLabel(msg+". Please Wait.");
			l.setBorder(BorderFactory.createCompoundBorder(
					BorderFactory.createEtchedBorder(),
					BorderFactory.createEmptyBorder(20,20,20,20)));
			pleaseWaitDlg.getContentPane().add(l);
			pleaseWaitDlg.pack();
			pleaseWaitDlg.setLocationRelativeTo(Main.main);
			pleaseWaitDlg.setResizable(false);
		}
		public final void run() {
			try {
				realRun();
	    	} catch (SAXException x) {
	    		x.printStackTrace();
	    		errorMessage = "Error while parsing: "+x.getMessage();
	    	} catch (JDOMException x) {
	    		x.printStackTrace();
	    		errorMessage = "Error while parsing: "+x.getMessage();
	    	} catch (FileNotFoundException x) {
	    		x.printStackTrace();
	    		errorMessage = "URL not found: " + x.getMessage();
	    	} catch (IOException x) {
	    		x.printStackTrace();
	    		errorMessage = x.getMessage();
			} finally {
				closeDialog();
			}
		}
		/**
		 * Called in the worker thread to do the actual work. When any of the
		 * exception is thrown, a message box will be displayed and closeDialog
		 * is called. finish() is called in any case.
		 */
		protected abstract void realRun() throws SAXException, JDOMException, IOException;
		/**
		 * Finish up the data work. Is guaranteed to be called if realRun is called.
		 * Finish is called in the gui thread just after the dialog disappeared.
		 */
		protected void finish() {}
		/**
		 * Close the dialog. Usually called from worker thread.
		 */
		public void closeDialog() {
			EventQueue.invokeLater(new Runnable(){
				public void run() {
					finish();
					pleaseWaitDlg.setVisible(false);
					pleaseWaitDlg.dispose();
					if (errorMessage != null)
						JOptionPane.showMessageDialog(Main.main, errorMessage);
                }
			});
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
