package org.openstreetmap.josm.gui.layer;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.util.Collection;

import javax.swing.Icon;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.Preferences.PreferenceChangedListener;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.visitor.BoundingXYVisitor;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.tools.ColorHelper;
import org.openstreetmap.josm.tools.ImageProvider;

/**
 * A layer holding data from a gps source.
 * The data is read only.
 * 
 * @author imi
 */
public class RawGpsDataLayer extends Layer {

	private static Icon icon;
	
	public static class GpsPoint {
		public final LatLon latlon;
		public final EastNorth eastNorth;
		public final String time;
		public GpsPoint(LatLon ll, String t) {
			latlon = ll; 
			eastNorth = Main.proj.latlon2eastNorth(ll); 
			time = t;
		}
	}
	
	/**
	 * A list of ways which containing a list of points.
	 */
	public final Collection<Collection<GpsPoint>> data;

	public RawGpsDataLayer(Collection<Collection<GpsPoint>> data, String name) {
		super(name);
		this.data = data;
		Main.pref.addPreferenceChangedListener(new PreferenceChangedListener(){
			public void preferenceChanged(String key, String newValue) {
				if (Main.main.getMapFrame() != null && (key.equals("drawRawGpsLines") || key.equals("forceRawGpsLines")))
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
		String gpsCol = Main.pref.get("color.gps point");
		String gpsColSpecial = Main.pref.get("color.layer "+name);
		if (!gpsColSpecial.equals(""))
			g.setColor(ColorHelper.html2color(gpsColSpecial));
		else if (!gpsCol.equals(""))
			g.setColor(ColorHelper.html2color(gpsCol));
		else
			g.setColor(Color.GRAY);
		Point old = null;
		for (Collection<GpsPoint> c : data) {
			if (!Main.pref.getBoolean("forceRawGpsLines"))
				old = null;
			for (GpsPoint p : c) {
				Point screen = mv.getPoint(p.eastNorth);
				if (Main.pref.getBoolean("drawRawGpsLines") && old != null)
					g.drawLine(old.x, old.y, screen.x, screen.y);
				else
					g.drawRect(screen.x, screen.y, 0, 0);
				old = screen;
			}
		}
	}

	@Override
	public String getToolTipText() {
		int points = 0;
		for (Collection<GpsPoint> c : data)
			points += c.size();
		return data.size()+" tracks, "+points+" points.";
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
	public void visitBoundingBox(BoundingXYVisitor v) {
		for (Collection<GpsPoint> c : data)
			for (GpsPoint p : c)
				v.visit(p.eastNorth);
	}

	@Override
	public Object getInfoComponent() {
		StringBuilder b = new StringBuilder();
		int points = 0;
		for (Collection<GpsPoint> c : data) {
			b.append("&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;a track with "+c.size()+" points<br>");
			points += c.size();
		}
		b.append("</html>");
		return "<html>"+name+" consists of "+data.size()+" tracks ("+points+" points)<br>"+b.toString();
	}
}
