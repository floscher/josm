package org.openstreetmap.josm.io;

import java.io.IOException;
import java.io.Reader;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.Namespace;
import org.jdom.input.SAXBuilder;
import org.openstreetmap.josm.data.GeoPoint;

/**
 * Read raw gps data from a gpx file. Only track points with their tracks segments
 * and waypoints are imported.
 * @author imi
 */
public class RawGpsReader {

	/**
	 * The data source from this reader.
	 */
	public Reader source;
	
	/**
	 * Construct a gps reader from an input reader source.
	 * @param source The source to read the raw gps data from. The data must be
	 * 		in GPX format.
	 */
	public RawGpsReader(Reader source) {
		this.source = source;
	}

	/**
	 * The gpx namespace.
	 */
	private Namespace GPX = Namespace.getNamespace("http://www.topografix.com/GPX/1/0");
	
	/**
	 * Parse and return the read data
	 */
	public Collection<Collection<GeoPoint>> parse() throws JDOMException, IOException {
		final SAXBuilder builder = new SAXBuilder();
		builder.setValidation(false);
		Element root = builder.build(source).getRootElement();
		return parseData(root);
	}

	/**
	 * Parse and return the whole data thing.
	 * @param root
	 * @return
	 */
	@SuppressWarnings("unchecked")
	private Collection<Collection<GeoPoint>> parseData(Element root) throws JDOMException {
		Collection<Collection<GeoPoint>> data = new LinkedList<Collection<GeoPoint>>();

		// workaround for bug where the server adds /gpx.asd to the namespace
		GPX = Namespace.getNamespace(root.getNamespaceURI());
		
		for (Object o : root.getChildren("wpt", GPX)) {
			Collection<GeoPoint> line = new LinkedList<GeoPoint>();
			line.add(new GeoPoint(parseFloat((Element)o, LatLon.lat), parseFloat((Element)o, LatLon.lon)));
			data.add(line);
		}
		for (Object o : root.getChildren("rte", GPX)) {
			Collection<GeoPoint> line = parseLine(((Element)o).getChildren("rtept", GPX));
			if (!line.isEmpty())
				data.add(line);
		}
		for (Object o : root.getChildren("trk", GPX)) {
			for (Object seg : ((Element)o).getChildren("trkseg", GPX)) {
				Collection<GeoPoint> line = parseLine(((Element)seg).getChildren("trkpt", GPX));
				if (!line.isEmpty())
					data.add(line);
			}
		}
		return data;
	}

	enum LatLon {lat, lon}
	/**
	 * Return a parsed float value from the element behind the object o.
	 * @param o An object of dynamic type org.jdom.Element (will be casted).
	 * @param attr The name of the attribute.
	 * @throws JDOMException If the absolute of the value is out of bound.
	 */
	private float parseFloat(Element e, LatLon attr) throws JDOMException {
		float f = Float.parseFloat(e.getAttributeValue(attr.toString()));
		if (Math.abs(f) > (attr == LatLon.lat ? 90 : 180))
			throw new JDOMException("Data error: "+attr+" value '"+f+"' is out of bound.");
		return f;
	}

	/**
	 * Parse the list of trackpoint - elements and return a collection with the
	 * points read.
	 */
	private Collection<GeoPoint> parseLine(List<Element> wpt) throws JDOMException {
		Collection<GeoPoint> data = new LinkedList<GeoPoint>();
		for (Element e : wpt)
			data.add(new GeoPoint(parseFloat(e, LatLon.lat), parseFloat(e, LatLon.lon)));
		return data;
	}
}
