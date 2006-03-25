
package org.openstreetmap.josm.data.osm.visitor;

import java.util.HashSet;
import java.util.Set;

import javax.swing.Icon;

import org.openstreetmap.josm.data.osm.LineSegment;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.tools.ImageProvider;

/**
 * Able to create a name and an icon for each data element.
 * 
 * @author imi
 */
public class SelectionComponentVisitor implements Visitor {

	/**
	 * The name of this item.
	 */
	public String name;
	/**
	 * The icon of this item.
	 */
	public Icon icon;
	
	
	/**
	 * If the line segment has a key named "name", its value is displayed. 
	 * Otherwise, if it has "id", this is used. If none of these available, 
	 * "(x1,y1) -> (x2,y2)" is displayed with the nodes coordinates.
	 */
	public void visit(LineSegment ls) {
		name = ls.get("name");
		if (name == null) {
			if (ls.incomplete)
				name = ""+ls.id;
			else
				name = ls.id+" ("+ls.from.coor.lat()+","+ls.from.coor.lon()+") -> ("+ls.to.coor.lat()+","+ls.to.coor.lon()+")";
		}
		icon = ImageProvider.get("data", "linesegment");
	}

	/**
	 * If the node has a name-key or id-key, this is displayed. If not, (lat,lon)
	 * is displayed.
	 */
	public void visit(Node n) {
		name = n.get("name");
		if (name == null)
			name = n.id+" ("+n.coor.lat()+","+n.coor.lon()+")";
		icon = ImageProvider.get("data", "node");
	}

	/**
	 * If the way has a name-key or id-key, this is displayed. If not, (x nodes)
	 * is displayed with x beeing the number of nodes in the way.
	 */
	public void visit(Way w) {
		name = w.get("name");
		if (name == null) {
			AllNodesVisitor.getAllNodes(w.segments);
			Set<Node> nodes = new HashSet<Node>();
			for (LineSegment ls : w.segments) {
				if (!ls.incomplete) {
					nodes.add(ls.from);
					nodes.add(ls.to);
				}
			}
			name = "("+nodes.size()+" nodes)";
		}
		icon = ImageProvider.get("data", "way");
	}
}
