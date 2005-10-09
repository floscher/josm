
package org.openstreetmap.josm.data.osm.visitor;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.swing.Icon;

import org.openstreetmap.josm.data.osm.Key;
import org.openstreetmap.josm.data.osm.LineSegment;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.Track;
import org.openstreetmap.josm.gui.ImageProvider;

/**
 * Able to create a name and an icon for each data element.
 * 
 * @author imi
 */
public class SelectionComponentVisitor extends Visitor {

	/**
	 * The name of this item.
	 */
	public String name;
	/**
	 * The icon of this item.
	 */
	public Icon icon;
	
	
	/**
	 * A key icon and the name of the key.
	 */
	@Override
	public void visit(Key k) {
		name = k.name;
		icon = ImageProvider.get("data", "key");
	}

	/**
	 * If the line segment has a key named "name", its value is displayed. 
	 * Otherwise, if it has "id", this is used. If none of these available, 
	 * "(x1,y1) -> (x2,y2)" is displayed with the nodes coordinates.
	 */
	@Override
	public void visit(LineSegment ls) {
		String name = getName(ls.keys);
		if (name == null)
			name = "("+ls.getStart().coor.lat+","+ls.getStart().coor.lon+") -> ("+ls.getEnd().coor.lat+","+ls.getEnd().coor.lon+")";
			
		this.name = name;
		icon = ImageProvider.get("data", "linesegment");
	}

	/**
	 * If the node has a name-key or id-key, this is displayed. If not, (lat,lon)
	 * is displayed.
	 */
	@Override
	public void visit(Node n) {
		String name = getName(n.keys);
		if (name == null)
			name = "("+n.coor.lat+","+n.coor.lon+")";
		
		this.name = name;
		icon = ImageProvider.get("data", "node");
	}

	/**
	 * If the track has a name-key or id-key, this is displayed. If not, (x nodes)
	 * is displayed with x beeing the number of nodes in the track.
	 */
	@Override
	public void visit(Track t) {
		String name = getName(t.keys);
		if (name == null) {
			Set<Node> nodes = new HashSet<Node>();
			for (LineSegment ls : t.segments()) {
				nodes.add(ls.getStart());
				nodes.add(ls.getEnd());
			}
			name = "("+nodes.size()+" nodes)";
		}
		
		this.name = name;
		icon = ImageProvider.get("data", "track");
	}

	
	/**
	 * Try to read a name from the given properties.
	 * @param keys The properties to search for a name. Can be <code>null</code>.
	 * @return If a name could be found, return it here.
	 */
	public String getName(Map<Key, String> keys) {
		String name = null;
		if (keys != null) {
			name = keys.get(Key.get("name"));
			if (name == null)
				name = keys.get(Key.get("id"));
		}
		return name;
	}
	
}
