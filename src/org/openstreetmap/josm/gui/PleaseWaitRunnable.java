package org.openstreetmap.josm.gui;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.EventQueue;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.FileNotFoundException;
import java.io.IOException;

import javax.swing.BorderFactory;
import javax.swing.BoundedRangeModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.tools.GBC;
import org.xml.sax.SAXException;

/**
 * Instanced of this thread will display a "Please Wait" message in middle of JOSM
 * to indicate a progress beeing executed.
 *  
 * @author Imi
 */
public abstract class PleaseWaitRunnable implements Runnable {

	public final JDialog pleaseWaitDlg;
	public String errorMessage;

	private final JProgressBar progressBar = new JProgressBar();
	private boolean closeDialogCalled = false;

	protected final JLabel currentAction = new JLabel(tr("Contact OSM server..."));
	protected final BoundedRangeModel progress = progressBar.getModel();

	/**
	 * Create the runnable object with a given message for the user.
	 */
	public PleaseWaitRunnable(String msg) {
		pleaseWaitDlg = new JDialog(JOptionPane.getFrameForComponent(Main.parent), msg, true);
		pleaseWaitDlg.setLayout(new GridBagLayout());
		JPanel pane = new JPanel(new GridBagLayout());
		pane.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
		pane.add(currentAction, GBC.eol().fill(GBC.HORIZONTAL));
		pane.add(progressBar, GBC.eop().fill(GBC.HORIZONTAL));
		JButton cancel = new JButton(tr(tr("Cancel")));
		pane.add(cancel, GBC.eol().anchor(GBC.CENTER));
		pleaseWaitDlg.setContentPane(pane);
		pleaseWaitDlg.setSize(350,100);
		pleaseWaitDlg.setLocationRelativeTo(Main.parent);

		cancel.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				cancel();
			}
		});
		pleaseWaitDlg.addWindowListener(new WindowAdapter(){
			@Override public void windowClosing(WindowEvent e) {
				if (!closeDialogCalled) {
					cancel();
					closeDialog();
				}
			}
		});
	}

	public final void run() {
		try {
			realRun();
		} catch (SAXException x) {
			x.printStackTrace();
			errorMessage = tr("Error while parsing: ")+x.getMessage();
		} catch (FileNotFoundException x) {
			x.printStackTrace();
			errorMessage = tr("Not found: ") + x.getMessage();
		} catch (IOException x) {
			x.printStackTrace();
			errorMessage = x.getMessage();
		} finally {
			closeDialog();
		}
	}

	/**
	 * User pressed cancel button.
	 */
	protected abstract void cancel();

	/**
	 * Called in the worker thread to do the actual work. When any of the
	 * exception is thrown, a message box will be displayed and closeDialog
	 * is called. finish() is called in any case.
	 */
	protected abstract void realRun() throws SAXException, IOException;

	/**
	 * Finish up the data work. Is guaranteed to be called if realRun is called.
	 * Finish is called in the gui thread just after the dialog disappeared.
	 */
	protected abstract void finish();

	/**
	 * Close the dialog. Usually called from worker thread.
	 */
	public void closeDialog() {
		if (closeDialogCalled)
			return;
		closeDialogCalled  = true;
		EventQueue.invokeLater(new Runnable(){
			public void run() {
				finish();
				pleaseWaitDlg.setVisible(false);
				pleaseWaitDlg.dispose();
				if (errorMessage != null)
					JOptionPane.showMessageDialog(Main.parent, errorMessage);
			}
		});
	}
}