package org.openstreetmap.josm.command;

import java.awt.Component;
import java.util.Collection;
import java.util.Map;

import javax.swing.JLabel;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Key;
import org.openstreetmap.josm.data.osm.LineSegment;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Track;
import org.openstreetmap.josm.data.osm.visitor.SelectionComponentVisitor;

/**
 * This is a combination of first combining objects to get a node free of 
 * references and then delete that node. It is used by the delete action.
 * @author imi
 */
public class CombineAndDeleteCommand implements Command {

	/**
	 * This class is used as one line segment pair that needs to get combined
	 * for the node to be deleted.
	 * @author imi
	 */
	public static class LineSegmentCombineEntry {
		public LineSegment first, second;
		public Track track;
	}
	
	/**
	 * The node that get deleted
	 */
	private Node node;
	/**
	 * These line segments are 
	 */
	private Collection<LineSegmentCombineEntry> combineLineSegments;
	/**
	 * These tracks are combined
	 */
	private Track firstTrack, secondTrack;
	/**
	 * This line segment is deleted together with the second track. It was the
	 * first segment of the second track (the other line segments were integrated
	 * into the first track).
	 */
	private LineSegment firstOfSecond;

	/**
	 * Create the command and assign the data entries.
	 */
	public CombineAndDeleteCommand(Node nodeToDelete, 
			Collection<LineSegmentCombineEntry> combineLineSegments,
			Track firstTrack, Track secondTrack) {
		node = nodeToDelete;
		this.combineLineSegments = combineLineSegments;
		this.firstTrack = firstTrack;
		this.secondTrack = secondTrack;
	}
	
	public void executeCommand() {
		// line segments
		DataSet ds = Main.main.ds;
		for (LineSegmentCombineEntry e : combineLineSegments) {
			if (e.first.start == e.second.end) {
				LineSegment tmp = e.first;
				e.first = e.second;
				e.second = tmp;
			}
			e.first.end = e.second.end;
			e.first.keys = mergeKeys(e.first.keys, e.second.keys);
			e.track.segments.remove(e.second);
		}
		
		// tracks
		if (firstTrack != null && secondTrack != null) {
			if (firstTrack.getStartingNode() == secondTrack.getEndingNode()) {
				Track t = firstTrack;
				firstTrack = secondTrack;
				secondTrack = t;
			}
			// concatenate the line segments.
			LineSegment lastOfFirst = firstTrack.getEndingSegment();
			firstOfSecond = secondTrack.getStartingSegment();
			lastOfFirst.end = firstOfSecond.end;
			lastOfFirst.keys = mergeKeys(lastOfFirst.keys, firstOfSecond.keys);
			secondTrack.segments.remove(firstOfSecond);
			// move the remaining line segments to first track.
			firstTrack.segments.addAll(secondTrack.segments);
			ds.tracks.remove(secondTrack);
		}
		ds.nodes.remove(node);
		ds.rebuildBackReferences();
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
		v.visit(node);
		return new JLabel("Remove "+v.name, v.icon, JLabel.LEADING);
	}

	public void fillModifiedData(Collection<OsmPrimitive> modified, Collection<OsmPrimitive> deleted, Collection<OsmPrimitive> added) {
		deleted.add(node);
		if (firstTrack != null)
			modified.add(firstTrack);
		if (secondTrack != null)
			deleted.add(secondTrack);
		if (firstOfSecond != null)
			deleted.add(firstOfSecond);
		for (LineSegmentCombineEntry e : combineLineSegments) {
			modified.add(e.first);
			deleted.add(e.second);
			modified.add(e.track);
		}
	}

}
