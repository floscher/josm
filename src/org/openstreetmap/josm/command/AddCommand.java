package org.openstreetmap.josm.command;

import java.awt.Component;
import java.util.Iterator;

import javax.swing.JLabel;

import org.openstreetmap.josm.data.osm.Key;
import org.openstreetmap.josm.data.osm.LineSegment;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Track;
import org.openstreetmap.josm.data.osm.visitor.SelectionComponentVisitor;
import org.openstreetmap.josm.data.osm.visitor.Visitor;

/**
 * A command that adds an osm primitive to a dataset. Keys cannot be added this
 * way. Use ChangeKeyValueCommand instead.
 * 
 * @author imi
 */
public class AddCommand implements Command, Visitor {

	/**
	 * The primitive to add to the dataset.
	 */
	private final OsmPrimitive osm;
	/**
	 * The dataset to add the primitive to.
	 */
	private final DataSet ds;

	/**
	 * Create the command and specify the element to add.
	 */
	public AddCommand(OsmPrimitive osm, DataSet dataSet) {
		this.osm = osm;
		this.ds = dataSet;
	}

	public void executeCommand() {
		osm.visit(this);
	}

	public Component commandDescription() {
		SelectionComponentVisitor v = new SelectionComponentVisitor();
		osm.visit(v);
		return new JLabel(v.name, v.icon, JLabel.LEADING);
	}

	/**
	 * Add the node to the nodes - list only.
	 * @param n The node to add.
	 */
	public void visit(Node n) {
		ds.nodes.add(n);
	}

	/**
	 * Add the line segment to the list of pending line segments.
	 * @param ls The line segment to add.
	 */
	public void visit(LineSegment ls) {
		ds.pendingLineSegments.add(ls);
	}

	/**
	 * Add the track to the dataset. Remove all line segments that were pending
	 * from the dataset.
	 */
	public void visit(Track t) {
		ds.addTrack(t);
		for (Iterator<LineSegment> it =  ds.pendingLineSegments.iterator(); it.hasNext();)
			if (t.segments().contains(it.next()))
				it.remove();
	}

	/**
	 * Add the key to the parent specified by the constructor
	 */
	public void visit(Key k) {
		throw new IllegalStateException("Keys are added by using ChangeKeyValueCommand");
	}
}
