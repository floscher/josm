
package org.openstreetmap.josm.data.osm.visitor;

import java.util.HashSet;
import java.util.Set;

import javax.swing.Icon;
import javax.swing.JLabel;

import org.openstreetmap.josm.data.osm.Segment;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.tools.ImageProvider;

/**
 * Able to create a name and an icon for each data element.
 * 
 * @author imi
 */
public class NameVisitor implements Visitor {

	/**
	 * The name of the item class
	 */
	public String className;
	/**
	 * The name of this item.
	 */
	public String name;
	/**
	 * The icon of this item.
	 */
	public Icon icon;
	
	
	/**
	 * If the segment has a key named "name", its value is displayed. 
	 * Otherwise, if it has "id", this is used. If none of these available, 
	 * "(x1,y1) -> (x2,y2)" is displayed with the nodes coordinates.
	 */
	public void visit(Segment ls) {
		name = ls.get("name");
		if (name == null) {
			if (ls.incomplete)
				name = ls.id == 0 ? "new" : ""+ls.id+" (unknown)";
			else
				name = (ls.id==0?"":ls.id+" ")+"("+ls.from.coor.lat()+","+ls.from.coor.lon()+") -> ("+ls.to.coor.lat()+","+ls.to.coor.lon()+")";
		}
		icon = ImageProvider.get("data", "segment");
		className = "segment";
	}

	/**
	 * If the node has a name-key or id-key, this is displayed. If not, (lat,lon)
	 * is displayed.
	 */
	public void visit(Node n) {
		name = n.get("name");
		if (name == null)
			name = (n.id==0?"":""+n.id)+" ("+n.coor.lat()+","+n.coor.lon()+")";
		icon = ImageProvider.get("data", "node");
		className = "node";
	}

	/**
	 * If the way has a name-key or id-key, this is displayed. If not, (x nodes)
	 * is displayed with x beeing the number of nodes in the way.
	 */
	public void visit(Way w) {
		name = w.get("name");
		if (name == null) {
			AllNodesVisitor.getAllNodes(w.segments);
			boolean incomplete = false;
			Set<Node> nodes = new HashSet<Node>();
			for (Segment ls : w.segments) {
				if (!ls.incomplete) {
					nodes.add(ls.from);
					nodes.add(ls.to);
				} else
					incomplete = true;
			}
			name = nodes.size()+" nodes";
			if (incomplete)
				name += " (incomplete)";
		}
		icon = ImageProvider.get("data", "way");
		className = "way";
	}
	
	public JLabel toLabel() {
		return new JLabel(name, icon, JLabel.HORIZONTAL);
	}
}
