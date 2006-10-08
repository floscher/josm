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
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.gui.BookmarkList;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.gui.WorldChooser;
import org.openstreetmap.josm.gui.BookmarkList.Bookmark;
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

	public interface DownloadTask {
		/**
		 * Execute the download.
		 */
		void download(DownloadAction action, double minlat, double minlon, double maxlat, double maxlon);
		/**
		 * @return The checkbox presented to the user
		 */
		JCheckBox getCheckBox();
		/**
		 * @return The name of the preferences suffix to use for storing the
		 * selection state.
		 */
		String getPreferencesSuffix();
	}

	/**
	 * The list of download tasks. First entry should be the osm data entry
	 * and the second the gps entry. After that, plugins can register additional
	 * download possibilities.
	 */
	public final List<DownloadTask> downloadTasks = new ArrayList<DownloadTask>(5);

	/**
	 * minlat, minlon, maxlat, maxlon
	 */
	public JTextField[] latlon = new JTextField[]{
			new JTextField(9),
			new JTextField(9),
			new JTextField(9),
			new JTextField(9)};

	public DownloadAction() {
		super(tr("Download from OSM"), "download", tr("Download map data from the OSM server."), KeyEvent.VK_D, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK);
		// TODO remove when bug in Java6 is fixed
		for (JTextField f : latlon)
			f.setMinimumSize(new Dimension(100,new JTextField().getMinimumSize().height));

		downloadTasks.add(new DownloadOsmTask());
		downloadTasks.add(new DownloadGpsTask());
	}

	public void actionPerformed(ActionEvent e) {
		JPanel dlg = new JPanel(new GridBagLayout());

		// World image
		WorldChooser wc = new WorldChooser();
		dlg.add(wc, GBC.eop());
		wc.setToolTipText(tr("Move and zoom the image like the main map. Select an area to download by dragging."));

		// Bounding box edits
		dlg.add(new JLabel(tr("Bounding box")), GBC.eol());
		dlg.add(new JLabel(tr("min lat")), GBC.std().insets(10,0,5,0));
		dlg.add(latlon[0], GBC.std());
		dlg.add(new JLabel(tr("min lon")), GBC.std().insets(10,0,5,0));
		dlg.add(latlon[1], GBC.eol());
		dlg.add(new JLabel(tr("max lat")), GBC.std().insets(10,0,5,0));
		dlg.add(latlon[2], GBC.std());
		dlg.add(new JLabel(tr("max lon")), GBC.std().insets(10,0,5,0));
		dlg.add(latlon[3], GBC.eol());
		if (Main.map != null) {
			MapView mv = Main.map.mapView;
			setEditBounds(new Bounds(
					mv.getLatLon(0, mv.getHeight()),
					mv.getLatLon(mv.getWidth(), 0)));
		}

		// adding the download tasks
		dlg.add(new JLabel(tr("Download the following data:")), GBC.eol().insets(0,5,0,0));
		for (DownloadTask task : downloadTasks) {
			dlg.add(task.getCheckBox(), GBC.eol().insets(20,0,0,0));
			task.getCheckBox().setSelected(Main.pref.getBoolean("download."+task.getPreferencesSuffix()));
		}

		// OSM url edit
		dlg.add(new JLabel(tr("URL from www.openstreetmap.org")), GBC.eol().insets(0,5,0,0));
		final JTextField osmUrl = new JTextField();
		dlg.add(osmUrl, GBC.eop().fill(GBC.HORIZONTAL));
		final KeyListener osmUrlRefresher = new KeyAdapter(){
			@Override public void keyTyped(KeyEvent e) {
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						try {
							double latMin = Double.parseDouble(latlon[0].getText());
							double lonMin = Double.parseDouble(latlon[1].getText());
							double latMax = Double.parseDouble(latlon[2].getText());
							double lonMax = Double.parseDouble(latlon[3].getText());
							double lat = (latMax+latMin)/2;
							double lon = (lonMax+lonMin)/2;
							// convert to mercator (for calculation of zoom only)
							latMin = Math.log(Math.tan(Math.PI/4.0+latMin/180.0*Math.PI/2.0))*180.0/Math.PI;
							latMax = Math.log(Math.tan(Math.PI/4.0+latMax/180.0*Math.PI/2.0))*180.0/Math.PI;
							double size = Math.max(Math.abs(latMax-latMin), Math.abs(lonMax-lonMin));
							int zoom = 0;
							while (zoom <= 20) {
								if (size >= 180)
									break;
								size *= 2;
								zoom++;
							}
							osmUrl.setText("http://www.openstreetmap.org/index.html?lat="+lat+"&lon="+lon+"&zoom="+zoom);
						} catch (NumberFormatException x) {
							osmUrl.setText("");
						}
						osmUrl.setCaretPosition(0);
					}
				});
			}
		};
		for (JTextField f : latlon)
			f.addKeyListener(osmUrlRefresher);
		SwingUtilities.invokeLater(new Runnable() {public void run() {osmUrlRefresher.keyTyped(null);}});
		osmUrl.addKeyListener(new KeyAdapter(){
			@Override public void keyTyped(KeyEvent e) {
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						Bounds b = osmurl2bounds(osmUrl.getText());
						if (b != null)
							setEditBounds(b);
						else
							for (JTextField f : latlon)
								f.setText("");
					}
				});
			}
		});

		// Bookmarks
		dlg.add(new JLabel(tr("Bookmarks")), GBC.eol());
		final BookmarkList bookmarks = new BookmarkList();
		bookmarks.getSelectionModel().addListSelectionListener(new ListSelectionListener(){
			public void valueChanged(ListSelectionEvent e) {
				Bookmark b = (Bookmark)bookmarks.getSelectedValue();
				for (int i = 0; i < 4; ++i) {
					latlon[i].setText(b == null ? "" : ""+b.latlon[i]);
					latlon[i].setCaretPosition(0);
				}
				osmUrlRefresher.keyTyped(null);
			}
		});
		wc.addListMarker(bookmarks);
		dlg.add(new JScrollPane(bookmarks), GBC.eol().fill());

		JPanel buttons = new JPanel(new GridLayout(1,2));
		JButton add = new JButton(tr("Add"));
		add.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				Bookmark b = readBookmark();
				if (b == null) {
					JOptionPane.showMessageDialog(Main.parent, tr("Please enter the desired coordinates first."));
					return;
				}
				b.name = JOptionPane.showInputDialog(Main.parent,tr("Please enter a name for the location."));
				if (b.name != null && !b.name.equals("")) {
					((DefaultListModel)bookmarks.getModel()).addElement(b);
					bookmarks.save();
				}
			}
		});
		buttons.add(add);
		JButton remove = new JButton(tr("Remove"));
		remove.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				Object sel = bookmarks.getSelectedValue();
				if (sel == null) {
					JOptionPane.showMessageDialog(Main.parent,tr("Select a bookmark first."));
					return;
				}
				((DefaultListModel)bookmarks.getModel()).removeElement(sel);
				bookmarks.save();
			}
		});
		buttons.add(remove);
		dlg.add(buttons, GBC.eop().fill(GBC.HORIZONTAL));

		Dimension d = dlg.getPreferredSize();
		wc.setPreferredSize(new Dimension(d.width, d.width/2));
		wc.addInputFields(latlon, osmUrl, osmUrlRefresher);

		// Finally: the dialog
		Bookmark b;
		boolean anySelected = false;
		do {
			final JOptionPane pane = new JOptionPane(dlg, JOptionPane.PLAIN_MESSAGE, JOptionPane.OK_CANCEL_OPTION);
			final JDialog panedlg = pane.createDialog(Main.parent, tr("Choose an area"));
			bookmarks.addMouseListener(new MouseAdapter(){
				@Override public void mouseClicked(MouseEvent e) {
	                if (e.getClickCount() >= 2) {
	    				pane.setValue(JOptionPane.OK_OPTION);
	    				panedlg.setVisible(false);
	                }
                }
			});
			panedlg.setVisible(true);
			Object answer = pane.getValue();
			if (answer == null || answer == JOptionPane.UNINITIALIZED_VALUE || (answer instanceof Integer && (Integer)answer != JOptionPane.OK_OPTION))
				return;
			b = readBookmark();

			for (DownloadTask task : downloadTasks) {
				if (task.getCheckBox().isSelected()) {
					anySelected = true;
					break;
				}
			}

			if (b == null)
				JOptionPane.showMessageDialog(Main.parent,tr("Please enter the desired coordinates or click on a bookmark."));
			else if (!anySelected)
				JOptionPane.showMessageDialog(Main.parent,tr("Please select at least one download data type."));
		} while (b == null || !anySelected);

		double minlon = b.latlon[0];
		double minlat = b.latlon[1];
		double maxlon = b.latlon[2];
		double maxlat = b.latlon[3];
		download(minlon, minlat, maxlon, maxlat);
	}

	/**
	 * Read a bookmark from the current set edit fields. If one of the fields is
	 * empty or contain illegal chars, <code>null</code> is returned.
	 * The name of the bookmark is <code>null</code>.
	 * @return A bookmark containing information from the edit fields and rawgps
	 * 		checkbox.
	 */
	Bookmark readBookmark() {
		try {
			Bookmark b = new Bookmark();
			for (int i = 0; i < 4; ++i) {
				if (latlon[i].getText().equals(""))
					return null;
				b.latlon[i] = Double.parseDouble(latlon[i].getText());
			}
			return b;
		} catch (NumberFormatException x) {
			return null;
		}
	}

	public static Bounds osmurl2bounds(String url) {
		int i = url.indexOf('?');
		if (i == -1)
			return null;
		String[] args = url.substring(i+1).split("&");
		HashMap<String, Double> map = new HashMap<String, Double>();
		for (String arg : args) {
			int eq = arg.indexOf('=');
			if (eq != -1) {
				try {
					map.put(arg.substring(0, eq), Double.parseDouble(arg.substring(eq + 1)));
				} catch (NumberFormatException e) {
				}
			}
		}
		try {
			double size = 180.0 / Math.pow(2, map.get("zoom"));
			return new Bounds(
					new LatLon(map.get("lat") - size/2, map.get("lon") - size),
					new LatLon(map.get("lat") + size/2, map.get("lon") + size));
		} catch (Exception x) { // NPE or IAE
			return null;
		}
	}

	/**
	 * Set the four edit fields to the given bounds coordinates.
	 */
	private void setEditBounds(Bounds b) {
		LatLon bottomLeft = b.min;
		LatLon topRight = b.max;
		if (bottomLeft.isOutSideWorld())
			bottomLeft = new LatLon(-89.999, -179.999); // do not use the Projection constants, since this looks better.
		if (topRight.isOutSideWorld())
			topRight = new LatLon(89.999, 179.999);
		latlon[0].setText(""+bottomLeft.lat());
		latlon[1].setText(""+bottomLeft.lon());
		latlon[2].setText(""+topRight.lat());
		latlon[3].setText(""+topRight.lon());
		for (JTextField f : latlon)
			f.setCaretPosition(0);
	}

	/**
	 * Do the download for the given area.
	 */
	public void download(double minlat, double minlon, double maxlat, double maxlon) {
		for (DownloadTask task : downloadTasks) {
			Main.pref.put("download."+task.getPreferencesSuffix(), task.getCheckBox().isSelected());
			if (task.getCheckBox().isSelected()) {
				task.download(this, minlat, minlon, maxlat, maxlon);
			}
		}
	}
}
