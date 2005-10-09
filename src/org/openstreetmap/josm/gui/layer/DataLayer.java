package org.openstreetmap.josm.gui.layer;

import java.awt.Graphics;

import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.LineSegment;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.Track;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.gui.engine.Engine;

/**
 * Base class for all layer that depends on some DataSet.
 * The dataset is final and so can not be changed later.
 * 
 * @author imi
 */
public abstract class DataLayer implements Layer {

	/**
	 * The dataSet this layer operates on.
	 */
	protected final DataSet dataSet;
	/**
	 * The engine used to draw the data.
	 */
	protected final Engine engine;
	/**
	 * The name of this layer.
	 */
	private final String name;
	/**
	 * The visibility state of the layer.
	 */
	private boolean visible = true;

	/**
	 * Construct the datalayer and fill the dataset.
	 * @param name The name of this layer. Returned by getName.
	 */
	public DataLayer(DataSet dataSet, Engine engine, String name) {
		if (dataSet == null || engine == null || name == null)
			throw new NullPointerException();
		this.name = name;
		this.dataSet = dataSet;
		this.engine = engine;
	}

	/**
	 * Paint the dataset using the engine set.
	 * @param mv The object that can translate GeoPoints to screen coordinates.
	 */
	public void paint(Graphics g, MapView mv) {
		engine.init(g, mv);

		for (Track t : dataSet.tracks())
			engine.drawTrack(t);
		for (LineSegment ls : dataSet.pendingLineSegments())
			engine.drawPendingLineSegment(ls);
		for (Node n : dataSet.nodes)
			engine.drawNode(n);
	}

	/**
	 * Return the data set behind this data layer.
	 */
	public DataSet getDataSet() {
		return dataSet;
	}

	public String getName() {
		return name;
	}

	public boolean isVisible() {
		return visible;
	}

	public void setVisible(boolean visible) {
		this.visible = visible;
	}
}
