package org.openstreetmap.josm.io;

import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import uk.co.wilson.xml.MinML2;

/**
 * Read only the ids and classes of an stream.
 * 
 * @author Imi
 */
public class OsmIdReader extends MinML2 {

	Map<Long, String> entries = new HashMap<Long, String>();

	@Override public void startElement(String namespaceURI, String localName, String qName, Attributes atts) throws SAXException {
		if (qName.equals("node") || qName.equals("segment") || qName.equals("way")) {
			try {
				entries.put(Long.valueOf(atts.getValue("id")), qName);
			} catch (Exception e) {
				e.printStackTrace();
				throw new SAXException("Error during parse.");
			}
		}
    }

	public static Map<Long, String> parseIds(Reader in) throws IOException, SAXException {
		OsmIdReader r = new OsmIdReader();
        r.parse(in);
		return r.entries;
	}
}
