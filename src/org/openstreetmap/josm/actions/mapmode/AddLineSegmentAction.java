package org.openstreetmap.josm.actions.mapmode;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.command.AddCommand;
import org.openstreetmap.josm.data.osm.LineSegment;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.gui.MapFrame;

/**
 * The user can add a new line segment between two nodes by pressing on the 
 * starting node and dragging to the ending node. 
 * 
 * No line segment can be created if there is already a line segment containing
 * both nodes.
 * 
 * @author imi
 */
public class AddLineSegmentAction extends MapMode implements MouseListener {

	/**
	 * The first node the user pressed the button onto.
	 */
	private Node first;
	/**
	 * The second node used if the user releases the button.
	 */
	private Node second;

	/**
	 * Whether the hint is currently drawn on screen.
	 */
	private boolean hintDrawn = false;
	
	/**
	 * Create a new AddLineSegmentAction.
	 * @param mapFrame The MapFrame this action belongs to.
	 */
	public AddLineSegmentAction(MapFrame mapFrame) {
		super("Add Line Segment", "addlinesegment", "Add a line segment between two nodes.", KeyEvent.VK_G, mapFrame);
	}

	@Override
	public void registerListener() {
		super.registerListener();
		mv.addMouseListener(this);
		mv.addMouseMotionListener(this);
	}

	@Override
	public void unregisterListener() {
		super.unregisterListener();
		mv.removeMouseListener(this);
		mv.removeMouseMotionListener(this);
		drawHint(false);
	}

	
	@Override
	public void actionPerformed(ActionEvent e) {
		super.actionPerformed(e);
		makeLineSegment();
	}

	/**
	 * If user clicked on a node, start the dragging with that node. 
	 */
	@Override
	public void mousePressed(MouseEvent e) {
		if (e.getButton() != MouseEvent.BUTTON1)
			return;

		OsmPrimitive clicked = mv.getNearest(e.getPoint(), false);
		if (clicked == null || !(clicked instanceof Node))
			return;

		drawHint(false);
		first = second = (Node)clicked;
	}

	/**
	 * Draw a hint which nodes will get connected if the user release
	 * the mouse button now.
	 */
	@Override
	public void mouseDragged(MouseEvent e) {
		if ((e.getModifiersEx() & MouseEvent.BUTTON1_DOWN_MASK) == 0)
			return;

		OsmPrimitive clicked = mv.getNearest(e.getPoint(), (e.getModifiersEx() & MouseEvent.ALT_DOWN_MASK) != 0);
		if (clicked == null || clicked == second || !(clicked instanceof Node))
			return;

		drawHint(false);

		second = (Node)clicked;
		drawHint(true);
	}

	/**
	 * If left button was released, try to create the line segment.
	 */
	@Override
	public void mouseReleased(MouseEvent e) {
		if (e.getButton() == MouseEvent.BUTTON1) {
			makeLineSegment();
			first = null; // release line segment drawing
		}
	}

	/**
	 * Create the line segment if first and second are different and there is
	 * not already a line segment.
	 */
	private void makeLineSegment() {
		if (first == null || second == null) {
			first = null;
			second = null;
			return;
		}

		drawHint(false);
		
		Node start = first;
		Node end = second;
		first = second;
		second = null;
		
		if (start != end) {
			// try to find a line segment
			for (LineSegment ls : Main.main.ds.lineSegments)
				if ((start == ls.start && end == ls.end) || (end == ls.start && start == ls.end))
					return; // already a line segment here - be happy, do nothing.

			LineSegment ls = new LineSegment(start, end);
			mv.editLayer().add(new AddCommand(Main.main.ds, ls));
		}

		mv.repaint();
	}

	/**
	 * Draw or remove the hint line, depending on the parameter.
	 */
	private void drawHint(boolean draw) {
		if (draw == hintDrawn)
			return;
		if (first == null || second == null)
			return;
		if (second == first)
			return;

		Graphics g = mv.getGraphics();
		g.setColor(Color.BLACK);
		g.setXORMode(Color.WHITE);
		Point firstDrawn = mv.getScreenPoint(first.coor);
		Point secondDrawn = mv.getScreenPoint(second.coor);
		g.drawLine(firstDrawn.x, firstDrawn.y, secondDrawn.x, secondDrawn.y);
		hintDrawn = !hintDrawn;
	}
}
