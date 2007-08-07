// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.command;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.Collection;

import javax.swing.JLabel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.MutableTreeNode;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.visitor.AddVisitor;
import org.openstreetmap.josm.data.osm.visitor.DeleteVisitor;
import org.openstreetmap.josm.data.osm.visitor.NameVisitor;

/**
 * A command that adds an osm primitive to a dataset. Keys cannot be added this
 * way. Use ChangeKeyValueCommand instead.
 * 
 * @author imi
 */
public class AddCommand extends Command {

	/**
	 * The primitive to add to the dataset.
	 */
	private final OsmPrimitive osm;

	/**
	 * Create the command and specify the element to add.
	 */
	public AddCommand(OsmPrimitive osm) {
		this.osm = osm;
	}

	@Override public void executeCommand() {
		osm.visit(new AddVisitor(Main.ds));
	}

	@Override public void undoCommand() {
		osm.visit(new DeleteVisitor(Main.ds));
	}

	@Override public void fillModifiedData(Collection<OsmPrimitive> modified, Collection<OsmPrimitive> deleted, Collection<OsmPrimitive> added) {
		added.add(osm);
	}

	@Override public MutableTreeNode description() {
		NameVisitor v = new NameVisitor();
		osm.visit(v);
		return new DefaultMutableTreeNode(new JLabel(tr("Add")+" "+tr(v.className)+" "+v.name, v.icon, JLabel.HORIZONTAL));
    }
}
