package org.openstreetmap.josm.gui.dialogs;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JColorChooser;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.GpxExportAction;
import org.openstreetmap.josm.actions.SaveAction;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.gui.layer.RawGpsDataLayer;
import org.openstreetmap.josm.gui.layer.WmsServerLayer;
import org.openstreetmap.josm.tools.ColorHelper;
import org.openstreetmap.josm.tools.ImageProvider;

/**
 * Popup menu handler for the layer list.
 */
public class LayerListPopup extends JPopupMenu {

	public LayerListPopup(final JList layers, final Layer layer) {
        add(new LayerList.ShowHideLayerAction(layers, layer));
        add(new LayerList.DeleteLayerAction(layers, layer));
        addSeparator();
        
		if (layer instanceof OsmDataLayer)
			add(new JMenuItem(new SaveAction()));

        if (!(layer instanceof WmsServerLayer))
            add(new JMenuItem(new GpxExportAction(layer)));

		if (layer instanceof RawGpsDataLayer) {
			JMenuItem color = new JMenuItem("Customize Color", ImageProvider.get("colorchooser"));
			color.addActionListener(new ActionListener(){
				public void actionPerformed(ActionEvent e) {
					String col = Main.pref.get("color.layer "+layer.name, Main.pref.get("color.gps point", ColorHelper.color2html(Color.gray)));
					JColorChooser c = new JColorChooser(ColorHelper.html2color(col));
					Object[] options = new Object[]{"OK", "Cancel", "Default"};
					int answer = JOptionPane.showOptionDialog(Main.main, c, "Choose a color", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, null, options, options[0]);
					switch (answer) {
					case 0:
						Main.pref.put("color.layer "+layer.name, ColorHelper.color2html(c.getColor()));
						break;
					case 1:
						return;
					case 2:
						Main.pref.put("color.layer "+layer.name, null);
						break;
					}
					Main.main.repaint();
				}
			});
			add(color);
		}

        if (!(layer instanceof WmsServerLayer))
            addSeparator();

		JMenuItem info = new JMenuItem("Info", ImageProvider.get("info"));
		info.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				JOptionPane.showMessageDialog(Main.main, layer.getInfoComponent());
			}
		});
		add(info);
	}
}
