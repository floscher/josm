package org.openstreetmap.josm.io;

import java.io.IOException;
import java.io.Reader;
import java.util.Collection;
import java.util.LinkedList;

import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.Namespace;
import org.jdom.input.SAXBuilder;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.gui.layer.RawGpsDataLayer.GpsPoint;

/**
 * Read raw gps data from a gpx file. Only way points with their ways segments
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
	public Collection<Collection<GpsPoint>> parse() throws JDOMException, IOException {
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
	private Collection<Collection<GpsPoint>> parseData(Element root) throws JDOMException {
		Collection<Collection<GpsPoint>> data = new LinkedList<Collection<GpsPoint>>();

		// workaround for bug where the server adds /gpx.asd to the namespace
		GPX = Namespace.getNamespace(root.getNamespaceURI());
		
		for (Object o : root.getChildren("wpt", GPX)) {
			Collection<GpsPoint> line = new LinkedList<GpsPoint>();
			line.add(new GpsPoint(
					new LatLon(parseDouble((Element)o, LatLonAttr.lat), parseDouble((Element)o, LatLonAttr.lon)),
					((Element)o).getChildText("time", GPX)));
			data.add(line);
		}
		for (Object o : root.getChildren("trk", GPX)) {
			for (Object seg : ((Element)o).getChildren("trkseg", GPX)) {
				Collection<GpsPoint> data1 = new LinkedList<GpsPoint>();
				for (Object trkObj : ((Element)seg).getChildren("trkpt", GPX)) {
					data1.add(new GpsPoint(
							new LatLon(parseDouble((Element)trkObj, LatLonAttr.lat), parseDouble((Element)trkObj, LatLonAttr.lon)),
							((Element)trkObj).getChildText("time", GPX)));
				}
				Collection<GpsPoint> line = data1;
				if (!line.isEmpty())
					data.add(line);
			}
		}
		return data;
	}

	private enum LatLonAttr {lat, lon}
	/**
	 * Return a parsed float value from the element behind the object o.
	 * @param o An object of dynamic type org.jdom.Element (will be casted).
	 * @param attr The name of the attribute.
	 * @throws JDOMException If the absolute of the value is out of bound.
	 */
	private double parseDouble(Element e, LatLonAttr attr) throws JDOMException {
		double d = Double.parseDouble(e.getAttributeValue(attr.toString()));
		if (Math.abs(d) > (attr == LatLonAttr.lat ? 90 : 180))
			throw new JDOMException("Data error: "+attr+" value '"+d+"' is out of bound.");
		return d;
	}
}
