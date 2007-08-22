package org.openstreetmap.josm.command;

import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trn;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.swing.JLabel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.MutableTreeNode;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.command.MoveCommand.OldState;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.visitor.AllNodesVisitor;
import org.openstreetmap.josm.tools.ImageProvider;

/**
 * RotateCommand rotates a number of objects around their centre.
 * 
 * @author Frederik Ramm <frederik@remote.org>
 */
public class RotateCommand extends Command {
	
	/**
	 * The objects to rotate.
	 */
	public Collection<Node> objects = new LinkedList<Node>();
	
	/**
	 * pivot point
	 */
	private Node pivot;
	
	/**
	 * angle of rotation starting click to pivot
	 */
	private double startAngle;
	
	/**
	 * computed rotation angle between starting click and current mouse pos
	 */
	private double rotationAngle;
	
	/**
	 * Small helper for holding the interesting part of the old data state of the
	 * objects. 
	 */
	class OldState
	{
		double x,y,lat,lon;
		boolean modified;
		Node originalNode;
	}
	
	/**
	 * List of all old states of the objects.
	 */
	private Map<Node, OldState> oldState = new HashMap<Node, OldState>();
	
	/**
	 * Creates a RotateCommand.
	 * Assign the initial object set, compute pivot point and rotation angle.
	 * Computation of pivot point is done by the same rules that are used in 
	 * the "align nodes in circle" action.
	 */
	public RotateCommand(Collection<OsmPrimitive> objects, EastNorth start, EastNorth end) {

		this.objects = AllNodesVisitor.getAllNodes(objects);
		pivot = new Node(new LatLon(0,0));
			
		for (Node n : this.objects) {
			OldState os = new OldState();
			os.x = n.eastNorth.east();
			os.y = n.eastNorth.north();
			os.lat = n.coor.lat();
			os.lon = n.coor.lon();
			os.modified = n.modified;
			oldState.put(n, os);
			pivot.eastNorth = new EastNorth(pivot.eastNorth.east()+os.x, pivot.eastNorth.north()+os.y);
			pivot.coor = Main.proj.eastNorth2latlon(pivot.eastNorth);
		}
		pivot.eastNorth = new EastNorth(pivot.eastNorth.east()/this.objects.size(), pivot.eastNorth.north()/this.objects.size());
		pivot.coor = Main.proj.eastNorth2latlon(pivot.eastNorth);	

		rotationAngle = Math.PI/2;
		rotateAgain(start, end);
	}

	/**
	 * Rotate the same set of objects again, by the angle between given 
	 * start and end nodes. Internally this is added to the existing
	 * rotation so a later undo will undo the whole rotation.
	 */
	public void rotateAgain(EastNorth start, EastNorth end) {
		// compute angle
		startAngle = Math.atan2(start.east()-pivot.eastNorth.east(), start.north()-pivot.eastNorth.north());
		double endAngle = Math.atan2(end.east()-pivot.eastNorth.east(), end.north()-pivot.eastNorth.north());
		rotationAngle += startAngle - endAngle;		
		rotateNodes(false);
	}
	
	/**
	 * Helper for actually rotationg the nodes.
	 * @param setModified - true if rotated nodes should be flagged "modified"
	 */
	private void rotateNodes(boolean setModified) {
		for (Node n : objects) {
			double cosPhi = Math.cos(rotationAngle);
			double sinPhi = Math.sin(rotationAngle);
			double x = oldState.get(n).x - pivot.eastNorth.east();
			double y = oldState.get(n).y - pivot.eastNorth.north();
			double nx =  sinPhi * x + cosPhi * y + pivot.eastNorth.east();
			double ny = -cosPhi * x + sinPhi * y + pivot.eastNorth.north();
			n.eastNorth = new EastNorth(nx, ny);
			n.coor = Main.proj.eastNorth2latlon(n.eastNorth);
			if (setModified) n.modified = true;
		}
	}
	
	@Override public void executeCommand() {
		rotateNodes(true);
	}

	@Override public void undoCommand() {
		for (Node n : objects) {
			OldState os = oldState.get(n);
			n.eastNorth = new EastNorth(os.x, os.y);
			n.coor = new LatLon(os.lat, os.lon);
			n.modified = os.modified;
		}
	}

	@Override public void fillModifiedData(Collection<OsmPrimitive> modified, Collection<OsmPrimitive> deleted, Collection<OsmPrimitive> added) {
		for (OsmPrimitive osm : objects)
			modified.add(osm);
	}

	@Override public MutableTreeNode description() {
		return new DefaultMutableTreeNode(new JLabel(tr("Rotate")+" "+objects.size()+" "+trn("node","nodes",objects.size()), ImageProvider.get("data", "node"), JLabel.HORIZONTAL));
    }
}
