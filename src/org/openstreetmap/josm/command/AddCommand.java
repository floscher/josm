package org.openstreetmap.josm.command;

import java.awt.Component;
import java.util.Collection;

import javax.swing.JLabel;

import org.openstreetmap.josm.data.osm.DataSet;
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
public class AddCommand implements Command {

	/**
	 * The dataset this command operates on.
	 */
	DataSet ds;

	/**
	 * Helper that adds the object
	 * @author imi
	 */
	private final class AddVisitor implements Visitor {
		public void visit(Node n) {ds.nodes.add(n);}
		public void visit(LineSegment ls) {ds.lineSegments.add(ls);}
		public void visit(Track t) {ds.tracks.add(t);}
		public void visit(Key k) {throw new IllegalStateException("Keys are added by using ChangeKeyValueCommand");}
	}

	/**
	 * Helper that deletes the object (for undo)
	 * @author imi
	 */
	private final class RemoveVisitor implements Visitor {
		public void visit(Node n) {ds.nodes.remove(n);}
		public void visit(LineSegment ls) {ds.lineSegments.remove(ls);}
		public void visit(Track t) {ds.tracks.remove(t);}
		public void visit(Key k) {throw new IllegalStateException("Keys are added by using ChangeKeyValueCommand");}
	}
	
	/**
	 * The primitive to add to the dataset.
	 */
	private final OsmPrimitive osm;

	/**
	 * Create the command and specify the element to add.
	 */
	public AddCommand(DataSet ds, OsmPrimitive osm) {
		this.ds = ds;
		this.osm = osm;
	}

	public void executeCommand() {
		osm.visit(new AddVisitor());
	}

	public void undoCommand() {
		osm.visit(new RemoveVisitor());
	}

	public Component commandDescription() {
		SelectionComponentVisitor v = new SelectionComponentVisitor();
		osm.visit(v);
		return new JLabel("Add "+v.name, v.icon, JLabel.LEADING);
	}
	
	public void fillModifiedData(Collection<OsmPrimitive> modified, Collection<OsmPrimitive> deleted, Collection<OsmPrimitive> added) {
		added.add(osm);
	}
}
