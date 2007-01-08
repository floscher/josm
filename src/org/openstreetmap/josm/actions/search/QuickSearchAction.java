package org.openstreetmap.josm.actions.search;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.Collection;
import java.util.Vector;

import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.JosmAction;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.visitor.BoundingXYVisitor;
import org.openstreetmap.josm.data.osm.visitor.NameVisitor;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import uk.co.wilson.xml.MinML2;

public class QuickSearchAction extends JosmAction {

	public final JTextField searchField = new JTextField(10);

	public QuickSearchAction() {
		super(tr("Quick Search"), "dialogs/search", tr("Try to search for the input string, first locally, then on geonames.org"), KeyEvent.VK_G, KeyEvent.CTRL_DOWN_MASK, false);
	}

	private static class GeoNamesHit {
		String name;
		LatLon lat;
		String lng;
		String countryCode;
		String countryName;
	}

	private class GeoNamesReader extends MinML2 {
		Collection<GeoNamesHit> result;
		GeoNamesHit current;
		String nextFieldName;
		String characters;

		@Override public void startElement(String namespaceURI, String localName, String qName, Attributes atts) throws SAXException {
			nextFieldName = qName;
			if (qName.equals("geoname"))
				current = new GeoNamesHit();
			characters = "";
		}

		@Override public void characters(char[] ch, int start, int length) {
			String s = new String(ch, start, length);
			characters += s;
		}

		@Override public void endElement(String namespaceURI, String localName, String qName) throws SAXException {
			if (qName.equals("geoname")) {
				result.add(current);
				return;
			}
			if ("|name|lat|lng|countryCode|countryName|".indexOf("|"+qName+"|") == -1)
				return;
			try {
				current.getClass().getField(qName).set(current, characters);
			} catch (Exception e) {
				throw new SAXException(e);
			}
		}
	}

	public void actionPerformed(ActionEvent e) {
		String search = searchField.getText();
		SearchCompiler.Match matcher = SearchCompiler.compile(search);
		Vector<Vector<Object>> result = new Vector<Vector<Object>>();
		Vector<String> columns = new Vector<String>();

		for (OsmPrimitive osm : Main.ds.allNonDeletedPrimitives()) {
			if (matcher.match(osm)) {
				Vector<Object> row = new Vector<Object>();
				NameVisitor v = new NameVisitor();
				BoundingXYVisitor bv = new BoundingXYVisitor();
				osm.visit(v);
				osm.visit(bv);
				LatLon center = bv.getBounds().center();
				row.add(v.icon);
				row.add(v.name);
				row.add(center.lat()+" / "+center.lon());
				result.add(row);
			}
		}
		if (!result.isEmpty()) {
			columns.add("");
			columns.add(tr("Name"));
			columns.add(tr("Location"));
		} else {
			columns.add(tr("Name"));
			columns.add(tr("Country"));
			columns.add(tr("Location"));
			String geoNamesUrl = Main.pref.get("search.geonames.url", "http://ws.geonames.org/search");
			GeoNamesReader r = new GeoNamesReader();
			try {
				URLConnection con = new URL(geoNamesUrl).openConnection();
				r.parse(new InputStreamReader(con.getInputStream()));
			} catch (SAXException e1) {
				e1.printStackTrace();
				// just nothing is found if xml response has parsing problems
			} catch (IOException e1) {
				e1.printStackTrace();
				// just nothing is found if connection problems to geonames
			}
			for (GeoNamesHit g : r.result) {
				Vector<String> row = new Vector<String>();
				row.add(g.name);
				row.add(g.countryName+" ("+g.countryCode+")");
				row.add(g.lat+" / "+g.lng);
			}
		}
		if (result.isEmpty()) {
			JOptionPane.showMessageDialog(Main.parent, tr("No hits found."));
			return;
		}

		JTable hits = new JTable(result, columns);
		JOptionPane.showMessageDialog(Main.parent, new JScrollPane(hits));
	}
}
