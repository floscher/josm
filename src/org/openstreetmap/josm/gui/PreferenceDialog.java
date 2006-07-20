package org.openstreetmap.josm.gui;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Locale;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.Vector;
import java.util.Map.Entry;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;
import javax.swing.table.TableCellRenderer;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.projection.Projection;
import org.openstreetmap.josm.tools.ColorHelper;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.ImageProvider;

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
			try {
				putValue(MNEMONIC_KEY, new Integer((String)UIManager.get("OptionPane.okButtonMnemonic")));
			} catch (NumberFormatException e) {
				// just don't set the mnemonic
			}
		}
		public void actionPerformed(ActionEvent e) {
			Main.pref.put("laf", ((LookAndFeelInfo)lafCombo.getSelectedItem()).getClassName());
			Main.pref.put("language", languages.getSelectedItem().toString());
			Main.pref.put("projection", projectionCombo.getSelectedItem().getClass().getName());
			Main.pref.put("osm-server.url", osmDataServer.getText());
			Main.pref.put("osm-server.username", osmDataUsername.getText());
			String pwd = String.valueOf(osmDataPassword.getPassword());
			if (pwd.equals(""))
				pwd = null;
			Main.pref.put("osm-server.password", pwd);
			Main.pref.put("wms.baseurl", wmsServerBaseUrl.getText());
			Main.pref.put("csv.importstring", csvImportString.getText());
			Main.pref.put("draw.rawgps.lines", drawRawGpsLines.isSelected());
			Main.pref.put("draw.rawgps.lines.force", forceRawGpsLines.isSelected());
			Main.pref.put("draw.rawgps.large", largeGpsPoints.isSelected());
			Main.pref.put("draw.segment.direction", directionHint.isSelected());

			if (annotationSources.getModel().getSize() > 0) {
				StringBuilder sb = new StringBuilder();
				for (int i = 0; i < annotationSources.getModel().getSize(); ++i)
					sb.append(";"+annotationSources.getModel().getElementAt(i));
				Main.pref.put("annotation.sources", sb.toString().substring(1));
			} else
				Main.pref.put("annotation.sources", null);

			for (int i = 0; i < colors.getRowCount(); ++i) {
				String name = (String)colors.getValueAt(i, 0);
				Color col = (Color)colors.getValueAt(i, 1);
				Main.pref.put("color."+name, ColorHelper.color2html(col));
			}

			if (requiresRestart)
				JOptionPane.showMessageDialog(PreferenceDialog.this,tr("You have to restart JOSM for some settings to take effect."));
			Main.parent.repaint();
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
			try {
				putValue(MNEMONIC_KEY, new Integer((String)UIManager.get("OptionPane.cancelButtonMnemonic")));
			} catch (NumberFormatException e) {
				// just don't set the mnemonic
			}
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
	private JComboBox languages = new JComboBox(new Locale[]{Locale.ENGLISH, Locale.GERMAN, Locale.FRENCH});
	/**
	 * The main tab panel.
	 */
	private JTabbedPane tabPane = new JTabbedPane(JTabbedPane.LEFT);

	/**
	 * Editfield for the Base url to the REST API from OSM. 
	 */
	private JTextField osmDataServer = new JTextField(20);
	/**
	 * Editfield for the username to the OSM account.
	 */
	private JTextField osmDataUsername = new JTextField(20);
	/**
	 * Passwordfield for the userpassword of the REST API.
	 */
	private JPasswordField osmDataPassword = new JPasswordField(20);
	/**
	 * Base url of the WMS server. Holds everything except the bbox= argument.
	 */
	private JTextField wmsServerBaseUrl = new JTextField(20);
	/**
	 * Comma seperated import string specifier or <code>null</code> if the first
	 * data line should be interpreted as one.
	 */
	private JTextField csvImportString = new JTextField(20);
	/**
	 * The checkbox stating whether nodes should be merged together.
	 */
	private JCheckBox drawRawGpsLines = new JCheckBox(tr("Draw lines between raw gps points."));
	/**
	 * The checkbox stating whether raw gps lines should be forced.
	 */
	private JCheckBox forceRawGpsLines = new JCheckBox(tr("Force lines if no segments imported."));
	private JCheckBox largeGpsPoints = new JCheckBox(tr("Draw large GPS points."));
	private JCheckBox directionHint = new JCheckBox(tr("Draw Direction Arrows"));
	private JTable colors;

	/**
	 * Combobox with all projections available
	 */
	private JComboBox projectionCombo = new JComboBox(Projection.allProjections);
	private JList annotationSources = new JList(new DefaultListModel());


	/**
	 * Create a preference setting dialog from an preferences-file. If the file does not
	 * exist, it will be created.
	 * If the dialog is closed with Ok, the preferences will be stored to the preferences-
	 * file, otherwise no change of the file happens.
	 */
	public PreferenceDialog() {
		super(JOptionPane.getFrameForComponent(Main.parent), tr("Preferences"));

		// look and feel combo box
		String laf = Main.pref.get("laf");
		for (int i = 0; i < lafCombo.getItemCount(); ++i) {
			if (((LookAndFeelInfo)lafCombo.getItemAt(i)).getClassName().equals(laf)) {
				lafCombo.setSelectedIndex(i);
				break;
			}
		}
		final ListCellRenderer oldRenderer = lafCombo.getRenderer();
		lafCombo.setRenderer(new DefaultListCellRenderer(){
			@Override public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
				return oldRenderer.getListCellRendererComponent(list, ((LookAndFeelInfo)value).getName(), index, isSelected, cellHasFocus);
			}
		});
		lafCombo.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				requiresRestart = true;
			}
		});

		// language
		String lang = Main.pref.get("language");
		for (int i = 0; i < languages.getItemCount(); ++i) {
			if (languages.getItemAt(i).toString().equals(lang)) {
				languages.setSelectedIndex(i);
				break;
			}
		}
		languages.setRenderer(new DefaultListCellRenderer(){
			@Override public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
	            return super.getListCellRendererComponent(list, ((Locale)value).getDisplayName(), index, isSelected, cellHasFocus);
            }
		});

		// projection combo box
		for (int i = 0; i < projectionCombo.getItemCount(); ++i) {
			if (projectionCombo.getItemAt(i).getClass().getName().equals(Main.pref.get("projection"))) {
				projectionCombo.setSelectedIndex(i);
				break;
			}
		}
		projectionCombo.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				requiresRestart = true;
			}
		});

		drawRawGpsLines.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				if (!drawRawGpsLines.isSelected())
					forceRawGpsLines.setSelected(false);
				forceRawGpsLines.setEnabled(drawRawGpsLines.isSelected());
			}
		});

		osmDataServer.setText(Main.pref.get("osm-server.url"));
		osmDataUsername.setText(Main.pref.get("osm-server.username"));
		osmDataPassword.setText(Main.pref.get("osm-server.password"));
		wmsServerBaseUrl.setText(Main.pref.get("wms.baseurl", "http://wms.jpl.nasa.gov/wms.cgi?request=GetMap&width=512&height=512&layers=global_mosaic&styles=&srs=EPSG:4326&format=image/jpeg&"));
		csvImportString.setText(Main.pref.get("csv.importstring"));
		drawRawGpsLines.setSelected(Main.pref.getBoolean("draw.rawgps.lines"));
		forceRawGpsLines.setToolTipText(tr("Force drawing of lines if the imported data contain no line information."));
		forceRawGpsLines.setSelected(Main.pref.getBoolean("draw.rawgps.lines.force"));
		forceRawGpsLines.setEnabled(drawRawGpsLines.isSelected());
		largeGpsPoints.setSelected(Main.pref.getBoolean("draw.rawgps.large"));
		largeGpsPoints.setToolTipText(tr("Draw larger dots for the GPS points."));
		directionHint.setToolTipText(tr("Draw direction hints for all segments."));
		directionHint.setSelected(Main.pref.getBoolean("draw.segment.direction"));

		String annos = Main.pref.get("annotation.sources");
		StringTokenizer st = new StringTokenizer(annos, ";");
		while (st.hasMoreTokens())
			((DefaultListModel)annotationSources.getModel()).addElement(st.nextToken());


		Map<String,String> allColors = new TreeMap<String, String>(Main.pref.getAllPrefix("color."));

		Vector<Vector<Object>> rows = new Vector<Vector<Object>>();
		for (Entry<String,String> e : allColors.entrySet()) {
			Vector<Object> row = new Vector<Object>(2);
			row.add(tr(e.getKey().substring("color.".length())));
			row.add(ColorHelper.html2color(e.getValue()));
			rows.add(row);
		}
		Vector<Object> cols = new Vector<Object>(2);
		cols.add(tr("Color"));
		cols.add(tr("Name"));
		colors = new JTable(rows, cols){
			@Override public boolean isCellEditable(int row, int column) {
				return false;
			}
		};
		colors.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		final TableCellRenderer oldColorsRenderer = colors.getDefaultRenderer(Object.class);
		colors.setDefaultRenderer(Object.class, new TableCellRenderer(){
			public Component getTableCellRendererComponent(JTable t, Object o, boolean selected, boolean focus, int row, int column) {
				if (column == 1) {
					JLabel l = new JLabel(ColorHelper.color2html((Color)o));
					l.setBackground((Color)o);
					l.setOpaque(true);
					return l;
				}
				return oldColorsRenderer.getTableCellRendererComponent(t,o,selected,focus,row,column);
			}
		});
		colors.getColumnModel().getColumn(1).setWidth(100);

		JButton colorEdit = new JButton(tr("Choose"));
		colorEdit.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				if (colors.getSelectedRowCount() == 0) {
					JOptionPane.showMessageDialog(PreferenceDialog.this, tr("Please select a color."));
					return;
				}
				int sel = colors.getSelectedRow();
				JColorChooser chooser = new JColorChooser((Color)colors.getValueAt(sel, 1));
				int answer = JOptionPane.showConfirmDialog(PreferenceDialog.this, chooser, tr("Choose a color for {0}", colors.getValueAt(sel, 0)), JOptionPane.OK_CANCEL_OPTION);
				if (answer == JOptionPane.OK_OPTION)
					colors.setValueAt(chooser.getColor(), sel, 1);
			}
		});

		// Annotation source panels
		JButton addAnno = new JButton(tr("Add"));
		addAnno.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				String source = JOptionPane.showInputDialog(Main.parent, tr("Annotation preset source"));
				if (source == null)
					return;
				((DefaultListModel)annotationSources.getModel()).addElement(source);
				requiresRestart = true;
			}
		});

		JButton editAnno = new JButton(tr("Edit"));
		editAnno.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				if (annotationSources.getSelectedIndex() == -1)
					JOptionPane.showMessageDialog(Main.parent, tr("Please select the row to edit."));
				else {
					String source = JOptionPane.showInputDialog(Main.parent, tr("Annotation preset source"), annotationSources.getSelectedValue());
					if (source == null)
						return;
					((DefaultListModel)annotationSources.getModel()).setElementAt(source, annotationSources.getSelectedIndex());
					requiresRestart = true;
				}
			}
		});

		JButton deleteAnno = new JButton(tr("Delete"));
		deleteAnno.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				if (annotationSources.getSelectedIndex() == -1)
					JOptionPane.showMessageDialog(Main.parent, tr("Please select the row to delete."));
				else {
					((DefaultListModel)annotationSources.getModel()).remove(annotationSources.getSelectedIndex());
					requiresRestart = true;
				}
			}
		});
		annotationSources.setVisibleRowCount(3);



		// setting tooltips
		osmDataServer.setToolTipText(tr("The base URL to the OSM server (REST API)"));
		osmDataUsername.setToolTipText(tr("Login name (email) to the OSM account."));
		osmDataPassword.setToolTipText(tr("Login password to the OSM account. Leave blank to not store any password."));
		wmsServerBaseUrl.setToolTipText(tr("The base URL to the server retrieving WMS background pictures from."));
		csvImportString.setToolTipText(tr("<html>Import string specification. lat/lon and time are imported.<br>" +
				"<b>lat</b>: The latitude coordinate<br>" +
				"<b>lon</b>: The longitude coordinate<br>" +
				"<b>time</b>: The measured time as string<br>" +
				"<b>ignore</b>: Skip this field<br>" +
				"An example: \"ignore ignore lat lon\" will use ' ' as delimiter, skip the first two values and read then lat/lon.<br>" +
		"Other example: \"lat,lon\" will just read lat/lon values comma seperated.</html>"));
		drawRawGpsLines.setToolTipText(tr("If your gps device draw to few lines, select this to draw lines along your way."));
		colors.setToolTipText(tr("Colors used by different objects in JOSM."));
		annotationSources.setToolTipText(tr("The sources (url or filename) of annotation preset definition files. See http://josm.eigenheimstrasse.de/wiki/AnnotationPresets for help."));
		addAnno.setToolTipText(tr("Add a new annotation preset source to the list."));
		deleteAnno.setToolTipText(tr("Delete the selected source from the list."));

		// creating the gui

		// Display tab
		JPanel display = createPreferenceTab("display", tr("Display Settings"), tr("Various settings that influence the visual representation of the whole program."));
		display.add(new JLabel(tr("Look and Feel")), GBC.std());
		display.add(GBC.glue(5,0), GBC.std().fill(GBC.HORIZONTAL));
		display.add(lafCombo, GBC.eol().fill(GBC.HORIZONTAL));
		display.add(new JLabel(tr("Language")), GBC.std());
		display.add(GBC.glue(5,0), GBC.std().fill(GBC.HORIZONTAL));
		display.add(languages, GBC.eol().fill(GBC.HORIZONTAL));
		display.add(drawRawGpsLines, GBC.eol().insets(20,0,0,0));
		display.add(forceRawGpsLines, GBC.eop().insets(40,0,0,0));
		display.add(largeGpsPoints, GBC.eop().insets(20,0,0,0));
		display.add(directionHint, GBC.eop().insets(20,0,0,0));
		display.add(new JLabel(tr("Colors")), GBC.eol());
		colors.setPreferredScrollableViewportSize(new Dimension(100,112));
		display.add(new JScrollPane(colors), GBC.eol().fill(GBC.BOTH));
		display.add(colorEdit, GBC.eol().anchor(GBC.EAST));
		//display.add(Box.createVerticalGlue(), GBC.eol().fill(GBC.VERTICAL));

		// Connection tab
		JPanel con = createPreferenceTab("connection", tr("Connection Settings"), tr("Connection Settings to the OSM server."));
		con.add(new JLabel(tr("Base Server URL")), GBC.std());
		con.add(osmDataServer, GBC.eol().fill(GBC.HORIZONTAL).insets(5,0,0,5));
		con.add(new JLabel(tr("OSM username (email)")), GBC.std());
		con.add(osmDataUsername, GBC.eol().fill(GBC.HORIZONTAL).insets(5,0,0,5));
		con.add(new JLabel(tr("OSM password")), GBC.std());
		con.add(osmDataPassword, GBC.eol().fill(GBC.HORIZONTAL).insets(5,0,0,0));
		JLabel warning = new JLabel(tr("<html>" +
				"WARNING: The password is stored in plain text in the preferences file.<br>" +
				"The password is transfered in plain text to the server, encoded in the url.<br>" +
		"<b>Do not use a valuable Password.</b></html>"));
		warning.setFont(warning.getFont().deriveFont(Font.ITALIC));
		con.add(warning, GBC.eop().fill(GBC.HORIZONTAL));
		//con.add(new JLabel("WMS server base url (everything except bbox-parameter)"), GBC.eol());
		//con.add(wmsServerBaseUrl, GBC.eop().fill(GBC.HORIZONTAL));
		//con.add(Box.createVerticalGlue(), GBC.eol().fill(GBC.VERTICAL));
		con.add(new JLabel(tr("CSV import specification (empty: read from first line in data)")), GBC.eol());
		con.add(csvImportString, GBC.eop().fill(GBC.HORIZONTAL));
		con.add(Box.createVerticalGlue(), GBC.eol().fill(GBC.VERTICAL));

		// Map tab
		JPanel map = createPreferenceTab("map", tr("Map Settings"), tr("Settings for the map projection and data interpretation."));
		map.add(new JLabel(tr("Projection method")), GBC.std());
		map.add(GBC.glue(5,0), GBC.std().fill(GBC.HORIZONTAL));
		map.add(projectionCombo, GBC.eop().fill(GBC.HORIZONTAL).insets(0,0,0,5));
		map.add(new JLabel(tr("Annotation preset sources")), GBC.eol().insets(0,5,0,0));
		map.add(new JScrollPane(annotationSources), GBC.eol().fill(GBC.BOTH));
		map.add(GBC.glue(5, 0), GBC.std().fill(GBC.HORIZONTAL));
		map.add(addAnno, GBC.std());
		map.add(editAnno, GBC.std().insets(5,0,5,0));
		map.add(deleteAnno, GBC.std());

		// I HATE SWING!
		map.add(Box.createVerticalGlue(), GBC.eol().fill(GBC.VERTICAL));
		map.add(Box.createVerticalGlue(), GBC.eol().fill(GBC.VERTICAL));
		map.add(Box.createVerticalGlue(), GBC.eol().fill(GBC.VERTICAL));
		map.add(Box.createVerticalGlue(), GBC.eol().fill(GBC.VERTICAL));
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
		setLocationRelativeTo(Main.parent);
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
}
