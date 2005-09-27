package org.openstreetmap.josm.actions;

import static java.awt.event.MouseEvent.BUTTON1;
import static java.awt.event.MouseEvent.BUTTON3;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;

import org.openstreetmap.josm.data.GeoPoint;
import org.openstreetmap.josm.gui.MapFrame;
import org.openstreetmap.josm.gui.MapView;

/**
 * Enable the zoom mode within the MapFrame.
 * 
 * @author imi
 */
public class ZoomAction extends MapMode 
	implements MouseListener, MouseMotionListener, MouseWheelListener {

	/**
	 * Point, the mouse was when pressing the button.
	 */
	private Point mousePosStart;
	/**
	 * Point to the actual mouse position.
	 */
	private Point mousePos;
	/**
	 * The point in the map that was the under the mouse point
	 * when moving around started.
	 */
	private GeoPoint mousePosMove;
	/**
	 * Shortcut to the mapview.
	 */
	private final MapView mv;
	/**
	 * Whether currently an zooming rectangle is visible or not
	 */
	private boolean zoomRectVisible = false;

	/**
	 * For the current state.
	 */
	enum State {NOTHING, MOVE, ZOOM, ZOOM_POS};
	/**
	 * The current state, the ZoomAction is in.
	 */
	State state = State.NOTHING;
	
	
	
	/**
	 * Construct a ZoomAction without a label.
	 * @param mapFrame The MapFrame, whose zoom mode should be enabled.
	 */
	public ZoomAction(MapFrame mapFrame) {
		super("Zoom", "images/zoom.png", KeyEvent.VK_Z, mapFrame);
		mv = mapFrame.mapView;
	}

	/**
	 * Just to keep track of mouse movements.
	 */
	public void mouseMoved(MouseEvent e) {
		mousePos = e.getPoint();
	}

	/**
	 * Initializing the mouse moving state.
	 */
	public void mousePressed(MouseEvent e) {
		switch (state) {
		case NOTHING:
			switch (e.getButton()) {
			case BUTTON1:
				state = State.ZOOM;
				mousePosStart = e.getPoint();
				paintZoomRect(true);
				break;
			case BUTTON3:
				state = State.MOVE;
				mousePosMove = mv.getPoint(e.getX(), e.getY(), false);
				mv.setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
				break;
			}
			break;
		case ZOOM:
			if (e.getButton() == BUTTON3)
				state = State.ZOOM_POS;
			break;
		case MOVE:
			if (e.getButton() == BUTTON1) {
				state = State.ZOOM_POS;
				mousePosStart = e.getPoint();
				mousePos = e.getPoint();
			}
		}
	}

	public void mouseReleased(MouseEvent e) {
		switch (state) {
		case ZOOM:
			if (e.getButton() == BUTTON1) {
				state = State.NOTHING;
				paintZoomRect(false);
				Rectangle r = calculateZoomableRectangle();
				mousePosStart = null;
				// zoom to the rectangle
				if (r != null) {
					double scale = mv.getScale() * r.getWidth()/mv.getWidth();
					GeoPoint newCenter = mv.getPoint(r.x+r.width/2, r.y+r.height/2, false);
					mv.zoomTo(newCenter, scale);
				}
			}
			break;
		case MOVE:
			if (e.getButton() == BUTTON3) {
				state = State.NOTHING;
				mousePosMove = null;
				mv.setCursor(Cursor.getDefaultCursor());
			}
			break;
		case ZOOM_POS:
			switch (e.getButton()) {
			case BUTTON1:
				state = State.MOVE;
				paintZoomRect(false);
				mousePosStart = null;
				mousePos = null;
				mousePosMove = mv.getPoint(e.getX(), e.getY(), false);
				mv.setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
				break;
			case BUTTON3:
				state = State.ZOOM;
				break;
			}
		}
	}

	public void mouseDragged(MouseEvent e) {
		switch (state) {
		case MOVE:
			if (mousePosMove == null) {
				mousePosMove = mv.getCenter();
				mv.setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
			}
			GeoPoint center = mv.getCenter();
			GeoPoint mouseCenter = mv.getPoint(e.getX(), e.getY(), false);
			GeoPoint p = new GeoPoint();
			p.x = mousePosMove.x + center.x - mouseCenter.x;  
			p.y = mousePosMove.y + center.y - mouseCenter.y;  
			mv.zoomTo(p, mv.getScale());
			break;
		case ZOOM:
			if (mousePosStart == null)
				mousePosStart = e.getPoint();
			if (mousePos == null)
				mousePos = e.getPoint();
			paintZoomRect(false);
			mousePos = e.getPoint();
			paintZoomRect(true);
			break;
		case ZOOM_POS:
			if (mousePosStart == null)
				mousePosStart = e.getPoint();
			if (mousePos == null)
				mousePos = e.getPoint();
			paintZoomRect(false);
			mousePosStart.x += e.getX()-mousePos.x;
			mousePosStart.y += e.getY()-mousePos.y;
			mousePos = e.getPoint();
			paintZoomRect(true);
			break;
		}
	}

	public void mouseWheelMoved(MouseWheelEvent e) {
		boolean zoomRect = zoomRectVisible;
		paintZoomRect(false);
		double zoom = Math.max(0.1, 1 + e.getWheelRotation()/5.0);
		mv.zoomTo(mv.getCenter(), mv.getScale()*zoom);
		paintZoomRect(zoomRect);
	}

	/**
	 * Calculate the zoomable rectangle between mousePos and mousePosStart.
	 * Zoomable is the rectangle with a fix point at mousePosStart and the
	 * correct aspect ratio that fits into the current mapView's size.
	 * @return Rectangle which should be used to zoom.
	 */
	private Rectangle calculateZoomableRectangle() {
		if (mousePosStart == null || mousePos == null || mousePosStart == mousePos)
			return null;
		Rectangle r = new Rectangle();
		r.x = mousePosStart.x;
		r.y = mousePosStart.y;
		r.width = mousePos.x - mousePosStart.x;
		r.height = mousePos.y - mousePosStart.y;
		if (r.width < 0) {
			r.x += r.width;
			r.width = -r.width;
		}
		if (r.height < 0) {
			r.y += r.height;
			r.height = -r.height;
		}
		
		// keep the aspect ration by shrinking the rectangle
		double aspectRatio = (double)mv.getWidth()/mv.getHeight();
		if ((double)r.width/r.height > aspectRatio)
			r.width = (int)(r.height*aspectRatio);
		else
			r.height = (int)(r.width/aspectRatio);

		return r;
	}
	
	/**
	 * Paint the mouse selection rectangle XOR'ed over the display.
	 *
	 * @param drawVisible True, to draw the rectangle or false to erase it.
	 */
	private void paintZoomRect(boolean drawVisible) {
		Rectangle r = calculateZoomableRectangle();
		if (r != null && drawVisible != zoomRectVisible) {
			Graphics g = mv.getGraphics();
			g.setColor(Color.BLACK);
			g.setXORMode(Color.WHITE);
			g.drawRect(r.x,r.y,r.width,r.height);
			zoomRectVisible = !zoomRectVisible;
		}
	}

	public void registerListener(MapView mapView) {
		mapView.addMouseListener(this);
		mapView.addMouseMotionListener(this);
		mapView.addMouseWheelListener(this);
	}

	public void unregisterListener(MapView mapView) {
		mapView.removeMouseListener(this);
		mapView.removeMouseMotionListener(this);
		mapView.removeMouseWheelListener(this);
	}
	
	/**
	 * Does nothing. Only to satisfy MouseListener
	 */
	public void mouseClicked(MouseEvent e) {}
	/**
	 * Does nothing. Only to satisfy MouseListener
	 */
	public void mouseEntered(MouseEvent e) {}
	/**
	 * Does nothing. Only to satisfy MouseListener
	 */
	public void mouseExited(MouseEvent e) {}
}
