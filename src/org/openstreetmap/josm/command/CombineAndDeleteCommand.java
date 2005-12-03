package org.openstreetmap.josm.command;

import java.awt.Component;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.swing.JLabel;

import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Key;
import org.openstreetmap.josm.data.osm.LineSegment;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Track;
import org.openstreetmap.josm.data.osm.visitor.SelectionComponentVisitor;

/**
 * This is a combination of first combining objects to get a node free of 
 * references and then delete that node. It is used by the delete action.
 * 
 * The rules is as follow:
 * If the node to delete is between exact two line segments, which are
 * in a straight (not pointing together), the second line segment is deleted
 * and the first now spans to the last node of the second line segment.
 * 
 * @author imi
 */
public class CombineAndDeleteCommand implements Command {

	/**
	 * The dataset, this command operates on.
	 */
	private DataSet ds;
	/**
	 * This line segment is combined with the second line segment.
	 * The node that get deleted is the end of this segment.
	 */
	private LineSegment first;
	/**
	 * This line segment is deleted by the combining.
	 * The node that get deleted is the start of this segment.
	 */
	private LineSegment second;

	/**
	 * The tracks (if any) the line segments are part of.
	 */
	private List<Track> track;

	
	// stuff for undo

	/**
	 * The old properties of the first line segment (for undo)
	 */
	private Map<Key, String> oldProperties;
	/**
	 * The positions of the second line segment in the tracks (if any track)
	 */
	private List<Integer> lineSegmentTrackPos;
	
	/**
	 * Create the command and assign the data entries.
	 * @param ds     The dataset this command operates on.
	 * @param first  The line segment that remain alive
	 * @param second The line segment that get deleted
	 */
	public CombineAndDeleteCommand(DataSet ds, LineSegment first, LineSegment second) {
		this.ds = ds;
		this.first = first;
		this.second = second;
		if (first.end != second.start)
			throw new IllegalArgumentException();
	}
	
	public void executeCommand() {
		first.end = second.end;
		oldProperties = new HashMap<Key, String>(first.keys);
		first.keys = mergeKeys(first.keys, second.keys);

		// delete second line segment
		for (Track t : ds.tracks) {
			if (t.segments.contains(second)) {
				if (track == null)
					track = new LinkedList<Track>();
				track.add(t);
			}
		}
		if (track != null) {
			lineSegmentTrackPos = new LinkedList<Integer>();
			for (Track t : track) {
				int i = t.segments.indexOf(second);
				if (i != -1)
					t.segments.remove(second);
				lineSegmentTrackPos.add(i);
			}
		}
		ds.lineSegments.remove(second);
		
		// delete node
		ds.nodes.remove(second.start);
	}

	public void undoCommand() {
		ds.nodes.add(second.start);
		ds.lineSegments.add(second);
		
		if (track != null) {
			Iterator<Track> it = track.iterator();
			for (int i : lineSegmentTrackPos) {
				Track t = it.next();
				if (i != -1)
					t.segments.add(i, second);
			}
		}
		first.keys = oldProperties;
		first.end = second.start;
	}

	/**
	 * Merges the second parameter into the first and return the merged map.
	 * @param first The first map that will hold keys.
	 * @param second The map to merge with the first.
	 * @return The merged key map.
	 */
	private Map<Key, String> mergeKeys(Map<Key, String> first, Map<Key, String> second) {
		if (first == null)
			first = second;
		else if (second != null && first != null)
			first.putAll(second);
		return first;
	}

	public Component commandDescription() {
		SelectionComponentVisitor v = new SelectionComponentVisitor();
		v.visit(second.start);
		return new JLabel("Remove "+v.name, v.icon, JLabel.LEADING);
	}

	public void fillModifiedData(Collection<OsmPrimitive> modified, Collection<OsmPrimitive> deleted, Collection<OsmPrimitive> added) {
		deleted.add(second);
		deleted.add(second.start);
		modified.add(first);
		if (track != null)
			modified.addAll(track);
	}

}
