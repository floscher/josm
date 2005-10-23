package org.openstreetmap.josm.command;

import java.awt.Component;
import java.util.Collection;
import java.util.HashSet;

import javax.swing.JLabel;

import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;

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
	private Collection<OsmPrimitive> objects;
	/**
	 * x difference movement. Coordinates are in northern/eastern 
	 */
	private double x;
	/**
	 * y difference movement. Coordinates are in northern/eastern 
	 */
	private double y;

	/**
	 * Create a MoveCommand and assign the initial object set and movement vector.
	 */
	public MoveCommand(Collection<OsmPrimitive> objects, double x, double y) {
		this.objects = objects;
		this.x = x;
		this.y = y;
	}

	/**
	 * Move the objects additional to the current movement.
	 */
	public void move(double x, double y) {
		this.x += x;
		this.y += y;
	}

	public void executeCommand() {
		Collection<Node> movingNodes = new HashSet<Node>();
		for (OsmPrimitive osm : objects)
			movingNodes.addAll(osm.getAllNodes());
		for (Node n : movingNodes) {
			n.coor.x += x;
			n.coor.y += y;
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
