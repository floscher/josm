package org.openstreetmap.josm.command;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import javax.swing.DefaultListModel;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.conflict.ConflictItem;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.gui.ConflictResolver;

public class ConflictResolveCommand extends Command {

	private final Collection<ConflictItem> conflicts;
	private final Map<OsmPrimitive, OsmPrimitive> resolved;
	private Map<OsmPrimitive, OsmPrimitive> origAllConflicts;

	public ConflictResolveCommand(List<ConflictItem> conflicts, Map<OsmPrimitive, OsmPrimitive> resolved) {
		this.conflicts = conflicts;
		this.resolved = resolved;
	}

	@Override public void executeCommand() {
		super.executeCommand();

		origAllConflicts = new HashMap<OsmPrimitive, OsmPrimitive>(Main.main.getMapFrame().conflictDialog.conflicts);

		
		Set<OsmPrimitive> completed = new HashSet<OsmPrimitive>(resolved.keySet());
		for (ConflictItem ci : conflicts) {
			for (Entry<OsmPrimitive, OsmPrimitive> e : resolved.entrySet()) {
				if (ci.resolution == ConflictResolver.Resolution.THEIR)
					ci.apply(e.getKey(), e.getValue());
				else if (ci.resolution == ConflictResolver.Resolution.MY)
					ci.apply(e.getValue(), e.getKey());
				else if (ci.hasConflict(e.getKey(), e.getValue()))
					completed.remove(e.getKey());
			}
		}
		for (OsmPrimitive k : completed) {
			Main.main.getMapFrame().conflictDialog.conflicts.remove(k);
			Main.main.getMapFrame().conflictDialog.model.removeElement(k);
		}
	}

	@Override public void undoCommand() {
		super.undoCommand();
		Map<OsmPrimitive, OsmPrimitive> c = Main.main.getMapFrame().conflictDialog.conflicts;
		DefaultListModel m = Main.main.getMapFrame().conflictDialog.model;

		c.clear();
		c.putAll(origAllConflicts);
		m.removeAllElements();
		for (Entry<OsmPrimitive, OsmPrimitive> e : c.entrySet())
			m.addElement(e.getKey());
	}

	@Override public void fillModifiedData(Collection<OsmPrimitive> modified, Collection<OsmPrimitive> deleted, Collection<OsmPrimitive> added) {
		modified.addAll(resolved.keySet());
	}
}
