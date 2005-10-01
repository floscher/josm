package org.openstreetmap.josm.gui;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.File;

import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.DefaultListCellRenderer;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
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
			pref.mergeNodes = mergeNodes.isSelected();
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
	 * Combobox with all projections available
	 */
	private JComboBox projectionCombo = new JComboBox(Preferences.allProjections.clone());
	/**
	 * The main tab panel.
	 */
	private JTabbedPane tabPane = new JTabbedPane(JTabbedPane.LEFT);
	/**
	 * The checkbox stating whether nodes should be merged together.
	 */
	private JCheckBox mergeNodes = new JCheckBox("Merge nodes with equal latitude/longitude.");

	
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

		// look and feel combo box
		final ListCellRenderer oldRenderer = lafCombo.getRenderer();
		lafCombo.setRenderer(new DefaultListCellRenderer(){
			@Override
			public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
				return oldRenderer.getListCellRendererComponent(list, ((LookAndFeelInfo)value).getName(), index, isSelected, cellHasFocus);
			}});
		lafCombo.setSelectedItem(pref.laf);
		lafCombo.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				setRequiresRestart();
			}});

		// projection method combo box
		for (int i = 0; i < projectionCombo.getItemCount(); ++i) {
			if (projectionCombo.getItemAt(i).getClass().equals(pref.projection.getClass())) {
				projectionCombo.setSelectedIndex(i);
				break;
			}
		}
		
		// Display tab
		JPanel display = createPreferenceTab("display", "Display Settings", "Various settings than influence the visual representation of the whole Program.");
		display.add(new JLabel("Look and Feel"), GBC.std());
		display.add(GBC.glue(5,0), GBC.std().fill(GBC.HORIZONTAL));
		display.add(lafCombo, GBC.eol().fill(GBC.HORIZONTAL));
		display.add(Box.createVerticalGlue(), GBC.eol().fill(GBC.VERTICAL));

		// Map tab
		JPanel map = createPreferenceTab("map", "Map Settings", "Settings for the map projection and data interpretation.");
		map.add(new JLabel("Projection method"), GBC.std());
		map.add(GBC.glue(5,0), GBC.std().fill(GBC.HORIZONTAL));
		map.add(projectionCombo, GBC.eol().fill(GBC.HORIZONTAL));
		JLabel labelNoteProjection = new JLabel(
				"<html>Note: This is the default projection method used for files, " +
				"where the correct projection could not be determined. " +
				"The actual used projection can be changed in the property " +
				"settings of each map.</html>");
		labelNoteProjection.setMinimumSize(new Dimension(550, 50));
		labelNoteProjection.setPreferredSize(new Dimension(550, 50));
		map.add(labelNoteProjection, GBC.eol().insets(0,5,0,20));
		map.add(new JLabel("GPX import / export"), GBC.eol());
		mergeNodes.setSelected(pref.mergeNodes);
		map.add(mergeNodes, GBC.eol());
		map.add(Box.createVerticalGlue(), GBC.eol().fill(GBC.VERTICAL));

		
		tabPane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
	
		// OK/Cancel panel at bottom
		JPanel okPanel = new JPanel(new GridBagLayout());
		okPanel.add(Box.createHorizontalGlue(), GBC.std().fill(GBC.HORIZONTAL));
		okPanel.add(new JButton(new OkAction()), GBC.std());
		okPanel.add(new JButton(new CancelAction()), GBC.std());

		// merging all in the content pane
		getContentPane().setLayout(new GridBagLayout());
		getContentPane().add(tabPane, GBC.eol().fill());
		getContentPane().add(okPanel, GBC.eol().fill(GBC.HORIZONTAL));

		setModal(true);
		pack();
		Dimension s = Main.main.getSize();
		setLocation(s.width/2-getWidth()/2, s.height/2-getHeight()/2);
	}

	/**
	 * Construct a JPanel for the preference settings. Layout is GridBagLayout
	 * and a centered title label and the description are added.
	 * @param icon The name of the icon.
	 * @param title The title of this preference tab.
	 * @param desc A description in one sentence for this tab. Will be displayed
	 * 		italic under the title.
	 * @return The created panel ready to add other controls.
	 */
	private JPanel createPreferenceTab(String icon, String title, String desc) {
		JPanel p = new JPanel(new GridBagLayout());
		p.add(new JLabel(title), GBC.eol().anchor(GBC.CENTER).insets(0,5,0,10));
		JLabel descLabel = new JLabel(desc);
		descLabel.setFont(descLabel.getFont().deriveFont(Font.ITALIC));
		p.add(descLabel, GBC.eol().insets(5,0,5,20));

		tabPane.addTab(null, new ImageIcon("images/preferences/"+icon+".png"), p);
		return p;
	}
	
	/**
	 * Remember, that the settings made requires a restart of the application.
	 * Called from various actionListener - classes
	 */
	protected void setRequiresRestart() {
		requiresRestart = true;
	}
}
