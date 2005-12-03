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
import org.openstreetmap.josm.data.osm.visitor.Visitor;

/**
 * A command to delete a number of primitives from the dataset.
 * @author imi
 */
public class DeleteCommand implements Command {

	/**
	 * The dataset this command operates on.
	 */
	DataSet ds;

	/**
	 * Helper that adds the object.
	 * @author imi
	 */
	private final class AddVisitor implements Visitor {
		public void visit(Node n) {ds.nodes.add(n);}
		public void visit(LineSegment ls) {ds.lineSegments.add(ls);}
		public void visit(Track t) {ds.tracks.add(t);}
		public void visit(Key k) {throw new IllegalStateException("Keys are added by using ChangeKeyValueCommand");}
	}

	/**
	 * Helper that deletes the object. Does not respect back reference cache.
	 * @author imi
	 */
	private final class DeleteVisitor implements Visitor {
		public void visit(Node n) {ds.nodes.remove(n);}
		public void visit(LineSegment ls) {ds.lineSegments.remove(ls);}
		public void visit(Track t) {ds.tracks.remove(t);}
		public void visit(Key k) {throw new IllegalStateException("Keys are added by using ChangeKeyValueCommand");}
	}
	
	
	
	/**
	 * The primitives that are going to deleted.
	 */
	private final Collection<OsmPrimitive> data;
	
	public DeleteCommand(DataSet ds, Collection<OsmPrimitive> data) {
		this.ds = ds;
		this.data = data;
	}
	
	public void executeCommand() {
		Visitor v = new DeleteVisitor();
		for (OsmPrimitive osm : data)
			osm.visit(v);
	}

	public void undoCommand() {
		Visitor v = new AddVisitor();
		for (OsmPrimitive osm : data)
			osm.visit(v);
	}

	public Component commandDescription() {
		return new JLabel("Delete "+data.size()+" primitives");
	}

	public void fillModifiedData(Collection<OsmPrimitive> modified, Collection<OsmPrimitive> deleted, Collection<OsmPrimitive> added) {
		deleted.addAll(data);
	}
}
