package org.openstreetmap.josm.actions.mapmode;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

import javax.swing.JLabel;

import org.openstreetmap.josm.data.osm.LineSegment;
import org.openstreetmap.josm.data.osm.Track;
import org.openstreetmap.josm.gui.MapFrame;
import org.openstreetmap.josm.gui.MapView;

/**
 * To debug zone information
 * 
 * @author imi
 */
public class DebugAction extends MapMode implements MouseMotionListener, MouseListener {

	private JLabel label = new JLabel();

	public DebugAction(MapFrame mapFrame) {
		super("Debug Zones", "debug", "Debug only. Just ignore.", KeyEvent.VK_D, mapFrame);
	}
	
	@Override
	public void registerListener(MapView mapView) {
		mapView.addMouseMotionListener(this);
		mapView.addMouseListener(this);
		mapFrame.add(label, BorderLayout.SOUTH);
	}

	@Override
	public void unregisterListener(MapView mapView) {
		mapView.removeMouseMotionListener(this);
		mapView.removeMouseListener(this);
		mapFrame.remove(label);
	}

	public void mouseDragged(MouseEvent e) {
	}

	public void mouseMoved(MouseEvent e) {
	}

	public void mouseClicked(MouseEvent e) {
		Graphics g = mapFrame.mapView.getGraphics();
		g.setColor(Color.WHITE);
		for (Track t :mapFrame.mapView.dataSet.tracks)
			for (LineSegment ls : t.segments) {
				Point A = mapFrame.mapView.getScreenPoint(ls.start.coor);
				Point B = mapFrame.mapView.getScreenPoint(ls.end.coor);
				Point C = e.getPoint();
				Rectangle r = new Rectangle(A.x, A.y, B.x-A.x, B.y-A.y);
				double dist = perpendicularDistSq(B.distanceSq(C), A.distanceSq(C), A.distanceSq(B));
				g.drawString(""+dist, (int)r.getCenterX(), (int)r.getCenterY());
			}
	}

	private double perpendicularDistSq(double a, double b, double c) {
		// I did this on paper by myself, so I am surprised too, that it is that 
		// performant ;-) 
		return a-(a-b+c)*(a-b+c)/4/c;
	}

	public void mousePressed(MouseEvent e) {
	}

	public void mouseReleased(MouseEvent e) {
	}

	public void mouseEntered(MouseEvent e) {
	}

	public void mouseExited(MouseEvent e) {
	}

}
