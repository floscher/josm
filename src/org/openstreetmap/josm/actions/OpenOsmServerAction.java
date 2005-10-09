package org.openstreetmap.josm.actions;

import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.AbstractAction;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.openstreetmap.josm.data.GeoPoint;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.gui.GBC;
import org.openstreetmap.josm.gui.ImageProvider;
import org.openstreetmap.josm.gui.Main;
import org.openstreetmap.josm.gui.MapFrame;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.LayerFactory;
import org.openstreetmap.josm.io.OsmReader;
import org.openstreetmap.josm.io.DataReader.ConnectionException;
import org.openstreetmap.josm.io.DataReader.ParseException;

/**
 * Action that opens a connection to the osm server.
 * 
 * An dialog is displayed asking the user to specify a rectangle to grab.
 * The url and account settings from the preferences are used.
 *  
 * @author imi
 */
public class OpenOsmServerAction extends AbstractAction {

	public OpenOsmServerAction() {
		super("Connect to OSM", ImageProvider.get("connectosm"));
		putValue(MNEMONIC_KEY, KeyEvent.VK_C);
		putValue(SHORT_DESCRIPTION, "Open a connection to the OSM server.");
	}

	public void actionPerformed(ActionEvent e) {
		JPanel dlg = new JPanel(new GridBagLayout());
		dlg.add(new JLabel("Bounding box"), GBC.eol());

		JTextField minLat = new JTextField(9);
		JTextField minLon = new JTextField(9);
		JTextField maxLat = new JTextField(9);
		JTextField maxLon = new JTextField(9);

		dlg.add(new JLabel("min lat"), GBC.std().insets(10,0,5,0));
		dlg.add(minLat, GBC.std());
		dlg.add(new JLabel("max lat"), GBC.std().insets(10,0,5,0));
		dlg.add(maxLat, GBC.eol());
		dlg.add(new JLabel("min lon"), GBC.std().insets(10,0,5,0));
		dlg.add(minLon, GBC.std());
		dlg.add(new JLabel("max lon"), GBC.std().insets(10,0,5,0));
		dlg.add(maxLon, GBC.eol());

		if (Main.main.getMapFrame() != null) {
			MapView mv = Main.main.getMapFrame().mapView;
			int w = mv.getWidth();
			int h = mv.getHeight();
			GeoPoint bottomLeft = mv.getPoint(0, h, true);
			GeoPoint topRight = mv.getPoint(w, 0, true);
			minLat.setText(""+bottomLeft.lat);
			minLon.setText(""+bottomLeft.lon);
			maxLat.setText(""+topRight.lat);
			maxLon.setText(""+topRight.lon);
			
			minLat.setCaretPosition(0);
			minLon.setCaretPosition(0);
			maxLat.setCaretPosition(0);
			maxLon.setCaretPosition(0);
		}

		int r = JOptionPane.showConfirmDialog(Main.main, dlg, "Choose an area", 
				JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
		if (r != JOptionPane.OK_OPTION)
			return;

		OsmReader osmReader = new OsmReader(Main.pref.osmDataServer,
				Main.pref.osmDataUsername, Main.pref.osmDataPassword,
				Double.parseDouble(minLat.getText()),
				Double.parseDouble(minLon.getText()),
				Double.parseDouble(maxLat.getText()),
				Double.parseDouble(maxLon.getText()));
		try {
			DataSet dataSet = osmReader.parse();

			String name = minLat.getText()+" "+minLon.getText()+" x "+
					maxLat.getText()+" "+maxLon.getText();
			
			Layer layer = LayerFactory.create(dataSet, name, false);

			if (Main.main.getMapFrame() == null)
				Main.main.setMapFrame(name, new MapFrame(layer));
			else
				Main.main.getMapFrame().mapView.addLayer(layer);
		} catch (ParseException x) {
			x.printStackTrace();
			JOptionPane.showMessageDialog(Main.main, x.getMessage());
		} catch (ConnectionException x) {
			x.printStackTrace();
			JOptionPane.showMessageDialog(Main.main, x.getMessage());
		}
	}
}
