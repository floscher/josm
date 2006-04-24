package org.openstreetmap.josm.actions.mapmode;

import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;

import javax.swing.JOptionPane;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.command.AddCommand;
import org.openstreetmap.josm.command.ChangeCommand;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.command.DeleteCommand;
import org.openstreetmap.josm.data.SelectionChangedListener;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Segment;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.MapFrame;

/**
 * Add a new way. The action is split into the first phase, where a new way get
 * created or selected and the second, where this way is modified.
 *
 * Way creation mode:
 * If there is a selection when the mode is entered, all segments in this
 * selection form a new way. All non-segment objects are deselected. If there
 * were ways selected, the user is asked whether to select all segments of these
 * ways or not, except there is exactly one way selected, which enter the
 * edit ways mode for this way immediatly.
 * 
 * If there is no selection on entering, and the user clicks on an segment, 
 * the way editing starts the with a new way and this segment. If the user click
 * on a way (not holding Alt down), then this way is edited in the way edit mode.
 *
 * Way editing mode:
 * The user can click on subsequent segments. If the segment belonged to the way
 * it get removed from the way. Elsewhere it get added to the way. JOSM try to add
 * the segment in the correct position. This is done by searching for connections
 * to the segment at its 'to' node which are also in the way. The segemnt is 
 * inserted in the way as predecessor of the found segment (or at last segment, if
 * nothing found). 
 *
 * @author imi
 */
public class AddWayAction extends MapMode implements SelectionChangedListener {

	private Way way;

	/**
	 * Create a new AddWayAction.
	 * @param mapFrame The MapFrame this action belongs to.
	 * @param followMode The mode to go into when finished creating a way.
	 */
	public AddWayAction(MapFrame mapFrame) {
		super("Add Way", "addway", "Add a new way to the data.", "W", KeyEvent.VK_W, mapFrame);
		
		Main.ds.addSelectionChangedListener(this);
	}

	@Override public void enterMode() {
		super.enterMode();
		Command c = null;
		way = makeWay();
		if (way != null) {
			c = new AddCommand(way);
			Main.ds.setSelected(way);
			Main.main.editLayer().add(c);
		} else
			Main.ds.clearSelection();
		Main.map.mapView.addMouseListener(this);
	}

	@Override public void exitMode() {
		super.exitMode();
		way = null;
		Main.map.mapView.removeMouseListener(this);
	}

	@Override public void mouseClicked(MouseEvent e) {
		if (e.getButton() != MouseEvent.BUTTON1)
			return;

		Segment s = Main.map.mapView.getNearestSegment(e.getPoint());
		if (s == null)
			return;

		// special case for initial selecting one way
		if (way == null && (e.getModifiers() & MouseEvent.ALT_DOWN_MASK) == 0) {
			Way w = Main.map.mapView.getNearestWay(e.getPoint());
			if (w != null) {
				way = w;
				Main.ds.setSelected(way);
				return;
			}
		}

		if (way != null && way.segments.contains(s)) {
			Way copy = new Way(way);

			copy.segments.remove(s);
			if (copy.segments.isEmpty()) {
				Main.main.editLayer().add(new DeleteCommand(Arrays.asList(new OsmPrimitive[]{way})));
				way = null;
			} else
				Main.main.editLayer().add(new ChangeCommand(way, copy));
		} else {
			if (way == null) {
				way = new Way();
				way.segments.add(s);
				Main.main.editLayer().add(new AddCommand(way));
			} else {
				Way copy = new Way(way);
				int i;
				for (i = 0; i < way.segments.size(); ++i)
					if (way.segments.get(i).from == s.to)
						break;
				copy.segments.add(i, s);
				Main.main.editLayer().add(new ChangeCommand(way, copy));
			}
		}
		Main.ds.setSelected(way);
	}

	/**
	 * Form a way, either out of the (one) selected way or by creating a way over the selected
	 * line segments.
	 */
	private Way makeWay() {
		Collection<OsmPrimitive> selection = Main.ds.getSelected();
		if (selection.isEmpty())
			return null;

		if (selection.size() == 1 && selection.iterator().next() instanceof Way)
			return (Way)selection.iterator().next();

		HashSet<Segment> segmentSet = new HashSet<Segment>();
		int numberOfSelectedWays = 0;
		for (OsmPrimitive osm : selection) {
			if (osm instanceof Way)
				numberOfSelectedWays++;
			else if (osm instanceof Segment)
				segmentSet.add((Segment)osm);
		}

		if (numberOfSelectedWays > 0) {
			String ways = "way" + (numberOfSelectedWays==1?" has":"s have");
			int answer = JOptionPane.showConfirmDialog(Main.parent, numberOfSelectedWays+" "+ways+" been selected.\n" +
					"Do you wish to select all segments belonging to the "+ways+" instead?", "Add segments from ways", JOptionPane.YES_NO_OPTION);
			if (answer == JOptionPane.YES_OPTION) {
				for (OsmPrimitive osm : selection)
					if (osm instanceof Way)
						segmentSet.addAll(((Way)osm).segments);
			}
		}

		if (segmentSet.isEmpty())
			return null;

		// sort the segments in best possible order. This is done by:
		// 0  if no elements in list, quit
		// 1  taking the first ls as pivot, remove it from list
		// 2  searching for a connection at from or to of pivot
		// 3  if found, attach it, remove it from list, goto 2
		// 4  if not found, save the pivot-string and goto 0
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

		if (JOptionPane.YES_OPTION != JOptionPane.showConfirmDialog(Main.parent, "Create a new way out of "+sortedSegments.size()+" segments?", "Create new way", JOptionPane.YES_NO_OPTION))
			return null;

		Way w = new Way();
		w.segments.addAll(sortedSegments);
		return w;
	}

	public void selectionChanged(Collection<? extends OsmPrimitive> newSelection) {
		if (newSelection.size() == 1) {
			OsmPrimitive osm = newSelection.iterator().next();
			way = osm instanceof Way ? (Way)osm : null;
		} else
			way = null;
    }
}
