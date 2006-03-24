package org.openstreetmap.josm.data.osm.visitor;

import java.util.Collection;
import java.util.LinkedList;

import org.openstreetmap.josm.data.osm.LineSegment;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.Way;

/**
 * Searches in the primitives for specific search strings.
 * @author Imi
 */
public class SearchVisitor implements Visitor {

	/**
	 * One search criteria. Criterias are seperated by space.
	 */
	static class Criteria {
		/**
		 * If <code>true</code>, the criteria must match. <code>false</code> for the
		 * criteria to NOT match.
		 */
		boolean match = true;
		/**
		 * Any Key must match this.
		 */
		String key;
		/**
		 * The value to the key must match the value. 
		 */
		String value;
		/**
		 * Any key or value must match this.
		 */
		String any;

		enum Type {node, segment, way}
		/**
		 * Must be of the given type. (<code>null</code> for any type)
		 */
		Type type;
	}
	
	private Collection<Criteria> criterias = new LinkedList<Criteria>();
	private String search;

	public SearchVisitor(String searchStr) {
		search = searchStr;
		for (String token = nextToken(); !token.equals(""); token = nextToken()) {
			Criteria crit = new Criteria();
			if (token.charAt(0) == '-' && token.length() > 1) {
				crit.match = false;
				token = token.substring(1);
			}
			if (token.charAt(0) == '"')
				crit.any = token.substring(1);
			else {
				int colon = token.indexOf(':');
				if (colon != -1) {
					crit.key = token.substring(0, colon);
					crit.value = token.substring(colon+1);
				} else
					crit.any = token;
			}
		}
	}
	
	public void visit(Node n) {
		// TODO Auto-generated method stub

	}

	public void visit(LineSegment ls) {
		// TODO Auto-generated method stub

	}

	public void visit(Way w) {
		// TODO Auto-generated method stub

	}

	
	/**
	 * Return the next token in the search string. Update the string as well.
	 */
	private String nextToken() {
		if ("".equals(search))
			return "";
		if (search.charAt(0) == '"' || search.startsWith("-\"")) {
			int start = search.charAt(0) == '"' ? 1 : 2;
			int i = search.indexOf('"', start);
			if (i == -1) {
				String token = search;
				search = "";
				return token;
			}
			String token = start == 1 ? search.substring(0, i) : "-"+search.substring(1, i);
			search = search.substring(i+1);
			return token;
		}
		int i = search.indexOf(' ');
		String token = search.substring(0, i);
		search = search.substring(i+1);
		return token;
	}
}
