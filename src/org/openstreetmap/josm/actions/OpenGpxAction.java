package org.openstreetmap.josm.actions;

import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collection;

import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.filechooser.FileFilter;

import org.jdom.JDOMException;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.GeoPoint;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.gui.GBC;
import org.openstreetmap.josm.gui.ImageProvider;
import org.openstreetmap.josm.gui.MapFrame;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.gui.layer.RawGpsDataLayer;
import org.openstreetmap.josm.io.GpxReader;
import org.openstreetmap.josm.io.RawGpsReader;

/**
 * Open a file chooser dialog and select an file to import. Than call the gpx-import
 * driver. Finally open an internal frame into the main window with the gpx data shown.
 * 
 * @author imi
 */
public class OpenGpxAction extends AbstractAction {

	/**
	 * Create an open action. The name is "Open GPX".
	 */
	public OpenGpxAction() {
		super("Open GPX", ImageProvider.get("opengpx"));
		putValue(ACCELERATOR_KEY, KeyStroke.getAWTKeyStroke(KeyEvent.VK_O, 
				InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK));
		putValue(MNEMONIC_KEY, KeyEvent.VK_O);
		putValue(SHORT_DESCRIPTION, "Open a file in GPX format.");
	}

	public void actionPerformed(ActionEvent e) {
		JFileChooser fc = new JFileChooser("data");
		fc.setFileFilter(new FileFilter(){
			@Override
			public boolean accept(File f) {
				String name = f.getName().toLowerCase();
				return f.isDirectory() || name.endsWith(".gpx") || name.endsWith(".xml");
			}
			@Override
			public String getDescription() {
				return "GPX or XML Files";
			}});
		
		// additional options
		JCheckBox rawGps = new JCheckBox("Raw GPS data", true);
		rawGps.setToolTipText("Check this, if the data are obtained from a gps device.");
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
		
		File gpxFile = fc.getSelectedFile();
		if (gpxFile == null)
			return;
		
		try {
			Layer layer;
			if (rawGps.isSelected()) {
				Collection<Collection<GeoPoint>> data = new RawGpsReader(new FileReader(gpxFile)).parse();
				layer = new RawGpsDataLayer(data, gpxFile.getName());
			} else {
				DataSet dataSet = new GpxReader(new FileReader(gpxFile)).parse();
				Collection<OsmPrimitive> l = Main.main.ds.mergeFrom(dataSet);
				layer = new OsmDataLayer(l, gpxFile.getName());
			}
			
			if (Main.main.getMapFrame() == null || !newLayer.isSelected())
				Main.main.setMapFrame(gpxFile.getName(), new MapFrame(layer));
			else
				Main.main.getMapFrame().mapView.addLayer(layer);
			
		} catch (JDOMException x) {
			x.printStackTrace();
			JOptionPane.showMessageDialog(Main.main, x.getMessage());
		} catch (IOException x) {
			x.printStackTrace();
			JOptionPane.showMessageDialog(Main.main, "Could not read '"+gpxFile.getName()+"'\n"+x.getMessage());
		}
	}
}
