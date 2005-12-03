package org.openstreetmap.josm.command;

import java.awt.Component;
import java.awt.geom.Point2D;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.swing.JLabel;

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
	private List<OsmPrimitive> objects;
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
	private List<Point2D.Double> oldPositions;
	
	/**
	 * Create a MoveCommand and assign the initial object set and movement vector.
	 */
	public MoveCommand(Collection<OsmPrimitive> objects, double x, double y) {
		this.objects = new LinkedList<OsmPrimitive>(objects);
		this.x = x;
		this.y = y;
	}

	public void executeCommand() {
		AllNodesVisitor visitor = new AllNodesVisitor();
		for (OsmPrimitive osm : objects)
			osm.visit(visitor);
		for (Node n : visitor.nodes) {
			n.coor.x += x;
			n.coor.y += y;
		}
	}

	public void undoCommand() {
		AllNodesVisitor visitor = new AllNodesVisitor();
		for (OsmPrimitive osm : objects)
			osm.visit(visitor);
		Iterator<Point2D.Double> it = oldPositions.iterator();
		for (Node n : visitor.nodes) {
			Point2D.Double p = it.next();
			n.coor.x = p.x;
			n.coor.y = p.y;
		}
	}

	public Component commandDescription() {
		String xstr = Math.abs(x) + (x < 0 ? "W" : "E");
		String ystr = Math.abs(y) + (y < 0 ? "S" : "N");
		return new JLabel("Move "+objects.size()+" primitives "+xstr+" "+ystr);
	}

	public void fillModifiedData(Collection<OsmPrimitive> modified, Collection<OsmPrimitive> deleted, Collection<OsmPrimitive> added) {
		for (OsmPrimitive osm : objects)
			if (!modified.contains(osm))
				modified.add(osm);
	}
}
