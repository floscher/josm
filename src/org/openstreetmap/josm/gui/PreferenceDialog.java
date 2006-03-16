package org.openstreetmap.josm.gui;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.ListCellRenderer;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;

import org.openstreetmap.josm.Main;
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
			super(UIManager.getString("OptionPane.okButtonText"), 
					UIManager.getIcon("OptionPane.okIcon"));
			putValue(MNEMONIC_KEY, new Integer((String)UIManager.get("OptionPane.okButtonMnemonic")));
		}
		public void actionPerformed(ActionEvent e) {
			Main.pref.laf = (LookAndFeelInfo)lafCombo.getSelectedItem();
			Main.pref.setProjection((Projection)projectionCombo.getSelectedItem());
			Main.pref.osmDataServer = osmDataServer.getText();
			Main.pref.osmDataUsername = osmDataUsername.getText();
			Main.pref.osmDataPassword = String.valueOf(osmDataPassword.getPassword());
			if (Main.pref.osmDataPassword == "")
				Main.pref.osmDataPassword = null;
			Main.pref.csvImportString = csvImportString.getText();
			Main.pref.setDrawRawGpsLines(drawRawGpsLines.isSelected());
			Main.pref.setForceRawGpsLines(forceRawGpsLines.isSelected());
			try {
				Main.pref.save();
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
			super(UIManager.getString("OptionPane.cancelButtonText"), 
					UIManager.getIcon("OptionPane.cancelIcon"));
			putValue(MNEMONIC_KEY, new Integer((String)UIManager.get("OptionPane.cancelButtonMnemonic")));
		}
		public void actionPerformed(ActionEvent e) {
			setVisible(false);
		}
	}

	/**
	 * Indicate, that the application has to be restarted for the settings to take effect.
	 */
	boolean requiresRestart = false;
	/**
	 * ComboBox with all look and feels.
	 */
	JComboBox lafCombo = new JComboBox(UIManager.getInstalledLookAndFeels());
	/**
	 * Combobox with all projections available
	 */
	JComboBox projectionCombo = new JComboBox(Preferences.allProjections);
	/**
	 * The main tab panel.
	 */
	private JTabbedPane tabPane = new JTabbedPane(JTabbedPane.LEFT);

	/**
	 * Editfield for the Base url to the REST API from OSM. 
	 */
	JTextField osmDataServer = new JTextField(20);
	/**
	 * Editfield for the username to the OSM account.
	 */
	JTextField osmDataUsername = new JTextField(20);
	/**
	 * Passwordfield for the userpassword of the REST API.
	 */
	JPasswordField osmDataPassword = new JPasswordField(20);
	/**
	 * Comma seperated import string specifier or <code>null</code> if the first
	 * data line should be interpreted as one.
	 */
	JTextField csvImportString = new JTextField(20);
	/**
	 * The checkbox stating whether nodes should be merged together.
	 */
	JCheckBox drawRawGpsLines = new JCheckBox("Draw lines between raw gps points.");
	/**
	 * The checkbox stating whether raw gps lines should be forced.
	 */
	JCheckBox forceRawGpsLines = new JCheckBox("Force lines if no line segments imported.");

	/**
	 * Create a preference setting dialog from an preferences-file. If the file does not
	 * exist, it will be created.
	 * If the dialog is closed with Ok, the preferences will be stored to the preferences-
	 * file, otherwise no change of the file happens.
	 */
	public PreferenceDialog() {
		super(Main.main, "Preferences");

		// look and feel combo box
		final ListCellRenderer oldRenderer = lafCombo.getRenderer();
		lafCombo.setRenderer(new DefaultListCellRenderer(){
			@Override
			public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
				return oldRenderer.getListCellRendererComponent(list, ((LookAndFeelInfo)value).getName(), index, isSelected, cellHasFocus);
			}});
		lafCombo.setSelectedItem(Main.pref.laf);
		lafCombo.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				setRequiresRestart();
			}});

		// projection combo box
		for (int i = 0; i < projectionCombo.getItemCount(); ++i) {
			if (projectionCombo.getItemAt(i).getClass().equals(Main.pref.getProjection().getClass())) {
				projectionCombo.setSelectedIndex(i);
				break;
			}
		}
		
		// drawRawGpsLines
		drawRawGpsLines.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				if (!drawRawGpsLines.isSelected())
					forceRawGpsLines.setSelected(false);
				forceRawGpsLines.setEnabled(drawRawGpsLines.isSelected());
			}
		});

		
		osmDataServer.setToolTipText("The base URL to the OSM server (REST API)");
		osmDataUsername.setToolTipText("Login name (email) to the OSM account.");
		osmDataPassword.setToolTipText("Login password to the OSM account. Leave blank to not store any password.");
		csvImportString.setToolTipText("<html>Import string specification. Currently, only lat/lon pairs are imported.<br>" +
				"<b>lat</b>: The latitude coordinate<br>" +
				"<b>lon</b>: The longitude coordinate<br>" +
				"<b>ignore</b>: Skip this field<br>" +
				"An example: \"ignore ignore lat lon\" will use ' ' as delimiter, skip the first two values and read then lat/lon.<br>" +
				"Other example: \"lat,lon\" will just read lat/lon values comma seperated.</html>");
		drawRawGpsLines.setToolTipText("If your gps device draw to few lines, select this to draw lines along your way.");
		drawRawGpsLines.setSelected(Main.pref.isDrawRawGpsLines());
		forceRawGpsLines.setToolTipText("Force drawing of lines if the imported data contain no line information.");
		forceRawGpsLines.setSelected(Main.pref.isForceRawGpsLines());
		forceRawGpsLines.setEnabled(drawRawGpsLines.isSelected());

		osmDataServer.setText(Main.pref.osmDataServer);
		osmDataUsername.setText(Main.pref.osmDataUsername);
		osmDataPassword.setText(Main.pref.osmDataPassword);
		csvImportString.setText(Main.pref.csvImportString);

		// Display tab
		JPanel display = createPreferenceTab("display", "Display Settings", "Various settings that influence the visual representation of the whole program.");
		display.add(new JLabel("Look and Feel"), GBC.std());
		display.add(GBC.glue(5,0), GBC.std().fill(GBC.HORIZONTAL));
		display.add(lafCombo, GBC.eol().fill(GBC.HORIZONTAL));
		display.add(drawRawGpsLines, GBC.eol().insets(20,0,0,0));
		display.add(forceRawGpsLines, GBC.eol().insets(40,0,0,0));
		display.add(Box.createVerticalGlue(), GBC.eol().fill(GBC.VERTICAL));

		// Connection tab
		JPanel con = createPreferenceTab("connection", "Connection Settings", "Connection Settings to the OSM server.");
		con.add(new JLabel("Base Server URL"), GBC.std());
		con.add(osmDataServer, GBC.eol().fill(GBC.HORIZONTAL).insets(5,0,0,5));
		con.add(new JLabel("OSM username (email)"), GBC.std());
		con.add(osmDataUsername, GBC.eol().fill(GBC.HORIZONTAL).insets(5,0,0,5));
		con.add(new JLabel("OSM password"), GBC.std());
		con.add(osmDataPassword, GBC.eol().fill(GBC.HORIZONTAL).insets(5,0,0,0));
		JLabel warning = new JLabel("<html>" +
				"WARNING: The password is stored in plain text in the preferences file.<br>" +
				"The password is transfered in plain text to the server, encoded in the url.<br>" +
				"<b>Do not use a valuable Password.</b></html>");
		warning.setFont(warning.getFont().deriveFont(Font.ITALIC));
		con.add(warning, GBC.eop().fill(GBC.HORIZONTAL));
		con.add(new JLabel("CSV import specification (empty: read from first line in data)"), GBC.eol());
		con.add(csvImportString, GBC.eop().fill(GBC.HORIZONTAL));
		
		// Map tab
		JPanel map = createPreferenceTab("map", "Map Settings", "Settings for the map projection and data interpretation.");
		map.add(new JLabel("Projection method"), GBC.std());
		map.add(GBC.glue(5,0), GBC.std().fill(GBC.HORIZONTAL));
		map.add(projectionCombo, GBC.eol().fill(GBC.HORIZONTAL).insets(0,0,0,5));
		map.add(Box.createVerticalGlue(), GBC.eol().fill(GBC.VERTICAL));


		tabPane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
	
		// OK/Cancel panel at bottom
		JPanel okPanel = new JPanel(new GridBagLayout());
		okPanel.add(Box.createHorizontalGlue(), GBC.std().fill(GBC.HORIZONTAL));
		okPanel.add(new JButton(new OkAction()), GBC.std());
		okPanel.add(new JButton(new CancelAction()), GBC.std());
		okPanel.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));

		// merging all in the content pane
		getContentPane().setLayout(new GridBagLayout());
		getContentPane().add(tabPane, GBC.eol().fill());
		getContentPane().add(okPanel, GBC.eol().fill(GBC.HORIZONTAL));

		setModal(true);
		pack();
		Dimension s = Main.main.getSize();
		setLocation(Main.main.getX()+s.width/2-getWidth()/2, Main.main.getY()+s.height/2-getHeight()/2);
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
		
		JLabel descLabel = new JLabel("<html>"+desc+"</html>");
		descLabel.setFont(descLabel.getFont().deriveFont(Font.ITALIC));
		p.add(descLabel, GBC.eol().insets(5,0,5,20).fill(GBC.HORIZONTAL));

		tabPane.addTab(null, ImageProvider.get("preferences", icon), p);
		tabPane.setToolTipTextAt(tabPane.getTabCount()-1, desc);
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
