// License: GPL. See LICENSE file for details.

package org.openstreetmap.josm.gui;

import static org.openstreetmap.josm.tools.I18n.marktr;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.geom.Rectangle2D;

import javax.swing.JComponent;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.HelpAction.Helpful;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.projection.Projection;
import org.openstreetmap.josm.tools.ColorHelper;

public class MapScaler extends JComponent implements Helpful {

	private final NavigatableComponent mv;
	private final Projection proj;

	public MapScaler(NavigatableComponent mv, Projection proj) {
		this.mv = mv;
		this.proj = proj;
		setSize(100,30);
		setOpaque(false);
    }

	@Override public void paint(Graphics g) {
		LatLon ll1 = mv.getLatLon(0,0);
		LatLon ll2 = mv.getLatLon(100,0);
		double dist = ll1.greatCircleDistance(ll2);
		String text = dist > 1000 ? (Math.round(dist/100)/10.0)+"km" : Math.round(dist*10)/10+"m";
		Rectangle2D bound = g.getFontMetrics().getStringBounds(text, g);
		g.setColor(Main.pref.getColor(marktr("scale"), Color.white));
		g.drawLine(0, 5, 99, 5);
		g.drawLine(0, 0, 0, 10);
		g.drawLine(99, 0, 99, 10);
		g.drawLine(49, 0, 49, 10);
		g.drawLine(24, 3, 24, 7);
		g.drawLine(74, 3, 74, 7);
		g.drawString(text, (int)(50-bound.getWidth()/2), 23);
    }

	public String helpTopic() {
	    return "MapView/Scaler";
    }
}
