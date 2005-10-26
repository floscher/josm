package org.openstreetmap.josm.actions.mapmode;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;

import javax.swing.JOptionPane;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.command.CombineCommand;
import org.openstreetmap.josm.data.osm.LineSegment;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Track;
import org.openstreetmap.josm.gui.MapFrame;

/**
 * A MapMode that allows the user to combine two objects to a new one.
 * 
 * When entering CombineAction, all selection is cleared.
 * 
 * The user can select objects by dragging them to another object, so the object 
 * he pressed and the one he released the button are combined. No selection 
 * rectangle is supported. 
 * 
 * Even if the user don't press Alt, tracks instead of line segments are selected. 
 * This means, it is impossible to select non-pending line segments.
 * 
 * Pressing Ctrl or Shift has no effect too.
 *
 * No object can be combined with an object it is already part of. E.g. line
 * segment cannot be combined with a track it is part of. In case of such a 
 * constillation, the user is informed.
 *
 * When combining, the object the user pressed on is called <i>source</i> and 
 * the object the button was released on is called <i>target</i>.
 *
 * The following objects can be combined:
 *
 * - A line segment and a track can be combined if one of them is a pending line
 *   segment. This get integrated into the track.
 * - Two tracks can be combined. The latter track get removed and all its 
 *   segments are moved to the first track. This is only possible, if both 
 *   tracks have no different value in any key.
 * - Two areas can be combined, if they share at least one node, in which case
 *   the combined area span both areas. If the areas share more than one node,
 *   all lines between the areas get removed. This is only possible if both areas
 *   have no different value in any key.
 *
 * All other object combinations cannot be combined.
 * 
 * TODO: This and AddLineSegmentAction are similar. Refactor both.
 * 
 * @author imi
 */
public class CombineAction extends MapMode {

	/**
	 * The object that was first selected as combine source. 
	 */
	private OsmPrimitive first;
	/**
	 * The object that was last selected as combine target. 
	 */
	private OsmPrimitive second;
	/**
	 * Whether a hint is drawn on screen or not.
	 */
	private boolean combineHintDrawn = false;

	/**
	 * Constructs a CombineAction. Mnemonic is "c".
	 */
	public CombineAction(MapFrame mapFrame) {
		super("Combine", "combine", "Combine objects together.", KeyEvent.VK_C, mapFrame);
	}

	@Override
	public void registerListener() {
		super.registerListener();
		mv.addMouseListener(this);
		mv.addMouseMotionListener(this);
		Main.main.ds.clearSelection();
	}

	@Override
	public void unregisterListener() {
		super.unregisterListener();
		mv.removeMouseListener(this);
		mv.removeMouseMotionListener(this);
		drawCombineHint(false);
	}

	/**
	 * If nothing is selected, select the object nearest to the mouse. Else
	 * start the "display possible combining" phase and draw a hint what would
	 * be combined if user releases the button. 
	 */
	@Override
	public void mousePressed(MouseEvent e) {
		if (e.getButton() != MouseEvent.BUTTON1)
			return;

		OsmPrimitive clicked = mv.getNearest(e.getPoint(), true);
		if (clicked == null || clicked instanceof Node)
			return;

		drawCombineHint(false);
		first = second = clicked;
	}

	/**
	 * Updates the drawn combine hint if necessary.
	 */
	@Override
	public void mouseDragged(MouseEvent e) {
		if ((e.getModifiersEx() & MouseEvent.BUTTON1_DOWN_MASK) == 0)
			return;
		
		OsmPrimitive clicked = mv.getNearest(e.getPoint(), true);
		if (clicked == null || clicked == second || clicked instanceof Node)
			return;

		drawCombineHint(false);
		second = clicked;
		drawCombineHint(true);
	}
	
	/**
	 * Start combining (if there is something to combine).
	 */
	@Override
	public void mouseReleased(MouseEvent e) {
		if (e.getButton() != MouseEvent.BUTTON1)
			return;

		if (first == null || second == null || first == second) {
			first = null;
			second = null;
			return;
		}

		drawCombineHint(false);
		
		if (first instanceof LineSegment && second instanceof LineSegment)
			JOptionPane.showMessageDialog(Main.main, "Cannot combine two line segments. To create tracks use 'Add Track'.");
		else if (first instanceof Track && second instanceof Track && !first.keyPropertiesMergable(second))
			JOptionPane.showMessageDialog(Main.main, "Cannot combine because of different properties.");
		else
			mv.editLayer().add(new CombineCommand(first, second));
		mv.repaint();
	}

	/**
	 * Draws or removes the combine hint using the combineHint structure.
	 *
	 * @param draw 	<code>true</code> to draw the hint or 
	 * 				<code>false</code> to remove it.
	 */
	private void drawCombineHint(boolean draw) {
		if (draw == combineHintDrawn)
			return;
		if (first == null || second == null)
			return;
		if (second == first)
			return;

		Graphics g = mv.getGraphics();
		g.setColor(Color.BLACK);
		g.setXORMode(Color.WHITE);
		draw(g, first);
		draw(g, second);
		combineHintDrawn = !combineHintDrawn;
	}

	/**
	 * Draw a hint for the specified primitive
	 * @param g The graphic to draw into
	 * @param osm The primitive to draw a hint for.
	 */
	private void draw(Graphics g, OsmPrimitive osm) {
		if (osm instanceof LineSegment) {
			LineSegment ls = (LineSegment)osm;
			Point start = mv.getScreenPoint(ls.start.coor);
			Point end = mv.getScreenPoint(ls.end.coor);
			if (Main.main.ds.pendingLineSegments.contains(osm) && g.getColor() == Color.GRAY)
				g.drawLine(start.x, start.y, end.x, end.y);
			else
				g.drawLine(start.x, start.y, end.x, end.y);
		} else if (osm instanceof Track) {
			for (LineSegment ls : ((Track)osm).segments)
				draw(g, ls);
		}
	}
}
