package org.openstreetmap.josm.actions;

import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.jdom.JDOMException;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.Preferences.PreferencesException;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.gui.BookmarkList;
import org.openstreetmap.josm.gui.MapFrame;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.gui.WorldChooser;
import org.openstreetmap.josm.gui.BookmarkList.Bookmark;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.gui.layer.RawGpsDataLayer;
import org.openstreetmap.josm.io.OsmServerReader;
import org.openstreetmap.josm.tools.GBC;
import org.xml.sax.SAXException;

/**
 * Action that opens a connection to the osm server and download map data.
 * 
 * An dialog is displayed asking the user to specify a rectangle to grab.
 * The url and account settings from the preferences are used.
 *  
 * @author imi
 */
public class DownloadAction extends JosmAction {

    private enum DownloadStatus {FINISHED, REDISPLAY}

    /**
     * minlat, minlon, maxlat, maxlon
     */
	JTextField[] latlon = new JTextField[]{
			new JTextField(9),
			new JTextField(9),
			new JTextField(9),
			new JTextField(9)};
	JCheckBox rawGps = new JCheckBox("Open as raw gps data", false);

	public DownloadAction() {
		super("Download from OSM", "download", "Download map data from the OSM server.", "Ctrl-Shift-D", 
				KeyStroke.getKeyStroke(KeyEvent.VK_D, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK));
		// TODO remove when bug in Java6 is fixed
		for (JTextField f : latlon)
			f.setMinimumSize(new Dimension(100,new JTextField().getMinimumSize().height));
	}

	public void actionPerformed(ActionEvent e) {
		
		//TODO: Remove this in later versions (temporary only)
		if (Main.pref.osmDataServer.endsWith("/0.2") || Main.pref.osmDataServer.endsWith("/0.2/")) {
			int answer = JOptionPane.showConfirmDialog(Main.main, 
					"You seem to have an outdated server entry in your preferences.\n" +
					"\n" +
					"As of JOSM Release 1.2, you must no longer specify the API version in\n" +
					"the osm url. For the OSM standard server, use http://www.openstreetmap.org/api" +
					"\n" +
					"Fix settings and continue?", "Outdated server url detected.", JOptionPane.YES_NO_OPTION);
			if (answer != JOptionPane.YES_OPTION)
				return;
			int cutPos = Main.pref.osmDataServer.endsWith("/0.2") ? 4 : 5;
			Main.pref.osmDataServer = Main.pref.osmDataServer.substring(0, Main.pref.osmDataServer.length()-cutPos);
			try {
				Main.pref.save();
			} catch (PreferencesException x) {
				x.printStackTrace();
				JOptionPane.showMessageDialog(Main.main, "Could not save the preferences chane:\n" +
						x.getMessage());
			}
		}

		JPanel dlg = new JPanel(new GridBagLayout());

		// World image
		WorldChooser wc = new WorldChooser();
		dlg.add(wc, GBC.eop());
		wc.setToolTipText("Move and zoom the image like the main map. Select an area to download by dragging.");

		// Bounding box edits
		dlg.add(new JLabel("Bounding box"), GBC.eol());
		dlg.add(new JLabel("min lat"), GBC.std().insets(10,0,5,0));
		dlg.add(latlon[0], GBC.std());
		dlg.add(new JLabel("min lon"), GBC.std().insets(10,0,5,0));
		dlg.add(latlon[1], GBC.eol());
		dlg.add(new JLabel("max lat"), GBC.std().insets(10,0,5,0));
		dlg.add(latlon[2], GBC.std());
		dlg.add(new JLabel("max lon"), GBC.std().insets(10,0,5,0));
		dlg.add(latlon[3], GBC.eol());
		if (Main.main.getMapFrame() != null) {
			MapView mv = Main.main.getMapFrame().mapView;
			setEditBounds(new Bounds(
					mv.getLatLon(0, mv.getHeight()),
					mv.getLatLon(mv.getWidth(), 0)));
			rawGps.setSelected(mv.getActiveLayer() instanceof RawGpsDataLayer);
		}
		dlg.add(rawGps, GBC.eop());
		
		// OSM url edit
		dlg.add(new JLabel("URL from www.openstreetmap.org"), GBC.eol());
		final JTextField osmUrl = new JTextField();
		dlg.add(osmUrl, GBC.eop().fill(GBC.HORIZONTAL));
		final KeyListener osmUrlRefresher = new KeyAdapter(){
			@Override
			public void keyTyped(KeyEvent e) {
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
			@Override
			public void keyTyped(KeyEvent e) {
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						Map<String, Double> map = readArgs(osmUrl.getText());
						try {
							double size = 180.0 / Math.pow(2, map.get("zoom"));
							Bounds b = new Bounds(
									new LatLon(map.get("lat") - size/2, map.get("lon") - size),
									new LatLon(map.get("lat") + size/2, map.get("lon") + size));
							setEditBounds(b);
						} catch (Exception x) { // NPE or IAE
							for (JTextField f : latlon)
								f.setText("");
						}
					}
				});
			}
		});
		
		// Bookmarks
		dlg.add(new JLabel("Bookmarks"), GBC.eol());
		final BookmarkList bookmarks = new BookmarkList();
		bookmarks.getSelectionModel().addListSelectionListener(new ListSelectionListener(){
			public void valueChanged(ListSelectionEvent e) {
				Bookmark b = (Bookmark)bookmarks.getSelectedValue();
				for (int i = 0; i < 4; ++i) {
					latlon[i].setText(b == null ? "" : ""+b.latlon[i]);
					latlon[i].setCaretPosition(0);
				}
				rawGps.setSelected(b == null ? false : b.rawgps);
				osmUrlRefresher.keyTyped(null);
			}
		});
		wc.addListMarker(bookmarks);
		dlg.add(new JScrollPane(bookmarks), GBC.eol().fill());

		JPanel buttons = new JPanel(new GridLayout(1,2));
		JButton add = new JButton("Add");
		add.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				Bookmark b = readBookmark();
				if (b == null) {
					JOptionPane.showMessageDialog(Main.main, "Please enter the desired coordinates first.");
					return;
				}
				b.name = JOptionPane.showInputDialog(Main.main, "Please enter a name for the location.");
				if (b.name != null && !b.name.equals("")) {
					((DefaultListModel)bookmarks.getModel()).addElement(b);
					bookmarks.save();
				}
			}
		});
		buttons.add(add);
		JButton remove = new JButton("Remove");
		remove.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				Object sel = bookmarks.getSelectedValue();
				if (sel == null) {
					JOptionPane.showMessageDialog(Main.main, "Select a bookmark first.");
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
		while(true) {
			int r = JOptionPane.showConfirmDialog(Main.main, dlg, "Choose an area", 
					JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
			if (r != JOptionPane.OK_OPTION)
				return;
			if (startDownload() == DownloadStatus.FINISHED) 
				break;
		}
	}

	/**
	 * Read the values from the edit fields and from the download.
	 */
	private DownloadStatus startDownload() {
		Bookmark b = readBookmark();
		if (b == null) {
			JOptionPane.showMessageDialog(Main.main, "Please enter the desired coordinates or click on a bookmark.");
			return DownloadStatus.REDISPLAY;
		}

		double minlon = b.latlon[0];
		double minlat = b.latlon[1];
		double maxlon = b.latlon[2];
		double maxlat = b.latlon[3];
		download(rawGps.isSelected(), minlon, minlat, maxlon, maxlat);
		return DownloadStatus.FINISHED;
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
			b.rawgps = rawGps.isSelected();
			return b;
		} catch (NumberFormatException x) {
			return null;
		}
	}
	
	
	/**
	 * Extrakt URL arguments.
	 */
	private Map<String, Double> readArgs(String s) {
		int i = s.indexOf('?');
		if (i == -1)
			return new HashMap<String, Double>();
		String[] args = s.substring(i+1).split("&");
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
		return map;
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
	public void download(final boolean rawGps, double minlat, double minlon, double maxlat, double maxlon) {
		final OsmServerReader osmReader = new OsmServerReader(minlat, minlon, maxlat, maxlon);
		new Thread(new PleaseWaitRunnable("Downloading data"){
			@Override
			public void realRun() {
				try {
					String name = latlon[0].getText() + " " + latlon[1].getText() + " x " + latlon[2].getText() + " " + latlon[3].getText();

					Layer layer = null;
					if (rawGps) {
						layer = new RawGpsDataLayer(osmReader.parseRawGps(), name);
					} else {
						DataSet dataSet = osmReader.parseOsm();
						if (dataSet == null)
							return; // user cancelled download
						if (dataSet.nodes.isEmpty()) {
							closeDialog();
							JOptionPane.showMessageDialog(Main.main,
							"No data imported.");
						}

						layer = new OsmDataLayer(dataSet, "Data Layer", false);
					}

					if (Main.main.getMapFrame() == null)
						Main.main.setMapFrame(new MapFrame(layer));
					else
						Main.main.getMapFrame().mapView.addLayer(layer);
				} catch (SAXException x) {
					closeDialog();
					x.printStackTrace();
					JOptionPane.showMessageDialog(Main.main, x.getMessage());
				} catch (JDOMException x) {
					closeDialog();
					x.printStackTrace();
					JOptionPane.showMessageDialog(Main.main, x.getMessage());
				} catch (FileNotFoundException x) {
					closeDialog();
					x.printStackTrace();
					JOptionPane.showMessageDialog(Main.main,
							"URL nicht gefunden: " + x.getMessage());
				} catch (IOException x) {
					closeDialog();
					x.printStackTrace();
					JOptionPane.showMessageDialog(Main.main, x.getMessage());
				}
			}
		}).start();
	}
}
