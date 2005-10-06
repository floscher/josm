package org.openstreetmap.josm.gui.engine;

import java.awt.Graphics;

import org.openstreetmap.josm.data.GeoPoint;
import org.openstreetmap.josm.data.osm.LineSegment;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.Track;
import org.openstreetmap.josm.gui.Layer;

/**
 * Subclasses of Engine are able to draw map data on the screen. The layout and
 * colors depend only on the engine used, but it may have an configuration panel.
 *
 * @author imi
 */
abstract public class Engine {

	/**
	 * The Graphics surface to draw on. This should be set before each painting
	 * sequence.
	 */
	protected Graphics g;
	/**
	 * The layer, this engine was created for.
	 */
	protected final Layer layer;

	
	/**
	 * Creates an Engine from an Layer it belongs to.
	 * @param layer The mapview this engine belongs to.
	 */
	public Engine(Layer layer) {
		this.layer = layer;
	}
	
	/**
	 * Called to initialize the Engine for a new drawing sequence.
	 * @param g
	 */
	public void init(Graphics g) {
		this.g = g;
	}

	/**
	 * Draw the background. The coordinates are given as hint (e.g. to draw
	 * an background image).
	 */
	abstract public void drawBackground(GeoPoint ul, GeoPoint lr);

	/**
	 * Draw the node.
	 */
	abstract public void drawNode(Node n);
	
	/**
	 * Draw the track.
	 */
	abstract public void drawTrack(Track t);
	
	/**
	 * Draw the pending line segment. Non-pending line segments must be drawn
	 * within drawTrack.
	 */
	abstract public void drawPendingLineSegment(LineSegment ls);
}
