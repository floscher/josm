package org.openstreetmap.josm.gui;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.geom.Rectangle2D;

import javax.swing.JComponent;

public class MapScaler extends JComponent {

	private final MapView mv;

	public MapScaler(MapView mv) {
		this.mv = mv;
		setSize(100,30);
		setOpaque(false);
    }

	@Override public void paint(Graphics g) {
		double circum = mv.getScale()*100/Math.PI/2*40041455; // circumference of the earth in meter
		String text = circum > 1000 ? (Math.round(circum/100)/10.0)+"km" : Math.round(circum)+"m";
		g.setColor(Color.white);
		g.drawLine(0, 5, 99, 5);
		g.drawLine(0, 0, 0, 10);
		g.drawLine(99, 0, 99, 10);
		Rectangle2D bound = g.getFontMetrics().getStringBounds(text, g);
		g.drawString(text, (int)(50-bound.getWidth()/2), 23);
    }
}
