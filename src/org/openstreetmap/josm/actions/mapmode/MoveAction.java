package org.openstreetmap.josm.actions.mapmode;

import java.awt.Cursor;
import java.awt.Point;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.Collection;
import java.util.HashSet;

import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.gui.MapFrame;

/**
 * Move is an action that can move all kind of OsmPrimitives (except Keys for now).
 * 
 * If any object is selected, all selected objects are moved. If no object is 
 * selected, the nearest object will be selected and moved. In this case, the
 * object will be unselected as soon as movement stopped.
 * 
 * @author imi
 */
public class MoveAction extends MapMode {

	/**
	 * The old cursor before the user pressed the mouse button.
	 */
	private Cursor oldCursor;
	/**
	 * The position of the mouse before the user moves a node.
	 */
	private Point mousePos;
	/**
	 * Non-<code>null</code>, if no object was selected before movement 
	 * (and so the object get unselected after mouse release).
	 */
	private OsmPrimitive singleOsmPrimitive;

	/**
	 * Create a new MoveAction
	 * @param mapFrame The MapFrame, this action belongs to.
	 */
	public MoveAction(MapFrame mapFrame) {
		super("Move", "move", "Move selected objects around", KeyEvent.VK_M, mapFrame);
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
	}

	
	/**
	 * If the left mouse button is pressed, move all currently selected
	 * objects.
	 */
	@Override
	public void mouseDragged(MouseEvent e) {
		if ((e.getModifiersEx() & MouseEvent.BUTTON1_DOWN_MASK) == 0)
			return;
		
		if (mousePos == null) {
			mousePos = e.getPoint();
			singleOsmPrimitive = null;
		}

		int dx = e.getX() - mousePos.x;
		int dy = e.getY() - mousePos.y;
		if (dx == 0 && dy == 0)
			return;

		Collection<OsmPrimitive> selection = ds.getSelected();
		// creating a list of all nodes that should be moved.
		Collection<Node> movingNodes = new HashSet<Node>();
		for (OsmPrimitive osm : selection)
			movingNodes.addAll(osm.getAllNodes());

		for (Node n : movingNodes) {
			Point pos = mv.getScreenPoint(n.coor);
			pos.x += dx;
			pos.y += dy;
			n.coor = mv.getPoint(pos.x, pos.y, true);
		}
		mv.repaint();
		
		mousePos = e.getPoint();
	}

	/**
	 * Look, whether any object is selected. If not, select the nearest node.
	 * If there are no nodes in the dataset, do nothing.
	 * 
	 * If the user did not press the left mouse button, do nothing.
	 * 
	 * Also remember the starting position of the movement and change the mouse 
	 * cursor to movement.
	 */
	@Override
	public void mousePressed(MouseEvent e) {
		if (e.getButton() != MouseEvent.BUTTON1)
			return;

		if (ds.getSelected().size() == 0) {
			OsmPrimitive osm = mv.getNearest(e.getPoint(), (e.getModifiersEx() & MouseEvent.ALT_DOWN_MASK) != 0);
			if (osm != null)
				osm.selected = true;
			singleOsmPrimitive = osm;
			mv.repaint();
		} else
			singleOsmPrimitive = null;
		
		mousePos = e.getPoint();
		oldCursor = mv.getCursor();
		mv.setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
	}
	
	/**
	 * Restore the old mouse cursor.
	 */
	@Override
	public void mouseReleased(MouseEvent e) {
		mv.setCursor(oldCursor);
		if (singleOsmPrimitive != null) {
			singleOsmPrimitive.selected = false;
			mv.repaint();
		}
	}
}
