package org.openstreetmap.josm.actions;

import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.filechooser.FileFilter;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.command.DataSet;
import org.openstreetmap.josm.gui.GBC;
import org.openstreetmap.josm.gui.ImageProvider;
import org.openstreetmap.josm.gui.MapFrame;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.LayerFactory;
import org.openstreetmap.josm.io.GpxReader;
import org.openstreetmap.josm.io.DataReader.ConnectionException;
import org.openstreetmap.josm.io.DataReader.ParseException;

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
			DataSet dataSet = new GpxReader(new FileReader(gpxFile), rawGps.isSelected()).parse();

			Layer layer = LayerFactory.create(dataSet, gpxFile.getName(), rawGps.isSelected());
			
			if (Main.main.getMapFrame() == null || !newLayer.isSelected())
				Main.main.setMapFrame(gpxFile.getName(), new MapFrame(layer));
			else
				Main.main.getMapFrame().mapView.addLayer(layer);
			
		} catch (ParseException x) {
			x.printStackTrace();
			JOptionPane.showMessageDialog(Main.main, x.getMessage());
		} catch (IOException x) {
			x.printStackTrace();
			JOptionPane.showMessageDialog(Main.main, "Could not read '"+gpxFile.getName()+"'\n"+x.getMessage());
		} catch (ConnectionException x) {
			x.printStackTrace();
			JOptionPane.showMessageDialog(Main.main, x.getMessage());
		}
	}
}
