package org.openstreetmap.josm.actions.mapmode;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;

import javax.swing.JOptionPane;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.command.AddCommand;
import org.openstreetmap.josm.data.osm.Segment;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.MapFrame;

/**
 * Add a new way from all selected segments.
 *
 * If there is a selection when the mode is entered, all segments in this
 * selection form a new way, except the user holds down Shift.
 *
 * The user can click on a segment. If he holds down Shift, no way is 
 * created yet. If he holds down Alt, the whole way is considered instead of 
 * the clicked segment. If the user holds down Ctrl, no way is created 
 * and the clicked segment get removed from the list.
 *
 * Also, the user may select a rectangle as in selection mode. No node, area or
 * way can be selected this way.
 *
 * @author imi
 *
 */
public class AddWayAction extends MapMode {

	private MapMode followMode;
	
	/**
	 * Create a new AddWayAction.
	 * @param mapFrame The MapFrame this action belongs to.
	 * @param followMode The mode to go into when finished creating a way.
	 */
	public AddWayAction(MapFrame mapFrame, MapMode followMode) {
		super("Add Way", "addway", "Combine selected segments to a new way.", "W", KeyEvent.VK_W, mapFrame);
		this.followMode = followMode;
	}

	@Override public void actionPerformed(ActionEvent e) {
		makeWay();
		super.actionPerformed(e);
		mapFrame.selectMapMode(followMode);
	}

	/**
	 * Just make a way of all selected items.
	 */
	private void makeWay() {
		Collection<OsmPrimitive> selection = Main.ds.getSelected();
		if (selection.isEmpty())
			return;

		// form a new way
		LinkedList<Segment> segments = new LinkedList<Segment>();
		int numberOfSelectedWays = 0;
		for (OsmPrimitive osm : selection) {
			if (osm instanceof Way)
				numberOfSelectedWays++;
			else if (osm instanceof Segment)
				segments.add((Segment)osm);
		}
		
		if (numberOfSelectedWays > 0) {
			String ways = "way" + (numberOfSelectedWays==1?" has":"s have");
			int answer = JOptionPane.showConfirmDialog(Main.main, numberOfSelectedWays+" "+ways+" been selected.\n" +
					"Do you wish to select all segments belonging to the "+ways+" instead?");
			if (answer == JOptionPane.CANCEL_OPTION)
				return;
			if (answer == JOptionPane.YES_OPTION) {
				for (OsmPrimitive osm : selection)
					if (osm instanceof Way)
						segments.addAll(((Way)osm).segments);
			}
		}
		
		if (segments.isEmpty())
			return;
		
		// sort the segments in best possible order. This is done by:
		// 0  if no elements in list, quit
		// 1  taking the first ls as pivot, remove it from list
		// 2  searching for a connection at from or to of pivot
		// 3  if found, attach it, remove it from list, goto 2
		// 4  if not found, save the pivot-string and goto 0
		LinkedList<Segment> sortedSegments = new LinkedList<Segment>();
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
		
		Way w = new Way();
		w.segments.addAll(sortedSegments);
		mv.editLayer().add(new AddCommand(w));
		Main.ds.clearSelection();
		mv.repaint();
	}
}
