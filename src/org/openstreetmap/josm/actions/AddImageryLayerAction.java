// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JOptionPane;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.imagery.ImageryInfo;
import org.openstreetmap.josm.data.imagery.ImageryInfo.ImageryType;
import org.openstreetmap.josm.gui.actionsupport.AlignImageryPanel;
import org.openstreetmap.josm.gui.layer.ImageryLayer;
import org.openstreetmap.josm.tools.ImageProvider;

public class AddImageryLayerAction extends JosmAction implements AdaptableAction {

    private static final int MAX_ICON_SIZE = 24;
    private final ImageryInfo info;

    public AddImageryLayerAction(ImageryInfo info) {
        super(info.getMenuName(), /* ICON */"imagery_menu", tr("Add imagery layer {0}",info.getName()), null, false, false);
        putValue("toolbar", "imagery_" + info.getToolbarName());
        this.info = info;
        installAdapters();

        // change toolbar icon from if specified
        try {
            if (info.getIcon() != null) {
                ImageIcon i = new ImageProvider(info.getIcon()).setOptional(true).
                        setMaxHeight(MAX_ICON_SIZE).setMaxWidth(MAX_ICON_SIZE).get();
                if (i != null) {
                    putValue(Action.SMALL_ICON, i);
                }
            }
        } catch (Exception ex) {
            throw new RuntimeException(ex.getMessage(), ex);
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (!isEnabled()) return;
        try {
            Main.main.addLayer(ImageryLayer.create(info));
            AlignImageryPanel.addNagPanelIfNeeded();
        } catch (IllegalArgumentException ex) {
            if (ex.getMessage() == null || ex.getMessage().isEmpty()) {
                throw ex;
            } else {
                JOptionPane.showMessageDialog(Main.parent,
                        ex.getMessage(), tr("Error"),
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    
    protected boolean isLayerAlreadyPresent() {
        if (Main.isDisplayingMapView()) {
            for (ImageryLayer layer : Main.map.mapView.getLayersOfType(ImageryLayer.class)) {
                if (info.equals(layer.getInfo())) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    protected void updateEnabledState() {
        // never enable blacklisted entries. Do not add same imagery layer twice (fix #2519)
        if (info.isBlacklisted() || isLayerAlreadyPresent()) {
            setEnabled(false);
        } else if (info.getImageryType() == ImageryType.TMS || info.getImageryType() == ImageryType.BING || info.getImageryType() == ImageryType.SCANEX) {
            setEnabled(true);
        } else if (Main.isDisplayingMapView() && !Main.map.mapView.getAllLayers().isEmpty()) {
            setEnabled(true);
        } else {
            setEnabled(false);
        }
    }
}
