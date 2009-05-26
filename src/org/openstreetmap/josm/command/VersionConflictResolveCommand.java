// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.command;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.Collection;

import javax.swing.JLabel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.MutableTreeNode;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.tools.ImageProvider;

/**
 * Represents a command for resolving a version conflict between two {@see OsmPrimitive}
 *
 *
 */
public class VersionConflictResolveCommand extends Command {

    private OsmPrimitive my;
    private OsmPrimitive their;
    
    /**
     * constructor 
     * @param my  my primitive (i.e. the primitive from the local dataset) 
     * @param their their primitive (i.e. the primitive from the server) 
     */
    public VersionConflictResolveCommand(OsmPrimitive my, OsmPrimitive their) {
      this.my = my;
      this.their = their; 
    }
    
    //FIXME copied from TagConflictResolveCommand -> refactor
    /**
     * replies a (localized) display name for the type of an OSM primitive
     * 
     * @param primitive the primitive
     * @return a localized display name
     */
    protected String getPrimitiveTypeAsString(OsmPrimitive primitive) {
        if (primitive instanceof Node) return tr("node");
        if (primitive instanceof Way) return tr("way");
        if (primitive instanceof Relation) return tr("relation");
        return "";
    }
    
    @Override
    public MutableTreeNode description() {
        return new DefaultMutableTreeNode(
            new JLabel(
               tr("Resolve version conflicts for {0} {1}",getPrimitiveTypeAsString(my), my.id), 
               ImageProvider.get("data", "object"), 
               JLabel.HORIZONTAL
            )
         );
    }

    @Override
    public boolean executeCommand() {
        super.executeCommand();
        my.version = Math.max(my.version, their.version);
        Main.map.conflictDialog.conflicts.remove(my);
        return true;
    }

    @Override
    public void fillModifiedData(Collection<OsmPrimitive> modified, Collection<OsmPrimitive> deleted,
            Collection<OsmPrimitive> added) {
        modified.add(my);
    }

    @Override
    public void undoCommand() {
        super.undoCommand();
        
        // restore a conflict if necessary
        //
        if (!Main.map.conflictDialog.conflicts.containsKey(my)) {
            Main.map.conflictDialog.conflicts.put(my,their);
        }
    }

    
}
