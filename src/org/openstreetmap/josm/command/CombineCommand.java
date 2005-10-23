package org.openstreetmap.josm.command;

import java.awt.Component;
import java.util.Collection;

import javax.swing.JLabel;
import javax.swing.JPanel;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.osm.LineSegment;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Track;
import org.openstreetmap.josm.data.osm.visitor.SelectionComponentVisitor;

/**
 * This command combines either a line segment with a track or two tracks together.
 * @author imi
 */
public class CombineCommand implements Command {

	/**
	 * Both primitives that get combined together. "mod" is modified to be the combined
	 * object and del is the deleted one.
	 */
	private final OsmPrimitive mod, del;
	
	/**
	 * Create a combine command and assign the members
	 */
	public CombineCommand(OsmPrimitive first, OsmPrimitive second) {
		if (first instanceof Track && second instanceof LineSegment) {
			mod = first;
			del = second;
		} else if (first instanceof LineSegment && second instanceof Track) {
			mod = second;
			del = first;
		} else if (((Track)first).getStartingNode() == ((Track)second).getEndingNode()) {
			mod = first;
			del = second;
		} else {
			mod = second;
			del = first;
		}
	}

	public void executeCommand() {
		if (del instanceof LineSegment) {
			LineSegment ls = (LineSegment)mod;
			Track t = (Track)del;
			if (!Main.main.ds.pendingLineSegments().contains(ls))
				throw new IllegalStateException("Should not be able to select non-pending line segments.");
			
			Main.main.ds.pendingLineSegments.remove(ls);
			if (t.getStartingNode() != ls.getEnd())
				t.add(ls);
			else
				t.addStart(ls);
		} else {
			Track t1 = (Track)mod;
			Track t2 = (Track)del;
			t1.addAll(t2.segments());
			if (t1.keys == null)
				t1.keys = t2.keys;
			else	
				t1.keys.putAll(t2.keys);
			t2.destroy();
			Main.main.ds.tracks.remove(t2);
		}
	}

	public Component commandDescription() {
		JPanel p = new JPanel();
		p.add(new JLabel("Combine"));
		SelectionComponentVisitor v = new SelectionComponentVisitor();
		mod.visit(v);
		p.add(new JLabel(v.name, v.icon, JLabel.LEADING));
		p.add(new JLabel("with"));
		del.visit(v);
		p.add(new JLabel(v.name, v.icon, JLabel.LEADING));
		return p;
	}
	
	public void fillModifiedData(Collection<OsmPrimitive> modified, Collection<OsmPrimitive> deleted, Collection<OsmPrimitive> added) {
		if (!modified.contains(mod))
			modified.add(mod);
		if (deleted.contains(del))
			throw new IllegalStateException("Deleted object twice: "+del);
		deleted.add(del);
	}
}
