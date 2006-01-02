package org.openstreetmap.josm.gui.layer;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Collection;

import javax.swing.Icon;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.GeoPoint;
import org.openstreetmap.josm.data.projection.Projection;
import org.openstreetmap.josm.gui.ImageProvider;
import org.openstreetmap.josm.gui.MapView;

/**
 * A layer holding data from a gps source.
 * The data is read only.
 * 
 * @author imi
 */
public class RawGpsDataLayer extends Layer {

	private static Icon icon;

	/**
	 * A list of tracks which containing a list of points.
	 */
	private final Collection<Collection<GeoPoint>> data;

	public RawGpsDataLayer(Collection<Collection<GeoPoint>> data, String name) {
		super(name);
		this.data = data;
		Main.pref.addPropertyChangeListener(new PropertyChangeListener(){
			public void propertyChange(PropertyChangeEvent evt) {
				if (Main.main.getMapFrame() == null) {
					Main.pref.removePropertyChangeListener(this);
					return;
				}
				if (evt.getPropertyName().equals("drawRawGpsLines") ||
						evt.getPropertyName().equals("forceRawGpsLines"))
					Main.main.getMapFrame().repaint();
			}
		});
	}

	/**
	 * Return a static icon.
	 */
	@Override
	public Icon getIcon() {
		if (icon == null)
			icon = ImageProvider.get("layer", "rawgps");
		return icon;
	}

	@Override
	public void paint(Graphics g, MapView mv) {
		g.setColor(Color.GRAY);
		Point old = null;
		for (Collection<GeoPoint> c : data) {
			if (!Main.pref.isForceRawGpsLines())
				old = null;
			for (GeoPoint p : c) {
				Point screen = mv.getScreenPoint(p);
				if (Main.pref.isDrawRawGpsLines() && old != null)
					g.drawLine(old.x, old.y, screen.x, screen.y);
				else
					g.drawRect(screen.x, screen.y, 0, 0);
				old = screen;
			}
		}
	}

	@Override
	public String getToolTipText() {
		return data.size()+" tracks.";
	}

	@Override
	public void mergeFrom(Layer from) {
		RawGpsDataLayer layer = (RawGpsDataLayer)from;
		data.addAll(layer.data);
	}

	@Override
	public boolean isMergable(Layer other) {
		return other instanceof RawGpsDataLayer;
	}

	@Override
	public Bounds getBoundsLatLon() {
		GeoPoint min = null;
		GeoPoint max = null;
		for (Collection<GeoPoint> c : data) {
			for (GeoPoint p : c) {
				if (min == null) {
					min = p.clone();
					max = p.clone();
				} else {
					min.lat = Math.min(min.lat, p.lat);
					min.lon = Math.min(min.lon, p.lon);
					max.lat = Math.max(max.lat, p.lat);
					max.lon = Math.max(max.lon, p.lon);
				}
			}
		}
		if (min == null)
			return null;
		return new Bounds(min, max);
	}

	@Override
	public Bounds getBoundsXY() {
		GeoPoint min = null;
		GeoPoint max = null;
		for (Collection<GeoPoint> c : data) {
			for (GeoPoint p : c) {
				if (min == null) {
					min = p.clone();
					max = p.clone();
				} else {
					min.x = Math.min(min.x, p.x);
					min.y = Math.min(min.y, p.y);
					max.x = Math.max(max.x, p.x);
					max.y = Math.max(max.y, p.y);
				}
			}
		}
		if (min == null)
			return null;
		return new Bounds(min, max);
	}

	@Override
	public void init(Projection projection) {
		for (Collection<GeoPoint> c : data)
			for (GeoPoint p : c)
				projection.latlon2xy(p);
	}
}
