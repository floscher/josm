package org.openstreetmap.josm.actions;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

import javax.swing.JLabel;

import org.openstreetmap.josm.data.GeoPoint;
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
		super("Debug Zones", "images/debug.png", KeyEvent.VK_D, mapFrame);
	}
	
	public void registerListener(MapView mapView) {
		mapView.addMouseMotionListener(this);
		mapView.addMouseListener(this);
		mapFrame.add(label, BorderLayout.SOUTH);
	}

	public void unregisterListener(MapView mapView) {
		mapView.removeMouseMotionListener(this);
		mapView.removeMouseListener(this);
		mapFrame.remove(label);
	}

	public void mouseDragged(MouseEvent e) {
	}

	public void mouseMoved(MouseEvent e) {
		double lon = ((double)e.getX()/mapFrame.mapView.getWidth()*360) - 180;
		double lat = 90 - (double)e.getY()/mapFrame.mapView.getHeight() * 180;
		GeoPoint p = new GeoPoint(lat, lon);
		mapFrame.mapView.getProjection().latlon2xy(p);
		label.setText("x="+e.getX()+" y="+e.getY()+" lat="+p.lat+" lon="+p.lon+" N="+p.y+" E="+p.x);
		
		GeoPoint mousePoint = mapFrame.mapView.getPoint(e.getX(), e.getY(), false);
		GeoPoint center = mapFrame.mapView.getCenter();
		double scale = mapFrame.mapView.getScale();
		int xscr = (int)Math.round((mousePoint.x-center.x) / scale + mapFrame.mapView.getWidth()/2);
		int yscr = (int)Math.round((center.y-mousePoint.y) / scale + mapFrame.mapView.getHeight()/2);
		Graphics g = mapFrame.mapView.getGraphics();
		g.setColor(Color.CYAN);
		g.drawArc(xscr, yscr, 4,4,0,360);
	}

	public void mouseClicked(MouseEvent e) {
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
