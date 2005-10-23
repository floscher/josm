package org.openstreetmap.josm.gui.layer;

import java.awt.Graphics;

import javax.swing.Icon;

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
	 * @return <code>true</code>, if the map data can be edited.
	 */
	public boolean isEditable() {
		return false;
	}
}
