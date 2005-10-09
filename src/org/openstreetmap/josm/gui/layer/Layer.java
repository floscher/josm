package org.openstreetmap.josm.gui.layer;

import java.awt.Graphics;

import javax.swing.Icon;

import org.openstreetmap.josm.data.osm.DataSet;
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
public interface Layer {

	/**
	 * Draw the layer on the given Graphics. The state of the graphics object
	 * can be changed (drawing color..)
	 *
	 * @param g The graphics to draw the layer on.
	 * @param mv The MapView with information about the screen layout. 
	 */
	void paint(Graphics g, MapView mv);

	/**
	 * Return a representative small image for this layer. The image must not 
	 * be larger than 64 pixel in any dimension.
	 */
	Icon getIcon();

	/**
	 * Provide a human readable name (may be in html format).
	 */
	String getName();
	
	/**
	 * If the layer has a dataset it can provide, return it here.
	 * @return The dataset for this layer or <code>null</code> if no dataset
	 * 		is available.
	 */
	DataSet getDataSet();
	
	/**
	 * @return <code>true</code>, if the map data can be edited.
	 */
	boolean isEditable();

	/**
	 * @return <code>true</code>, if the layer is visible
	 */
	boolean isVisible();
	
	/**
	 * Set the visibility state of the layer.
	 */
	void setVisible(boolean visible);
}
