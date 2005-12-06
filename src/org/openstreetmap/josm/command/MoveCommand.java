package org.openstreetmap.josm.command;

import java.awt.geom.Point2D;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.visitor.AllNodesVisitor;

/**
 * MoveCommand moves a set of OsmPrimitives along the map. It can be moved again
 * to collect several MoveCommands into one command.
 * 
 * @author imi
 */
public class MoveCommand implements Command {

	/**
	 * The objects that should be moved.
	 */
	public List<Node> objects = new LinkedList<Node>();
	/**
	 * x difference movement. Coordinates are in northern/eastern 
	 */
	private double x;
	/**
	 * y difference movement. Coordinates are in northern/eastern 
	 */
	private double y;

	/**
	 * x/y List of all old positions of the objects.
	 */
	private List<Point2D.Double> oldPositions = new LinkedList<Point2D.Double>();

	/**
	 * Create a MoveCommand and assign the initial object set and movement vector.
	 */
	public MoveCommand(Collection<OsmPrimitive> objects, double x, double y) {
		this.x = x;
		this.y = y;
		this.objects = getAffectedNodes(objects);
		for (Node n : this.objects)
			oldPositions.add(new Point2D.Double(n.coor.x, n.coor.y));
	}

	/**
	 * @return a list of all nodes that will be moved if using the given set of
	 * objects.
	 */
	public static List<Node> getAffectedNodes(Collection<OsmPrimitive> objects) {
		AllNodesVisitor visitor = new AllNodesVisitor();
		for (OsmPrimitive osm : objects)
			osm.visit(visitor);
		return new LinkedList<Node>(visitor.nodes);
	}
	
	/**
	 * Move the same set of objects again by the specified vector. The vectors
	 * are added together and so the resulting will be moved to the previous
	 * vector plus this one.
	 * 
	 * The move is immediatly executed and any undo will undo both vectors to
	 * the original position the objects had before first moving.
	 */
	public void moveAgain(double x, double y) {
		for (Node n : objects) {
			n.coor.x += x;
			n.coor.y += y;
		}
		this.x += x;
		this.y += y;
	}
	
	public void executeCommand() {
		for (Node n : objects) {
			n.coor.x += x;
			n.coor.y += y;
		}
	}

	public void undoCommand() {
		Iterator<Point2D.Double> it = oldPositions.iterator();
		for (Node n : objects) {
			Point2D.Double p = it.next();
			n.coor.x = p.x;
			n.coor.y = p.y;
		}
	}

	public void fillModifiedData(Collection<OsmPrimitive> modified, Collection<OsmPrimitive> deleted, Collection<OsmPrimitive> added) {
		for (OsmPrimitive osm : objects)
			modified.add(osm);
	}
}
