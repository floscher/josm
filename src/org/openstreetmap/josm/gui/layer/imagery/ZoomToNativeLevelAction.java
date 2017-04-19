// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.layer.imagery;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.gui.layer.AbstractTileSourceLayer;

/**
 * Change resolution to native zoom level.
 * @since 11950 (extracted from {@link AbstractTileSourceLayer})
 */
public class ZoomToNativeLevelAction extends AbstractAction {

    private final AbstractTileSourceLayer<?> layer;

    /**
     * Constructs a new {@code ZoomToNativeLevelAction}.
     * @param layer imagery layer
     */
    public ZoomToNativeLevelAction(AbstractTileSourceLayer<?> layer) {
        super(tr("Zoom to native resolution"));
        this.layer = layer;
    }

    @Override
    public void actionPerformed(ActionEvent ae) {
        double newFactor = Math.sqrt(layer.getScaleFactor(layer.currentZoomLevel));
        Main.map.mapView.zoomToFactor(newFactor);
        layer.invalidate();
    }
}
