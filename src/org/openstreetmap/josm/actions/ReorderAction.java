package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;

import javax.swing.JOptionPane;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.command.ChangeCommand;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Segment;
import org.openstreetmap.josm.data.osm.Way;

public class ReorderAction extends JosmAction {

	public ReorderAction() {
		super(tr("Reorder segments"), "reorder", tr("Try to reorder segments of a way so that they are in a line. May try to flip segments around to match a line."), KeyEvent.VK_R, KeyEvent.CTRL_DOWN_MASK | KeyEvent.ALT_DOWN_MASK, true);
    }
	
	public void actionPerformed(ActionEvent e) {
		Collection<Way> ways = new LinkedList<Way>();
		for (OsmPrimitive osm : Main.ds.getSelected())
			if (osm instanceof Way)
				ways.add((Way)osm);
		if (ways.size() < 1) {
			JOptionPane.showMessageDialog(Main.parent, tr("Please select at least one way."));
			return;
		}
		if (ways.size() > 1) {
			int answer = JOptionPane.showConfirmDialog(Main.parent, tr("You selected more than one way. Reorder the segments of {0} ways?"), tr("Reorder segments"), JOptionPane.OK_CANCEL_OPTION);
			if (answer != JOptionPane.OK_OPTION)
				return;
		}
		for (Way way : ways) {
			Way newWay = new Way(way);
			newWay.segments.clear();
			newWay.segments.addAll(sortSegments(new HashSet<Segment>(way.segments)));
			Main.main.editLayer().add(new ChangeCommand(way, newWay));
			Main.map.mapView.repaint();
		}
	}

	/**
	 * sort the segments in best possible order. This is done by:
	 * 0  if no elements in list, quit
	 * 1  taking the first ls as pivot, remove it from list
	 * 2  searching for a connection at from or to of pivot
	 * 3  if found, attach it, remove it from list, goto 2
	 * 4  if not found, save the pivot-string and goto 0
	 */
	public static LinkedList<Segment> sortSegments(HashSet<Segment> segmentSet) {
		LinkedList<Segment> sortedSegments = new LinkedList<Segment>();
		LinkedList<Segment> segments = new LinkedList<Segment>(segmentSet);
		while (!segments.isEmpty()) {
			LinkedList<Segment> pivotList = new LinkedList<Segment>();
			pivotList.add(segments.getFirst());
			segments.removeFirst();
			for (boolean found = true; found;) {
				found = false;
				for (Iterator<Segment> it = segments.iterator(); it.hasNext();) {
					Segment ls = it.next();
					if (ls.incomplete)
						continue; // incomplete segments are never added to a new way
					if (ls.from == pivotList.getLast().to) {
						pivotList.addLast(ls);
						it.remove();
						found = true;
					} else if (ls.to == pivotList.getFirst().from) {
						pivotList.addFirst(ls);
						it.remove();
						found = true;
					}
				}
			}
			sortedSegments.addAll(pivotList);
		}
	    return sortedSegments;
    }
}
