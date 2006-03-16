
package org.openstreetmap.josm.data.osm.visitor;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.swing.Icon;

import org.openstreetmap.josm.data.osm.LineSegment;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.ImageProvider;

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
		String name = getName(ls.keys);
		if (name == null)
			name = "("+ls.start.coor.lat+","+ls.start.coor.lon+") -> ("+ls.end.coor.lat+","+ls.end.coor.lon+")";
			
		this.name = name;
		icon = ImageProvider.get("data", "linesegment");
	}

	/**
	 * If the node has a name-key or id-key, this is displayed. If not, (lat,lon)
	 * is displayed.
	 */
	public void visit(Node n) {
		String name = getName(n.keys);
		if (name == null)
			name = "("+n.coor.lat+","+n.coor.lon+")";
		
		this.name = name;
		icon = ImageProvider.get("data", "node");
	}

	/**
	 * If the way has a name-key or id-key, this is displayed. If not, (x nodes)
	 * is displayed with x beeing the number of nodes in the way.
	 */
	public void visit(Way t) {
		String name = getName(t.keys);
		if (name == null) {
			Set<Node> nodes = new HashSet<Node>();
			for (LineSegment ls : t.segments) {
				nodes.add(ls.start);
				nodes.add(ls.end);
			}
			name = "("+nodes.size()+" nodes)";
		}
		
		this.name = name;
		icon = ImageProvider.get("data", "way");
	}

	
	/**
	 * Try to read a name from the given properties.
	 * @param keys The properties to search for a name. Can be <code>null</code>.
	 * @return If a name could be found, return it here.
	 */
	public String getName(Map<String, String> keys) {
		String name = null;
		if (keys != null) {
			name = keys.get("name");
			if (name == null)
				name = keys.get("id");
		}
		return name;
	}
	
}
