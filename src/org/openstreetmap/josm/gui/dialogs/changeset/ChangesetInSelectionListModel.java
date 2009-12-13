// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.changeset;

import java.util.Collection;

import javax.swing.DefaultListSelectionModel;

import org.openstreetmap.josm.data.SelectionChangedListener;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;

public class ChangesetInSelectionListModel extends ChangesetListModel implements SelectionChangedListener, MapView.LayerChangeListener{

    public ChangesetInSelectionListModel(DefaultListSelectionModel selectionModel) {
        super(selectionModel);
    }
    /* ---------------------------------------------------------------------------- */
    /* Interface SelectionChangeListener                                            */
    /* ---------------------------------------------------------------------------- */
    public void selectionChanged(Collection<? extends OsmPrimitive> newSelection) {
        initFromPrimitives(newSelection);
    }

    /* ---------------------------------------------------------------------------- */
    /* Interface LayerChangeListener                                                */
    /* ---------------------------------------------------------------------------- */
    public void activeLayerChange(Layer oldLayer, Layer newLayer) {
        if (newLayer == null || ! (newLayer instanceof OsmDataLayer)) {
            setChangesets(null);
        } else {
            initFromPrimitives(((OsmDataLayer) newLayer).data.getSelected());
        }
    }
    public void layerAdded(Layer newLayer) {}
    public void layerRemoved(Layer oldLayer) {}
}
