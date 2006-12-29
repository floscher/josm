/**
 * 
 */
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.Collection;
import java.util.LinkedList;

import javax.swing.JOptionPane;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.command.ChangeCommand;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.command.SequenceCommand;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Segment;

public final class ReverseSegmentAction extends JosmAction {

    public ReverseSegmentAction() {
    	super(tr("Reverse Segments"), "segmentflip", tr("Revert the direction of all selected Segments."), KeyEvent.VK_R, KeyEvent.CTRL_MASK | KeyEvent.SHIFT_MASK, true);
    }

	public void actionPerformed(ActionEvent e) {
    	Collection<OsmPrimitive> sel = Main.ds.getSelected();
    	boolean hasSegments = false;
    	for (OsmPrimitive osm : sel) {
    		if (osm instanceof Segment) {
    			hasSegments = true;
    			break;
    		}
    	}
    	if (!hasSegments) {
    		JOptionPane.showMessageDialog(Main.parent, tr("Please select at least one segment."));
    		return;
    	}
    	Collection<Command> c = new LinkedList<Command>();
    	for (OsmPrimitive osm : sel) {
    		if (!(osm instanceof Segment))
    			continue;
    		Segment s = (Segment)osm;
    		Segment snew = new Segment(s);
    		Node n = snew.from;
    		snew.from = snew.to;
    		snew.to = n;
    		c.add(new ChangeCommand(s, snew));
    	}
    	Main.main.editLayer().add(new SequenceCommand(tr("Reverse Segments"), c));
    	Main.map.repaint();
    }
}