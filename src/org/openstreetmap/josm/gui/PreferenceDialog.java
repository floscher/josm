package org.openstreetmap.josm.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.File;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.DefaultListCellRenderer;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.ListCellRenderer;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;

import org.openstreetmap.josm.data.Preferences;
import org.openstreetmap.josm.data.Preferences.PreferencesException;
import org.openstreetmap.josm.data.projection.Projection;

/**
 * The preference settings.
 *
 * @author imi
 */
public class PreferenceDialog extends JDialog {

	/**
	 * Action to take place when user pressed the ok button.
	 */
	class OkAction extends AbstractAction {
		public OkAction() {
			super("Ok", new ImageIcon("images/ok.png"));
			putValue(MNEMONIC_KEY, KeyEvent.VK_ENTER);
		}
		public void actionPerformed(ActionEvent e) {
			Preferences pref = new Preferences();
			pref.laf = (LookAndFeelInfo)lafCombo.getSelectedItem();
			pref.projection = (Projection)projectionCombo.getSelectedItem();
			Main.pref.projection = pref.projection;
			try {
				pref.save();
			} catch (PreferencesException x) {
				x.printStackTrace();
				JOptionPane.showMessageDialog(PreferenceDialog.this, "Could not save preferences:\n"+x.getMessage());
			}
			if (requiresRestart)
				JOptionPane.showMessageDialog(PreferenceDialog.this, "You have to restart JOSM for some settings to take effect.");
			setVisible(false);
		}
	}

	/**
	 * Action to take place when user pressed the cancel button.
	 */
	class CancelAction extends AbstractAction {
		public CancelAction() {
			super("Cancel", new ImageIcon("images/cancel.png"));
			putValue(MNEMONIC_KEY, KeyEvent.VK_ESCAPE);
		}
		public void actionPerformed(ActionEvent e) {
			setVisible(false);
		}
	}

	/**
	 * Indicate, that the application has to be restarted for the settings to take effect.
	 */
	private boolean requiresRestart = false;
	/**
	 * ComboBox with all look and feels.
	 */
	private JComboBox lafCombo = new JComboBox(UIManager.getInstalledLookAndFeels());
	/**
	 * The tabbed pane to add tabulars to.
	 */
	private JTabbedPane tabPanel = new JTabbedPane();
	/**
	 * Combobox with all projections available
	 */
	private JComboBox projectionCombo = new JComboBox(Preferences.allProjections);

	
	/**
	 * Create a preference setting dialog from an preferences-file. If the file does not
	 * exist, it will be created.
	 * If the dialog is closed with Ok, the preferences will be stored to the preferences-
	 * file, otherwise no change of the file happens.
	 */
	public PreferenceDialog() {
		super(Main.main, "Preferences");

		Preferences pref = new Preferences();
		try {
			if (new File(Preferences.getPreferencesFile()).exists())
				pref.load();
		} catch (PreferencesException e) {
			JOptionPane.showMessageDialog(Main.main, "Preferences settings could not be parsed:\n"+e.getMessage());
			e.printStackTrace();
			return;
		}

		getContentPane().setLayout(new BorderLayout());
		getContentPane().add(tabPanel, BorderLayout.CENTER);

		newTab("Display");
		// laf
		JPanel p = newPanelLine();
		p.add(new JLabel("Look and Feel"));
		final ListCellRenderer oldRenderer = lafCombo.getRenderer();
		lafCombo.setRenderer(new DefaultListCellRenderer(){
			public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
				return oldRenderer.getListCellRendererComponent(list, ((LookAndFeelInfo)value).getName(), index, isSelected, cellHasFocus);
			}});
		lafCombo.setSelectedItem(pref.laf);
		lafCombo.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				setRequiresRestart();
			}});
		p.add(lafCombo);

		newTab("Projection");
		p = newPanelLine();
		p.add(new JLabel("Projection System"));
		p.add(projectionCombo);
		for (int i = 0; i < projectionCombo.getItemCount(); ++i)
			if (projectionCombo.getItemAt(i).getClass().equals(pref.projection.getClass())) {
				projectionCombo.setSelectedIndex(i);
				break;
			}

		// OK/Cancel
		JPanel okPanel = new JPanel();
		okPanel.add(new JButton(new OkAction()));
		okPanel.add(new JButton(new CancelAction()));
		getContentPane().add(okPanel, BorderLayout.SOUTH);

		setModal(true);
		pack();
		Dimension s = Main.main.getSize();
		setLocation(s.width/2-getWidth()/2, s.height/2-getHeight()/2);
	}

	/**
	 * Start a new tab with the given name
	 * @param tabName The name of the new tab.
	 */
	private void newTab(String tabName) {
		Box tab = Box.createVerticalBox();
		tab.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
		tabPanel.addTab(tabName, tab);
	}

	/**
	 * Remember, that the settings made requires a restart of the application.
	 * Called from various actionListener - classes
	 */
	protected void setRequiresRestart() {
		requiresRestart = true;
	}

	private JPanel newPanelLine() {
		JPanel p;
		p = new JPanel();
		p.setBorder(BorderFactory.createEmptyBorder(5,0,0,0));
		((Container)tabPanel.getComponent(tabPanel.getTabCount()-1)).add(p);
		return p;
	}
}
