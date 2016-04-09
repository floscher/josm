// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.io;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.List;

import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.PrimitiveId;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.io.MultiFetchServerObjectReader;

/**
 * Task downloading a set of OSM primitives.
 * @since 4081
 */
public class DownloadPrimitivesTask extends AbstractPrimitiveTask {

    private final List<PrimitiveId> ids;

    /**
     * Constructs a new {@code DownloadPrimitivesTask}.
     *
     * @param layer the layer in which primitives are updated. Must not be null.
     * @param ids a collection of primitives to update from the server. Set to
     * the empty collection if null.
     * @param fullRelation true if a full download is required, i.e.,
     * a download including the immediate children of a relation.
     * @throws IllegalArgumentException if layer is null.
     */
    public DownloadPrimitivesTask(OsmDataLayer layer, List<PrimitiveId> ids, boolean fullRelation) {
        this(layer, ids, fullRelation, null);
    }

    /**
     * Constructs a new {@code DownloadPrimitivesTask}.
     *
     * @param layer the layer in which primitives are updated. Must not be null.
     * @param ids a collection of primitives to update from the server. Set to
     *     the empty collection if null.
     * @param fullRelation true if a full download is required, i.e.,
     *     a download including the immediate children of a relation.
     * @param progressMonitor ProgressMonitor to use or null to create a new one.
     * @throws IllegalArgumentException if layer is null.
     */
    public DownloadPrimitivesTask(OsmDataLayer layer, List<PrimitiveId> ids, boolean fullRelation,
            ProgressMonitor progressMonitor) {
        super(tr("Download objects"), progressMonitor, layer);
        this.ids = ids;
        setZoom(true);
        setDownloadRelations(true, fullRelation);
    }

    @Override
    protected void initMultiFetchReader(MultiFetchServerObjectReader reader) {
        getProgressMonitor().indeterminateSubTask(tr("Initializing nodes to download ..."));
        for (PrimitiveId id : ids) {
            OsmPrimitive osm = layer.data.getPrimitiveById(id);
            if (osm == null) {
                switch (id.getType()) {
                    case NODE:
                        osm = new Node(id.getUniqueId());
                        break;
                    case WAY:
                        osm = new Way(id.getUniqueId());
                        break;
                    case RELATION:
                        osm = new Relation(id.getUniqueId());
                        break;
                    default: throw new AssertionError();
                }
            }
            reader.append(osm);
        }
    }
}
