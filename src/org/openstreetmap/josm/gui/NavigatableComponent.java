package org.openstreetmap.josm.gui;

import java.awt.Point;
import java.util.Collection;
import java.util.HashSet;

import javax.swing.JComponent;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.GeoPoint;
import org.openstreetmap.josm.data.osm.LineSegment;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.projection.Projection;

/**
 * An component that can be navigated by a mapmover. Used as map view and for the
 * zoomer in the download dialog.
 * 
 * @author imi
 */
public class NavigatableComponent extends JComponent {

	/**
	 * The scale factor in x or y-units per pixel. This means, if scale = 10,
	 * every physical pixel on screen are 10 x or 10 y units in the 
	 * northing/easting space of the projection.
	 */
	protected double scale;
	/**
	 * Center n/e coordinate of the desired screen center.
	 */
	protected GeoPoint center;

	/**
	 * Return the current scale value.
	 * @return The scale value currently used in display
	 */
	public double getScale() {
		return scale;
	}

	/**
	 * @return Returns the center point. A copy is returned, so users cannot
	 * 		change the center by accessing the return value. Use zoomTo instead.
	 */
	public GeoPoint getCenter() {
		return center.clone();
	}

	/**
	 * Get geographic coordinates from a specific pixel coordination
	 * on the screen.
	 * 
	 * If you don't need it, provide false at third parameter to speed
	 * up the calculation.
	 *  
	 * @param x X-Pixelposition to get coordinate from
	 * @param y Y-Pixelposition to get coordinate from
	 * @param latlon If set, the return value will also have the 
	 * 				 latitude/longitude filled.
	 * 
	 * @return The geographic coordinate, filled with x/y (northing/easting)
	 * 		settings and, if requested with latitude/longitude.
	 */
	public GeoPoint getPoint(int x, int y, boolean latlon) {
		GeoPoint p = new GeoPoint();
		p.x = center.x + (x - getWidth()/2.0)*scale;
		p.y = center.y - (y - getHeight()/2.0)*scale;
		if (latlon)
			getProjection().xy2latlon(p);
		return p;
	}

	/**
	 * Return the point on the screen where this GeoPoint would be.
	 * @param point The point, where this geopoint would be drawn.
	 * @return The point on screen where "point" would be drawn, relative
	 * 		to the own top/left.
	 */
	public Point getScreenPoint(GeoPoint point) {
		GeoPoint p;
		if (!Double.isNaN(point.x) && !Double.isNaN(point.y))
			p = point;
		else {
			if (Double.isNaN(point.lat) || Double.isNaN(point.lon))
				throw new IllegalArgumentException("point: Either lat/lon or x/y must be set.");
			p = point.clone();
			getProjection().latlon2xy(p);
		}
		double x = (p.x-center.x)/scale + getWidth()/2;
		double y = (center.y-p.y)/scale + getHeight()/2;
		return new Point((int)x,(int)y);
	}

	/**
	 * Zoom to the given coordinate.
	 * @param centerX The center x-value (easting) to zoom to.
	 * @param centerY The center y-value (northing) to zoom to.
	 * @param scale The scale to use.
	 */
	public void zoomTo(GeoPoint newCenter, double scale) {
		center = newCenter.clone();
		getProjection().xy2latlon(center);
		this.scale = scale;
		repaint();
	}

	/**
	 * Return the object, that is nearest to the given screen point.
	 * 
	 * First, a node will be searched. If a node within 10 pixel is found, the
	 * nearest node is returned.
	 * 
	 * If no node is found, search for pending line segments.
	 * 
	 * If no such line segment is found, and a non-pending line segment is 
	 * within 10 pixel to p, this segment is returned, except when 
	 * <code>wholeWay</code> is <code>true</code>, in which case the 
	 * corresponding Way is returned.
	 * 
	 * If no line segment is found and the point is within an area, return that
	 * area.
	 * 
	 * If no area is found, return <code>null</code>.
	 * 
	 * @param p				 The point on screen.
	 * @param lsInsteadWay Whether the line segment (true) or only the whole
	 * 					 	 way should be returned.
	 * @return	The primitive, that is nearest to the point p.
	 */
	public OsmPrimitive getNearest(Point p, boolean lsInsteadWay) {
		double minDistanceSq = Double.MAX_VALUE;
		OsmPrimitive minPrimitive = null;
	
		// nodes
		for (Node n : Main.main.ds.nodes) {
			if (n.isDeleted())
				continue;
			Point sp = getScreenPoint(n.coor);
			double dist = p.distanceSq(sp);
			if (minDistanceSq > dist && dist < 100) {
				minDistanceSq = p.distanceSq(sp);
				minPrimitive = n;
			}
		}
		if (minPrimitive != null)
			return minPrimitive;
		
		// for whole waies, try the waies first
		minDistanceSq = Double.MAX_VALUE;
		if (!lsInsteadWay) {
			for (Way t : Main.main.ds.waies) {
				if (t.isDeleted())
					continue;
				for (LineSegment ls : t.segments) {
					if (ls.isDeleted())
						continue;
					Point A = getScreenPoint(ls.start.coor);
					Point B = getScreenPoint(ls.end.coor);
					double c = A.distanceSq(B);
					double a = p.distanceSq(B);
					double b = p.distanceSq(A);
					double perDist = a-(a-b+c)*(a-b+c)/4/c; // perpendicular distance squared
					if (perDist < 100 && minDistanceSq > perDist && a < c+100 && b < c+100) {
						minDistanceSq = perDist;
						minPrimitive = t;
					}
				}			
			}
			if (minPrimitive != null)
				return minPrimitive;
		}
		
		minDistanceSq = Double.MAX_VALUE;
		// line segments
		for (LineSegment ls : Main.main.ds.lineSegments) {
			if (ls.isDeleted())
				continue;
			Point A = getScreenPoint(ls.start.coor);
			Point B = getScreenPoint(ls.end.coor);
			double c = A.distanceSq(B);
			double a = p.distanceSq(B);
			double b = p.distanceSq(A);
			double perDist = a-(a-b+c)*(a-b+c)/4/c; // perpendicular distance squared
			if (perDist < 100 && minDistanceSq > perDist && a < c+100 && b < c+100) {
				minDistanceSq = perDist;
				minPrimitive = ls;
			}
		}
	
		return minPrimitive;
	}

	/**
	 * @return A list of all objects that are nearest to 
	 * the mouse. To do this, first the nearest object is 
	 * determined.
	 * 
	 * If its a node, return all line segments and
	 * streets the node is part of, as well as all nodes
	 * (with their line segments and waies) with the same
	 * location.
	 * 
	 * If its a line segment, return all waies this segment 
	 * belongs to as well as all line segments that are between
	 * the same nodes (in both direction) with all their waies.
	 * 
	 * @return A collection of all items or <code>null</code>
	 * 		if no item under or near the point. The returned
	 * 		list is never empty.
	 */
	public Collection<OsmPrimitive> getAllNearest(Point p) {
		OsmPrimitive osm = getNearest(p, true);
		if (osm == null)
			return null;
		Collection<OsmPrimitive> c = new HashSet<OsmPrimitive>();
		c.add(osm);
		if (osm instanceof Node) {
			Node node = (Node)osm;
			for (Node n : Main.main.ds.nodes)
				if (!n.isDeleted() && n.coor.equalsLatLon(node.coor))
					c.add(n);
			for (LineSegment ls : Main.main.ds.lineSegments)
				// line segments never match nodes, so they are skipped by contains
				if (!ls.isDeleted() && (c.contains(ls.start) || c.contains(ls.end)))
					c.add(ls);
		} 
		if (osm instanceof LineSegment) {
			LineSegment line = (LineSegment)osm;
			for (LineSegment ls : Main.main.ds.lineSegments)
				if (!ls.isDeleted() && ls.equalPlace(line))
					c.add(ls);
		}
		if (osm instanceof Node || osm instanceof LineSegment) {
			for (Way t : Main.main.ds.waies) {
				if (t.isDeleted())
					continue;
				for (LineSegment ls : t.segments) {
					if (!ls.isDeleted() && c.contains(ls)) {
						c.add(t);
						break;
					}
				}
			}
		}
		return c;
	}
	
	/**
	 * @return The projection to be used in calculating stuff.
	 */
	protected Projection getProjection() {
		return Main.pref.getProjection();
	}
}
