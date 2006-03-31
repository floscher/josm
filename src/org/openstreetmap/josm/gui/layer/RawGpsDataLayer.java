package org.openstreetmap.josm.gui.layer;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.util.Collection;
import java.util.LinkedList;

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
	
	/**
	 * A list of ways which containing a list of points.
	 */
	private final Collection<Collection<LatLon>> data;
	private Collection<Collection<EastNorth>> eastNorth;

	public RawGpsDataLayer(Collection<Collection<LatLon>> data, String name) {
		super(name);
		this.data = data;

		eastNorth = new LinkedList<Collection<EastNorth>>();
		for (Collection<LatLon> c : data) {
			Collection<EastNorth> eastNorthList = new LinkedList<EastNorth>();
			for (LatLon ll : c)
				eastNorthList.add(Main.proj.latlon2eastNorth(ll));
			this.eastNorth.add(eastNorthList);
		}

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
		for (Collection<EastNorth> c : eastNorth) {
			if (!Main.pref.getBoolean("forceRawGpsLines"))
				old = null;
			for (EastNorth eastNorth : c) {
				Point screen = mv.getPoint(eastNorth);
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
		for (Collection<LatLon> c : data)
			points += c.size();
		return data.size()+" ways, "+points+" points.";
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
		for (Collection<EastNorth> c : eastNorth)
			for (EastNorth eastNorth : c)
				v.visit(eastNorth);
	}
}
