package org.openstreetmap.josm.data.osm;

import java.util.Collection;
import java.util.List;

import org.openstreetmap.josm.data.Bounds;

/**
 * DataSet is the data behind one window in the application. It can consist of only a few
 * points up to the whole osm database. DataSet's can be merged together, split up into
 * several different ones, saved, (up/down/disk)loaded etc.
 *
 * Note, that DataSet is not an osm-primitive, so it has no key association but a few
 * members to store some information.
 * 
 * @author imi
 */
public class DataSet implements Cloneable {

	/**
	 * All nodes goes here, even when included in other data (tracks etc) listed.
	 * This enables the instant conversion of the whole DataSet by iterating over
	 * this data structure.
	 */
	public List<Node> allNodes;

	/**
	 * All tracks (Streets etc.) in the DataSet. 
	 * 
	 * The nodes of the track segments of this track must be objects from 
	 * the nodes list, however the track segments are stored only in the 
	 * track list.
	 */
	public List<Track> tracks;

	/**
	 * Return the bounds of this DataSet, depending on X/Y values.
	 * The min of the return value is the upper left GeoPoint, the max the lower
	 * down GeoPoint, regarding to the X/Y values.
	 * 
	 * Return null, if any point not converted yet or if there are no points at all.
	 * 
	 * @return Bounding coordinate structure.
	 */
	public Bounds getBoundsXY() {
		if (allNodes.size() == 0)
			return null;

		Bounds b = new Bounds(allNodes.get(0).coor.clone(), allNodes.get(0).coor.clone());
		for (Node w : allNodes)
		{
			if (Double.isNaN(w.coor.x) || Double.isNaN(w.coor.y))
				return null;
			if (w.coor.x < b.min.x)
				b.min.x = w.coor.x;
			if (w.coor.y < b.min.y)
				b.min.y = w.coor.y;
			if (w.coor.x > b.max.x)
				b.max.x = w.coor.x;
			if (w.coor.y > b.max.y)
				b.max.y = w.coor.y;
		}
		return b;
	}

	/**
	 * Return the bounds of this DataSet, depending on lat/lon values.
	 * The min of the return value is the upper left GeoPoint, the max the lower
	 * down GeoPoint.
	 * 
	 * Return null, if any point does not have lat/lon or if there are no 
	 * points at all.
	 * 
	 * @return Bounding coordinate structure.
	 */
	public Bounds getBoundsLatLon() {
		if (allNodes.size() == 0)
			return null;

		Bounds b = new Bounds(allNodes.get(0).coor.clone(), allNodes.get(0).coor.clone());
		for (Node w : allNodes)
		{
			if (Double.isNaN(w.coor.lat) || Double.isNaN(w.coor.lon))
				return null;
			if (w.coor.lat < b.min.lat)
				b.min.lat = w.coor.lat;
			if (w.coor.lon < b.min.lon)
				b.min.lon = w.coor.lon;
			if (w.coor.lat > b.max.lat)
				b.max.lat = w.coor.lat;
			if (w.coor.lon > b.max.lon)
				b.max.lon = w.coor.lon;
		}
		return b;
	}

	/**
	 * Remove the selection of the whole dataset.
	 */
	public void clearSelection() {
		clearSelection(allNodes);
		clearSelection(tracks);
		for (Track t : tracks)
			clearSelection(t.segments);
	}
	
	/**
	 * Remove the selection from every value in the collection.
	 * @param list The collection to remove the selection from.
	 */
	private void clearSelection(Collection<? extends OsmPrimitive> list) {
		if (list == null)
			return;
		for (OsmPrimitive osm : list) {
			osm.selected = false;
			if (osm.keys != null)
				clearSelection(osm.keys.keySet());
		}
	}
	
	public DataSet clone() {
		try {return (DataSet)super.clone();} catch (CloneNotSupportedException e) {}
		return null;
	}
}
