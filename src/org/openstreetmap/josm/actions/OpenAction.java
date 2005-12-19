package org.openstreetmap.josm.actions;

import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collection;

import javax.swing.Box;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.KeyStroke;

import org.jdom.JDOMException;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.GeoPoint;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.gui.GBC;
import org.openstreetmap.josm.gui.MapFrame;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.gui.layer.RawGpsDataLayer;
import org.openstreetmap.josm.io.GpxReader;
import org.openstreetmap.josm.io.OsmReader;
import org.openstreetmap.josm.io.RawGpsReader;

/**
 * Open a file chooser dialog and select an file to import. Than call the gpx-import
 * driver. Finally open an internal frame into the main window with the gpx data shown.
 * 
 * @author imi
 */
public class OpenAction extends JosmAction {

	/**
	 * Create an open action. The name is "Open GPX".
	 */
	public OpenAction() {
		super("Open", "open", "Open a file.", null, KeyStroke.getAWTKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_DOWN_MASK));
	}

	public void actionPerformed(ActionEvent e) {
		JFileChooser fc = new JFileChooser("data");
		for (int i = 0; i < ExtensionFileFilter.filters.length; ++i)
			fc.addChoosableFileFilter(ExtensionFileFilter.filters[i]);
		fc.setAcceptAllFileFilterUsed(true);
		
		// additional options
		JCheckBox rawGps = new JCheckBox("Raw GPS data", true);
		rawGps.setToolTipText("Check this, if the data were obtained from a gps device.");
		JCheckBox newLayer = new JCheckBox("As Layer", true); 
		newLayer.setToolTipText("Open as a new layer or replace all current layers.");
		if (Main.main.getMapFrame() == null) {
			newLayer.setEnabled(false);
			newLayer.setSelected(false);
		}
		
		JPanel p = new JPanel(new GridBagLayout());
		p.add(new JLabel("Options"), GBC.eop());
		p.add(rawGps, GBC.eol());
		p.add(newLayer, GBC.eol());
		p.add(Box.createVerticalGlue(), GBC.eol().fill());
		fc.setAccessory(p);

		if (fc.showOpenDialog(Main.main) != JFileChooser.APPROVE_OPTION)
			return;
		
		File filename = fc.getSelectedFile();
		if (filename == null)
			return;

		try {
			Layer layer;
			if (rawGps.isSelected()) {
				Collection<Collection<GeoPoint>> data = new RawGpsReader(new FileReader(filename)).parse();
				layer = new RawGpsDataLayer(data, filename.getName());
			} else {
				DataSet dataSet = filename.getName().toLowerCase().endsWith(".gpx") ?
						new GpxReader(new FileReader(filename)).parse() :
						new OsmReader(new FileReader(filename)).parse();
				layer = new OsmDataLayer(dataSet, filename.getName());
			}
			
			if (Main.main.getMapFrame() == null || !newLayer.isSelected())
				Main.main.setMapFrame(filename.getName(), new MapFrame(layer));
			else
				Main.main.getMapFrame().mapView.addLayer(layer);

		} catch (JDOMException x) {
			x.printStackTrace();
			JOptionPane.showMessageDialog(Main.main, x.getMessage());
		} catch (IOException x) {
			x.printStackTrace();
			JOptionPane.showMessageDialog(Main.main, "Could not read '"+filename.getName()+"'\n"+x.getMessage());
		}
	}
}
