package org.openstreetmap.josm.io;

import java.io.IOException;
import java.io.Reader;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.jdom.Element;
import org.jdom.JDOMException;
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
	private Collection<Collection<GeoPoint>> parseData(Element root) {
		Collection<Collection<GeoPoint>> data = new LinkedList<Collection<GeoPoint>>();

		for (Object o : root.getChildren("wpt", GpxReader.GPX)) {
			Collection<GeoPoint> line = new LinkedList<GeoPoint>();
			line.add(new GeoPoint(
					Float.parseFloat(((Element)o).getAttributeValue("lat")),
					Float.parseFloat(((Element)o).getAttributeValue("lon"))));
			data.add(line);
		}
		for (Object o : root.getChildren("rte", GpxReader.GPX)) {
			Collection<GeoPoint> line = parseLine(((Element)o).getChildren("rtept", GpxReader.GPX));
			if (!line.isEmpty())
				data.add(line);
		}
		for (Object o : root.getChildren("trk", GpxReader.GPX)) {
			for (Object seg : ((Element)o).getChildren("trkseg", GpxReader.GPX)) {
				Collection<GeoPoint> line = parseLine(((Element)seg).getChildren("trkpt", GpxReader.GPX));
				if (!line.isEmpty())
					data.add(line);
			}
		}
		return data;
	}

	/**
	 * Parse the list of trackpoint - elements and return a collection with the
	 * points read.
	 */
	private Collection<GeoPoint> parseLine(List<Element> wpt) {
		Collection<GeoPoint> data = new LinkedList<GeoPoint>();
		for (Element e : wpt)
			data.add(new GeoPoint(
					Float.parseFloat(e.getAttributeValue("lat")),
					Float.parseFloat(e.getAttributeValue("lon"))));
		return data;
	}
}
