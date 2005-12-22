package org.openstreetmap.josm.command;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.openstreetmap.josm.Main;
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
	 * Small helper for holding the interesting part of the old data state of the
	 * objects. 
	 * @author imi
	 */
	class OldState
	{
		double x,y,lat,lon;
		boolean modified;
	}
	/**
	 * List of all old states of the objects.
	 */
	private List<OldState> oldState = new LinkedList<OldState>();

	/**
	 * Create a MoveCommand and assign the initial object set and movement vector.
	 */
	public MoveCommand(Collection<OsmPrimitive> objects, double x, double y) {
		this.x = x;
		this.y = y;
		this.objects = getAffectedNodes(objects);
		for (Node n : this.objects) {
			OldState os = new OldState();
			os.x = n.coor.x;
			os.y = n.coor.y;
			os.lat = n.coor.lat;
			os.lon = n.coor.lon;
			os.modified = n.modified;
			oldState.add(os);
		}
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
			Main.pref.getProjection().xy2latlon(n.coor);
		}
		this.x += x;
		this.y += y;
	}
	
	public void executeCommand() {
		for (Node n : objects) {
			n.coor.x += x;
			n.coor.y += y;
			Main.pref.getProjection().xy2latlon(n.coor);
			n.modified = true;
		}
	}

	public void undoCommand() {
		Iterator<OldState> it = oldState.iterator();
		for (Node n : objects) {
			OldState os = it.next();
			n.coor.x = os.x;
			n.coor.y = os.y;
			n.coor.lat = os.lat;
			n.coor.lon = os.lon;
			n.modified = os.modified;
		}
	}

	public void fillModifiedData(Collection<OsmPrimitive> modified, Collection<OsmPrimitive> deleted, Collection<OsmPrimitive> added) {
		for (OsmPrimitive osm : objects)
			modified.add(osm);
	}
}
