package org.openstreetmap.josm.actions.mapmode;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;

import javax.swing.JLabel;

import org.openstreetmap.josm.data.osm.LineSegment;
import org.openstreetmap.josm.data.osm.Track;
import org.openstreetmap.josm.gui.MapFrame;

/**
 * To debug zone information
 * 
 * @author imi
 */
public class DebugAction extends MapMode {

	private JLabel label = new JLabel();

	public DebugAction(MapFrame mapFrame) {
		super("Debug Zones", "debug", "Debug only. Just ignore.", KeyEvent.VK_D, mapFrame);
	}
	
	@Override
	public void registerListener() {
		super.registerListener();
		layer.addMouseMotionListener(this);
		layer.addMouseListener(this);
		mapFrame.add(label, BorderLayout.SOUTH);
	}

	@Override
	public void unregisterListener() {
		super.unregisterListener();
		layer.removeMouseMotionListener(this);
		layer.removeMouseListener(this);
		mapFrame.remove(label);
	}

	@Override
	public void mouseClicked(MouseEvent e) {
		Graphics g = mapFrame.layer.getGraphics();
		g.setColor(Color.WHITE);
		for (Track t :mapFrame.layer.dataSet.tracks())
			for (LineSegment ls : t.segments()) {
				Point A = mapFrame.layer.getScreenPoint(ls.getStart().coor);
				Point B = mapFrame.layer.getScreenPoint(ls.getEnd().coor);
				Point C = e.getPoint();
				Rectangle r = new Rectangle(A.x, A.y, B.x-A.x, B.y-A.y);
				double dist = perpendicularDistSq(B.distanceSq(C), A.distanceSq(C), A.distanceSq(B));
				g.drawString(""+dist, (int)r.getCenterX(), (int)r.getCenterY());
			}
	}

	private double perpendicularDistSq(double a, double b, double c) {
		return a-(a-b+c)*(a-b+c)/4/c;
	}
}
