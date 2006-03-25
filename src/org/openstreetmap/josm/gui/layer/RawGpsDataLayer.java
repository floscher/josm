package org.openstreetmap.josm.gui.layer;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Collection;
import java.util.LinkedList;

import javax.swing.Icon;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.osm.visitor.BoundingXYVisitor;
import org.openstreetmap.josm.data.projection.Projection;
import org.openstreetmap.josm.gui.MapView;
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
		for (Collection<EastNorth> c : eastNorth) {
			if (!Main.pref.isForceRawGpsLines())
				old = null;
			for (EastNorth eastNorth : c) {
				Point screen = mv.getPoint(eastNorth);
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

	@Override
	public void init(Projection projection) {
		eastNorth = new LinkedList<Collection<EastNorth>>();
		for (Collection<LatLon> c : data) {
			Collection<EastNorth> eastNorthList = new LinkedList<EastNorth>();
			for (LatLon ll : c)
				eastNorthList.add(Main.pref.getProjection().latlon2eastNorth(ll));
			this.eastNorth.add(eastNorthList);
		}
	}
}
