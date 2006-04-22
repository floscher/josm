package org.openstreetmap.josm.actions;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.AbstractButton;
import javax.swing.JOptionPane;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.gui.MapFrame;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.WmsServerLayer;

public class WmsServerAction extends JosmAction {

    public WmsServerAction() {
        super("Show background", "wmsserver", "Download and show landsat background images.", KeyEvent.VK_B);
    }

    public void actionPerformed(ActionEvent e) {
    	JOptionPane.showMessageDialog(Main.main, "Not implemented yet.");
    	if (1==1) return;
        MapFrame mapFrame = Main.main.getMapFrame();
        if (!((AbstractButton)e.getSource()).isSelected()) {
            if (mapFrame != null) {
                MapView mv = mapFrame.mapView;
                for (Layer l : mv.getAllLayers()) {
                    if (l instanceof WmsServerLayer) {
                        if (mv.getAllLayers().size() == 1) {
                            Main.main.setMapFrame(null);
                            Main.ds = new DataSet();
                        } else
                            mv.removeLayer(l);
                        return;
                    }
                }
            }
        } else {
            WmsServerLayer layer = new WmsServerLayer(Main.pref.get("wms.baseurl", "http://wms.jpl.nasa.gov/wms.cgi?request=GetMap&width=512&height=512&layers=global_mosaic&styles=&srs=EPSG:4326&format=image/jpeg&"));
            if (mapFrame == null)
                Main.main.setMapFrame(new MapFrame(layer));
            else
                mapFrame.mapView.addLayer(layer);
        }
    }
}
