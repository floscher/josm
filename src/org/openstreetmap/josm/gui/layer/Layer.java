package org.openstreetmap.josm.gui.layer;

import java.awt.Graphics;

import javax.swing.Icon;

import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.projection.Projection;
import org.openstreetmap.josm.gui.MapView;

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
	 * The name of this layer.
	 */
	public final String name;

	/**
	 * Create the layer and fill in the necessary components.
	 */
	public Layer(String name) {
		this.name = name;
	}

	/**
	 * Paint the dataset using the engine set.
	 * @param mv The object that can translate GeoPoints to screen coordinates.
	 */
	abstract public void paint(Graphics g, MapView mv);
	/**
	 * Return a representative small image for this layer. The image must not 
	 * be larger than 64 pixel in any dimension.
	 */
	abstract public Icon getIcon();

	/**
	 * @return A small tooltip hint about some statistics for this layer.
	 */
	abstract public String getToolTipText();

	/**
	 * Merges the given layer into this layer. Throws if the layer types are
	 * incompatible.
	 * @param from The layer that get merged into this one. After the merge,
	 * 		the other layer is not usable anymore and passing to one others
	 * 		mergeFrom should be one of the last things to do with a layer.
	 */
	abstract public void mergeFrom(Layer from);
	
	/**
	 * @param other The other layer that is tested to be mergable with this.
	 * @return Whether the other layer can be merged into this layer.
	 */
	abstract public boolean isMergable(Layer other);
	
	/**
	 * @return The bounding rectangle this layer occupies on screen when looking
	 * 		at lat/lon values or <code>null</code>, if infinite area or unknown
	 * 		area is occupied.
	 */
	abstract public Bounds getBoundsLatLon();
	
	/**
	 * @return The bounding rectangle this layer occupies on screen when looking
	 * 		at x/y values or <code>null</code>, if infinite area or unknown
	 * 		area is occupied.
	 */
	abstract public Bounds getBoundsXY();

	/**
	 * Initialize the internal dataset with the given projection.
	 */
	abstract public void init(Projection projection);
}
