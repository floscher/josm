package org.openstreetmap.josm.gui.layer;

import java.awt.Graphics;

import javax.swing.Icon;

import org.openstreetmap.josm.command.DataSet;
import org.openstreetmap.josm.data.osm.LineSegment;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.Track;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.gui.engine.Engine;

/**
 * A layer encapsulates the gui componente of one dataset and its representation.
 * 
 * Some layers may display data directly importet from OSM server. Other only 
 * display background images. Some can be edited, some not. Some are static and 
 * other changes dynamically (auto-updated).
 *
 * Layers can be visible or not. Most actions the user can do applies only on
 * selected layers. The available actions depend on the selected layers too.
 * 
 * All layers are managed by the MapView. They are displayed in a list to the 
 * right of the screen.
 * 
 * @author imi
 */
abstract public class Layer {

	/**
	 * The visibility state of the layer.
	 */
	public boolean visible = true;
	/**
	 * The dataSet this layer operates on, if any. Not all layer may have a
	 * dataset associated.
	 */
	public final DataSet dataSet;
	/**
	 * The name of this layer.
	 */
	public final String name;
	/**
	 * The engine used to draw the data.
	 */
	private final Engine engine;
	
	/**
	 * Create the layer and fill in the necessary components.
	 * @param dataSet The DataSet, this layer operates on. Can be <code>null</code>.
	 */
	public Layer(DataSet dataSet, Engine engine, String name) {
		if (engine == null || name == null)
			throw new NullPointerException();
		this.dataSet = dataSet;
		this.name = name;
		this.engine = engine;
	}

	/**
	 * Paint the dataset using the engine set.
	 * @param mv The object that can translate GeoPoints to screen coordinates.
	 */
	public final void paint(Graphics g, MapView mv) {
		engine.init(g, mv);

		for (Track t : dataSet.tracks())
			engine.drawTrack(t);
		for (LineSegment ls : dataSet.pendingLineSegments())
			engine.drawPendingLineSegment(ls);
		for (Node n : dataSet.nodes)
			engine.drawNode(n);
	}

	/**
	 * Return a representative small image for this layer. The image must not 
	 * be larger than 64 pixel in any dimension.
	 */
	abstract public Icon getIcon();

	/**
	 * @return <code>true</code>, if the map data can be edited.
	 */
	public boolean isEditable() {
		return false;
	}
}
