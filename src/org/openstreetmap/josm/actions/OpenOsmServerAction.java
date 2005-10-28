package org.openstreetmap.josm.actions;

import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.util.Collection;

import javax.swing.AbstractAction;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.jdom.JDOMException;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.GeoPoint;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.gui.BookmarkList;
import org.openstreetmap.josm.gui.GBC;
import org.openstreetmap.josm.gui.ImageProvider;
import org.openstreetmap.josm.gui.MapFrame;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.gui.BookmarkList.Bookmark;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.gui.layer.RawGpsDataLayer;
import org.openstreetmap.josm.io.OsmServerReader;

/**
 * Action that opens a connection to the osm server.
 * 
 * An dialog is displayed asking the user to specify a rectangle to grab.
 * The url and account settings from the preferences are used.
 *  
 * @author imi
 */
public class OpenOsmServerAction extends AbstractAction {

	JTextField[] latlon = new JTextField[]{
			new JTextField(9),
			new JTextField(9),
			new JTextField(9),
			new JTextField(9)};
	JCheckBox rawGps = new JCheckBox("Open as raw gps data", false);

	public OpenOsmServerAction() {
		super("Connect to OSM", ImageProvider.get("connectosm"));
		putValue(MNEMONIC_KEY, KeyEvent.VK_C);
		putValue(SHORT_DESCRIPTION, "Open a connection to the OSM server.");
	}

	public void actionPerformed(ActionEvent e) {
		JPanel dlg = new JPanel(new GridBagLayout());
		dlg.add(new JLabel("Bounding box"), GBC.eol());

		dlg.add(new JLabel("min lat"), GBC.std().insets(10,0,5,0));
		dlg.add(latlon[0], GBC.std());
		dlg.add(new JLabel("max lat"), GBC.std().insets(10,0,5,0));
		dlg.add(latlon[1], GBC.eol());
		dlg.add(new JLabel("min lon"), GBC.std().insets(10,0,5,0));
		dlg.add(latlon[2], GBC.std());
		dlg.add(new JLabel("max lon"), GBC.std().insets(10,0,5,0));
		dlg.add(latlon[3], GBC.eop());

		if (Main.main.getMapFrame() != null) {
			MapView mv = Main.main.getMapFrame().mapView;
			int w = mv.getWidth();
			int h = mv.getHeight();
			GeoPoint bottomLeft = mv.getPoint(0, h, true);
			GeoPoint topRight = mv.getPoint(w, 0, true);
			latlon[0].setText(""+bottomLeft.lat);
			latlon[1].setText(""+bottomLeft.lon);
			latlon[2].setText(""+topRight.lat);
			latlon[3].setText(""+topRight.lon);
			for (JTextField f : latlon)
				f.setCaretPosition(0);
			rawGps.setSelected(mv.getActiveLayer() instanceof RawGpsDataLayer);
		}

		dlg.add(rawGps, GBC.eop());
		
		// load bookmarks
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
			}
		});
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
				if (!b.name.equals("")) {
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
		
		int r = JOptionPane.showConfirmDialog(Main.main, dlg, "Choose an area", 
				JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
		if (r != JOptionPane.OK_OPTION)
			return;

		Bookmark b = readBookmark();
		if (b == null) {
			JOptionPane.showMessageDialog(Main.main, "Please enter the desired coordinates or click on a bookmark.");
			return;
		}
		OsmServerReader osmReader = new OsmServerReader(Main.pref.osmDataServer,
				b.latlon[0], b.latlon[1], b.latlon[2], b.latlon[3]);
		try {
			String name = latlon[0].getText()+" "+latlon[1].getText()+" x "+
					latlon[2].getText()+" "+latlon[3].getText();
			
			Layer layer;
			if (rawGps.isSelected()) {
				layer = new RawGpsDataLayer(osmReader.parseRawGps(), name);
			} else {
				DataSet dataSet = osmReader.parseOsm();
				if (dataSet == null)
					return; // user cancelled download
				if (dataSet.nodes.isEmpty())
					JOptionPane.showMessageDialog(Main.main, "No data imported.");
				
				Collection<OsmPrimitive> data = Main.main.ds.mergeFrom(dataSet);
				layer = new OsmDataLayer(data, name);
			}

			if (Main.main.getMapFrame() == null)
				Main.main.setMapFrame(name, new MapFrame(layer));
			else
				Main.main.getMapFrame().mapView.addLayer(layer);
		} catch (JDOMException x) {
			x.printStackTrace();
			JOptionPane.showMessageDialog(Main.main, x.getMessage());
		} catch (IOException x) {
			x.printStackTrace();
			JOptionPane.showMessageDialog(Main.main, x.getMessage());
		}
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
}
