package org.openstreetmap.josm.gui.layer;

import java.awt.Graphics;
import java.util.Collection;

import javax.swing.Icon;

import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.visitor.AllNodesVisitor;
import org.openstreetmap.josm.data.osm.visitor.BoundingVisitor;
import org.openstreetmap.josm.data.osm.visitor.SimplePaintVisitor;
import org.openstreetmap.josm.data.projection.Projection;
import org.openstreetmap.josm.gui.ImageProvider;
import org.openstreetmap.josm.gui.MapView;

/**
 * A layer holding data imported from the osm server.
 * 
 * The data can be fully edited.
 * 
 * @author imi
 */
public class OsmDataLayer extends Layer {

	private static Icon icon;

	/**
	 * The data behind this layer. A list of primitives which are also in Main.main.ds.
	 */
	private final Collection<OsmPrimitive> data;

	/**
	 * Construct a OsmDataLayer.
	 */
	public OsmDataLayer(Collection<OsmPrimitive> data, String name) {
		super(name);
		this.data = data;
	}

	/**
	 * TODO: @return Return a dynamic drawn icon of the map data. The icon is
	 * 		updated by a background thread to not disturb the running programm.
	 */
	@Override
	public Icon getIcon() {
		if (icon == null)
			icon = ImageProvider.get("layer", "osmdata");
		return icon;
	}

	/**
	 * Draw all primitives in this layer but do not draw modified ones (they
	 * are drawn by the edit layer).
	 * Draw nodes last to overlap the line segments they belong to.
	 */
	@Override
	public void paint(Graphics g, MapView mv) {
		SimplePaintVisitor visitor = new SimplePaintVisitor(g, mv, null);
		// first draw the tracks (and line segments)
		for (OsmPrimitive osm : data)
			if (!osm.modified && !(osm instanceof Node))
				osm.visit(visitor);
		for (OsmPrimitive osm : data)
			if (!osm.modified && osm instanceof Node)
				osm.visit(visitor);
	}

	@Override
	public String getToolTipText() {
		return data.size()+" primitives.";
	}

	@Override
	public void mergeFrom(Layer from) {
		OsmDataLayer layer = (OsmDataLayer)from;
		data.addAll(layer.data);
	}

	@Override
	public boolean isMergable(Layer other) {
		return other instanceof OsmDataLayer;
	}

	@Override
	public Bounds getBoundsLatLon() {
		BoundingVisitor b = new BoundingVisitor(BoundingVisitor.Type.LATLON);
		for (OsmPrimitive osm : data)
			osm.visit(b);
		return b.bounds;
	}

	@Override
	public Bounds getBoundsXY() {
		BoundingVisitor b = new BoundingVisitor(BoundingVisitor.Type.XY);
		for (OsmPrimitive osm : data)
			osm.visit(b);
		return b.bounds;
	}

	@Override
	public void init(Projection projection) {
		for (OsmPrimitive osm : data)
			for (Node n : AllNodesVisitor.getAllNodes(osm))
				projection.latlon2xy(n.coor);
	}
}
