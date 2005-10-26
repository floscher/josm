package org.openstreetmap.josm.command;

import java.awt.Component;
import java.util.Collection;
import java.util.LinkedList;

import javax.swing.JLabel;

import org.openstreetmap.josm.Main;
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
public class DeleteCommand implements Command, Visitor {

	/**
	 * The primitives that are going to deleted.
	 */
	private final Collection<OsmPrimitive> data;
	
	public DeleteCommand(Collection<OsmPrimitive> data) {
		this.data = data;
	}
	
	public void executeCommand() {
		for (OsmPrimitive osm : data)
			osm.visit(this);
	}

	public Component commandDescription() {
		return new JLabel("Delete "+data.size()+" primitives");
	}

	public void fillModifiedData(Collection<OsmPrimitive> modified,
			Collection<OsmPrimitive> deleted, Collection<OsmPrimitive> added) {
		if (deleted != null)
			deleted.addAll(data);
	}


	public void visit(Node n) {
		Main.main.ds.nodes.remove(n);
		Main.main.ds.removeBackReference(n);
	}

	public void visit(LineSegment ls) {
		Main.main.ds.pendingLineSegments.remove(ls);
		LinkedList<Track> tracksToDelete = new LinkedList<Track>();
		for (Track t : Main.main.ds.tracks) {
			t.segments.remove(ls);
			if (t.segments.isEmpty())
				tracksToDelete.add(t);
		}
		for (Track t : tracksToDelete) {
			Main.main.ds.tracks.remove(t);
			Main.main.ds.removeBackReference(t);
		}
		Main.main.ds.removeBackReference(ls);
	}

	public void visit(Track t) {
		Main.main.ds.tracks.remove(t);
		for (LineSegment ls : t.segments)
			Main.main.ds.pendingLineSegments.add(ls);
		Main.main.ds.removeBackReference(t);
	}

	public void visit(Key k) {
		// TODO
	}

}
