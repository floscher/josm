package org.openstreetmap.josm.gui.layer;

import java.awt.Component;
import java.awt.Graphics;
import java.io.File;

import javax.swing.Icon;

import org.openstreetmap.josm.data.osm.visitor.BoundingXYVisitor;
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
	public String name;
	/**
	 * If a file is associated with this layer, this variable should be set to it.
	 */
	public File associatedFile;

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
	 * 		at x/y values or <code>null</code>, if infinite area or unknown
	 * 		area is occupied.
	 */
	abstract public void visitBoundingBox(BoundingXYVisitor v);

	abstract public Object getInfoComponent();
	
	abstract public Component[] getMenuEntries();
	
	/**
	 * Called, when the layer is removed from the mapview and is going to be
	 * destroyed.
	 * 
	 * This is because the Layer constructor can not add itself safely as listener
	 * to the layerlist dialog, because there may be no such dialog yet (loaded
	 * via command line parameter).
	 */
	public void destroy() {}
}
