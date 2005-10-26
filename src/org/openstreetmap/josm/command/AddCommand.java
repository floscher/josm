package org.openstreetmap.josm.command;

import java.awt.Component;
import java.util.Collection;
import java.util.Iterator;

import javax.swing.JLabel;

import org.openstreetmap.josm.Main;
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
	 * Create the command and specify the element to add.
	 */
	public AddCommand(OsmPrimitive osm) {
		this.osm = osm;
	}

	public void executeCommand() {
		osm.visit(this);
	}
	
	public Component commandDescription() {
		SelectionComponentVisitor v = new SelectionComponentVisitor();
		osm.visit(v);
		return new JLabel("Add "+v.name, v.icon, JLabel.LEADING);
	}
	
	public void fillModifiedData(Collection<OsmPrimitive> modified, Collection<OsmPrimitive> deleted, Collection<OsmPrimitive> added) {
		if (added != null && !added.contains(osm))
			added.add(osm);
	}

	/**
	 * Add the node to the nodes - list only.
	 * @param n The node to add.
	 */
	public void visit(Node n) {
		Main.main.ds.nodes.add(n);
	}

	/**
	 * Add the line segment to the list of pending line segments.
	 * @param ls The line segment to add.
	 */
	public void visit(LineSegment ls) {
		Main.main.ds.pendingLineSegments.add(ls);
		Main.main.ds.addBackReference(ls.start, ls);
		Main.main.ds.addBackReference(ls.end, ls);
	}

	/**
	 * Add the track to the dataset. Remove all line segments that were pending
	 * from the dataset.
	 */
	public void visit(Track t) {
		Main.main.ds.tracks.add(t);
		for (Iterator<LineSegment> it =  Main.main.ds.pendingLineSegments.iterator(); it.hasNext();)
			if (t.segments.contains(it.next()))
				it.remove();
		for (LineSegment ls : t.segments) {
			Main.main.ds.addBackReference(ls, t);
			Main.main.ds.addBackReference(ls.start, t);
			Main.main.ds.addBackReference(ls.end, t);
		}
	}

	/**
	 * Add the key to the parent specified by the constructor
	 */
	public void visit(Key k) {
		throw new IllegalStateException("Keys are added by using ChangeKeyValueCommand");
	}
}
