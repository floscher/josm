package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.downloadtasks.DownloadGpsTask;
import org.openstreetmap.josm.actions.downloadtasks.DownloadOsmTask;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.Preferences;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.gui.BookmarkList;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.gui.download.DownloadDialog;
import org.openstreetmap.josm.gui.download.WorldChooser;
import org.openstreetmap.josm.gui.download.DownloadDialog.DownloadTask;
import org.openstreetmap.josm.gui.preferences.PreferenceDialog;
import org.openstreetmap.josm.tools.GBC;

/**
 * Action that opens a connection to the osm server and download map data.
 *
 * An dialog is displayed asking the user to specify a rectangle to grab.
 * The url and account settings from the preferences are used.
 *
 * @author imi
 */
public class DownloadAction extends JosmAction {
	
	public DownloadDialog dialog;
	
	public DownloadAction() {
		super(tr("Download from OSM"), "download", tr("Download map data from the OSM server."), KeyEvent.VK_D, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK, true);
	}

	public void actionPerformed(ActionEvent e) {
		dialog = new DownloadDialog(Integer.parseInt(Main.pref.get("download.tab", "0")));
		
		JPanel downPanel = new JPanel(new GridBagLayout());
		downPanel.add(dialog, GBC.eol().fill(GBC.BOTH));

		JOptionPane pane = new JOptionPane(downPanel, JOptionPane.PLAIN_MESSAGE, JOptionPane.OK_CANCEL_OPTION);
		JDialog dlg = pane.createDialog(Main.parent, tr("Download"));

		if (dlg.getWidth() > 1000)
			dlg.setSize(1000, dlg.getHeight());
		if (dlg.getHeight() > 600)
			dlg.setSize(dlg.getWidth(),600);

		dlg.setVisible(true);
		if (pane.getValue() instanceof Integer && (Integer)pane.getValue() == JOptionPane.OK_OPTION) {
			Main.pref.put("download.tab", Integer.toString(dialog.getSelectedTab()));
			for (DownloadTask task : dialog.downloadTasks) {
				Main.pref.put("download."+task.getPreferencesSuffix(), task.getCheckBox().isSelected());
				if (task.getCheckBox().isSelected()) {
					task.download(this, dialog.minlat, dialog.minlon, dialog.maxlat, dialog.maxlon);
				}
			}
		}
	}
}
