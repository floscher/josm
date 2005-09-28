package org.openstreetmap.josm.actions;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

/**
 * Manages the selection of a rectangle. Listening to left and right mouse button
 * presses and to mouse motions and draw the rectangle accordingly.
 * 
 * Left mouse button selects a rectangle from the press until release. Pressing
 * right mouse button while left is still pressed enable the rectangle to move
 * around. Releasing the left button fires an action event to the listener given
 * at constructor, except if the right is still pressed, which just remove the
 * selection rectangle and does nothing.
 * 
 * The point where the left mouse button was pressed and the current mouse 
 * position are two opposite corners of the selection rectangle.
 * 
 * It is possible to specify an aspect ratio (width per height) which the 
 * selection rectangle always must have. In this case, the selection rectangle
 * will be the largest window with this aspect ratio, where the position the left
 * mouse button was pressed and the corner of the current mouse position are at 
 * opposite sites (the mouse position corner is the corner nearest to the mouse
 * cursor). 
 * 
 * When the left mouse button was released, an ActionEvent is send to the 
 * ActionListener given at constructor. The source of this event is this manager.
 * 
 * @author imi
 */
public class SelectionManager implements MouseListener, MouseMotionListener {

	/**
	 * This is the interface that an user of SelectionManager has to implement
	 * to get informed when a selection closes.
	 * @author imi
	 */
	public interface SelectionEnded {
		/**
		 * Called, when the left mouse button was released.
		 * @param r The rectangle, that is currently the selection.
		 * @param modifiers The modifiers returned from the MouseEvent when
		 * 		the left mouse button was released. 
		 * @see InputEvent#getModifiersEx()
		 */
		public void selectionEnded(Rectangle r, int modifiers);
	}
	/**
	 * The listener that receives the events after left mouse button is released.
	 */
	private final SelectionEnded selectionEndedListener;
	/**
	 * Position of the map when the mouse button was pressed.
	 * If this is not <code>null</code>, a rectangle is drawn on screen. 
	 */
	private Point mousePosStart;
	/**
	 * Position of the map when the selection rectangle was last drawn.
	 */
	private Point mousePos;
	/**
	 * The component, the selection rectangle is drawn onto.
	 */
	private final Component drawComponent;
	/**
	 * Whether the selection rectangle must obtain the aspect ratio of the 
	 * drawComponent.
	 */
	private boolean aspectRatio;

	/**
	 * Create a new SelectionManager.
	 *
	 * @param actionListener The action listener that receives the event when
	 * 		the left button is released.
	 * @param aspectRatio If true, the selection window must obtain the aspect
	 * 		ratio of the drawComponent.
	 * @param drawComponent The component, the rectangle is drawn onto.
	 */
	public SelectionManager(SelectionEnded selectionEndedListener, boolean aspectRatio, Component drawComponent) {
		this.selectionEndedListener = selectionEndedListener;
		this.aspectRatio = aspectRatio;
		this.drawComponent = drawComponent;
	}
	
	/**
	 * Register itself at the given event source.
	 * @param eventSource The emitter of the mouse events.
	 */
	public void register(Component eventSource) {
		eventSource.addMouseListener(this);
		eventSource.addMouseMotionListener(this);
	}
	/**
	 * Unregister itself from the given event source. If a selection rectangle is
	 * shown, hide it first.
	 *
	 * @param eventSource The emitter of the mouse events.
	 */
	public void unregister(Component eventSource) {
		eventSource.removeMouseListener(this);
		eventSource.removeMouseMotionListener(this);
	}

	/**
	 * If the correct button, start the "drawing rectangle" mode
	 */
	public void mousePressed(MouseEvent e) {
		if (e.getButton() == MouseEvent.BUTTON1) {
			mousePosStart = e.getPoint();
			mousePos = e.getPoint();
			paintRect();
		}
	}

	/**
	 * If the correct button is hold, draw the rectangle.
	 */
	public void mouseDragged(MouseEvent e) {
		int buttonPressed = e.getModifiersEx() & (MouseEvent.BUTTON1_DOWN_MASK | MouseEvent.BUTTON3_DOWN_MASK); 

		if (buttonPressed != 0) {
			if (mousePosStart == null) {
				mousePosStart = e.getPoint();
				mousePos = e.getPoint();
				paintRect();
			}
			paintRect();
		}
		
		if (buttonPressed == MouseEvent.BUTTON1_DOWN_MASK) {
			mousePos = e.getPoint();
			paintRect();
		} else if (buttonPressed == (MouseEvent.BUTTON1_DOWN_MASK | MouseEvent.BUTTON3_DOWN_MASK)) {
			mousePosStart.x += e.getX()-mousePos.x;
			mousePosStart.y += e.getY()-mousePos.y;
			mousePos = e.getPoint();
			paintRect();
		}
	}

	/**
	 * Check the state of the keys and buttons and set the selection accordingly.
	 */
	public void mouseReleased(MouseEvent e) {
		if (e.getButton() != MouseEvent.BUTTON1)
			return;
		// disable the selection rect
		paintRect();
		Rectangle r = getSelectionRectangle();
		mousePosStart = null;
		mousePos = null;
		if ((e.getModifiersEx() & MouseEvent.BUTTON3_DOWN_MASK) == 0)
			selectionEndedListener.selectionEnded(r, e.getModifiersEx());
	}


	/**
	 * Draw a selection rectangle on screen. If already a rectangle is drawn,
	 * it is removed instead.
	 */
	private void paintRect() {
		Graphics g = drawComponent.getGraphics();
		g.setColor(Color.BLACK);
		g.setXORMode(Color.WHITE);

		Rectangle r = getSelectionRectangle();
		g.drawRect(r.x,r.y,r.width,r.height);
	}

	/**
	 * Calculate and return the current selection rectangle
	 * @return A rectangle that spans from mousePos to mouseStartPos
	 */
	private Rectangle getSelectionRectangle() {
		int x = mousePosStart.x;
		int y = mousePosStart.y;
		int w = mousePos.x - mousePosStart.x;
		int h = mousePos.y - mousePosStart.y;
		if (w < 0) {
			x += w;
			w = -w;
		}
		if (h < 0) {
			y += h;
			h = -h;
		}
		
		if (aspectRatio) {
			// keep the aspect ration by shrinking the rectangle
			double aspectRatio = (double)drawComponent.getWidth()/drawComponent.getHeight();
			if ((double)w/h > aspectRatio) {
				int neww = (int)(h*aspectRatio);
				if (mousePos.x < mousePosStart.x)
					x += w-neww;
				w = neww;
			} else {
				int newh = (int)(w/aspectRatio);
				if (mousePos.y < mousePosStart.y)
					y += h-newh;
				h = newh;
			}
		}
		
		return new Rectangle(x,y,w,h);
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
	/**
	 * Does nothing. Only to satisfy MouseMotionListener
	 */
	public void mouseMoved(MouseEvent e) {}
}
