package org.openstreetmap.josm.gui;

import java.awt.Cursor;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;

import org.openstreetmap.josm.data.GeoPoint;

/**
 * Enables moving of the map by holding down the right mouse button and drag
 * the mouse. Also, enables zooming by the mouse wheel.
 *
 * @author imi
 */
class MapMover extends MouseAdapter implements MouseMotionListener, MouseWheelListener {

	/**
	 * The point in the map that was the under the mouse point
	 * when moving around started.
	 */
	private GeoPoint mousePosMove;
	/**
	 * The map to move around.
	 */
	private final NavigatableComponent nc;
	/**
	 * The old cursor when we changed it to movement cursor.
	 */
	private Cursor oldCursor;

	
	/**
	 * Create a new MapMover
	 */
	MapMover(NavigatableComponent navComp) {
		this.nc = navComp;
		nc.addMouseListener(this);
		nc.addMouseMotionListener(this);
		nc.addMouseWheelListener(this);
	}
	
	/**
	 * If the right (and only the right) mouse button is pressed, move the map
	 */
	public void mouseDragged(MouseEvent e) {
		int offMask = MouseEvent.BUTTON1_DOWN_MASK | MouseEvent.BUTTON2_DOWN_MASK;
		if ((e.getModifiersEx() & (MouseEvent.BUTTON3_DOWN_MASK | offMask)) == MouseEvent.BUTTON3_DOWN_MASK) {
			if (mousePosMove == null)
				startMovement(e);
			GeoPoint center = nc.getCenter();
			GeoPoint mouseCenter = nc.getPoint(e.getX(), e.getY(), false);
			GeoPoint p = new GeoPoint();
			p.x = mousePosMove.x + center.x - mouseCenter.x;  
			p.y = mousePosMove.y + center.y - mouseCenter.y;
			nc.zoomTo(p, nc.getScale());
		} else
			endMovement();
	}

	/**
	 * Start the movement, if it was the 3rd button (right button).
	 */
	@Override
	public void mousePressed(MouseEvent e) {
		int offMask = MouseEvent.BUTTON1_DOWN_MASK | MouseEvent.BUTTON2_DOWN_MASK;
		if (e.getButton() == MouseEvent.BUTTON3 && (e.getModifiersEx() & offMask) == 0)
			startMovement(e);
	}

	/**
	 * Change the cursor back to it's pre-move cursor.
	 */
	@Override
	public void mouseReleased(MouseEvent e) {
		if (e.getButton() == MouseEvent.BUTTON3)
			endMovement();
	}

	/**
	 * Start movement by setting a new cursor and remember the current mouse
	 * position.
	 * @param e The mouse event that leat to the movement start.
	 */
	private void startMovement(MouseEvent e) {
		mousePosMove = nc.getPoint(e.getX(), e.getY(), false);
		oldCursor = nc.getCursor();
		nc.setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
	}
	
	/**
	 * End the movement. Setting back the cursor and clear the movement variables
	 */
	private void endMovement() {
		if (oldCursor != null)
			nc.setCursor(oldCursor);
		else
			nc.setCursor(Cursor.getDefaultCursor());
		mousePosMove = null;
		oldCursor = null;
	}

	/**
	 * Zoom the map by 1/5th of current zoom per wheel-delta.
	 * @param e The wheel event.
	 */
	public void mouseWheelMoved(MouseWheelEvent e) {
		double zoom = Math.max(0.1, 1 + e.getWheelRotation()/5.0);
		double zoomfactor = (zoom -1)/2+1;
		int newHalfWidth = (int) (nc.getWidth()*zoomfactor - nc.getWidth()/2);
		int centerx = e.getX() - (e.getX()-nc.getWidth()/2)*newHalfWidth*2/nc.getWidth();
		int newHalfHeight = (int) (nc.getHeight()*zoomfactor - nc.getHeight()/2);
		int centery = e.getY() - (e.getY()-nc.getHeight()/2)*newHalfHeight*2/nc.getHeight();
		GeoPoint newCenter = nc.getPoint(centerx, centery, false); 
		nc.zoomTo(newCenter, nc.getScale()*zoom);
	}

	/**
	 * Does nothing. Only to satisfy MouseMotionListener
	 */
	public void mouseMoved(MouseEvent e) {}
}
