package org.openstreetmap.josm.actions.mapmode;

import java.awt.Cursor;
import java.awt.Point;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.Collection;

import javax.swing.JOptionPane;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.command.MoveCommand;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.visitor.AllNodesVisitor;
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
		super("Move", "move", "Move selected objects around.", "M", KeyEvent.VK_M, mapFrame);
	}

	@Override public void enterMode() {
		super.enterMode();
		Main.map.mapView.addMouseListener(this);
		Main.map.mapView.addMouseMotionListener(this);
	}

	@Override public void exitMode() {
		super.exitMode();
		Main.map.mapView.removeMouseListener(this);
		Main.map.mapView.removeMouseMotionListener(this);
	}

	
	/**
	 * If the left mouse button is pressed, move all currently selected
	 * objects.
	 */
	@Override public void mouseDragged(MouseEvent e) {
		if ((e.getModifiersEx() & MouseEvent.BUTTON1_DOWN_MASK) == 0)
			return;
		
		if (mousePos == null) {
			mousePos = e.getPoint();
			singleOsmPrimitive = null;
		}

		EastNorth mouseGeo = Main.map.mapView.getEastNorth(e.getX(), e.getY());
		EastNorth mouseStartGeo = Main.map.mapView.getEastNorth(mousePos.x, mousePos.y);
		double dx = mouseGeo.east() - mouseStartGeo.east();
		double dy = mouseGeo.north() - mouseStartGeo.north();
		if (dx == 0 && dy == 0)
			return;

		Collection<OsmPrimitive> selection = Main.ds.getSelected();
		Collection<Node> affectedNodes = AllNodesVisitor.getAllNodes(selection);
		
		// check if any coordinate would be outside the world
		for (OsmPrimitive osm : affectedNodes) {
			if (osm instanceof Node && ((Node)osm).coor.isOutSideWorld()) {
				JOptionPane.showMessageDialog(Main.parent, "Cannot move objects outside of the world.");
				return;
			}
		}
		
		Command c = !Main.main.editLayer().commands.isEmpty() ? Main.main.editLayer().commands.getLast() : null;
		if (c instanceof MoveCommand && affectedNodes.equals(((MoveCommand)c).objects))
			((MoveCommand)c).moveAgain(dx,dy);
		else
			Main.main.editLayer().add(new MoveCommand(selection, dx, dy));
		
		Main.map.mapView.repaint();
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
	@Override public void mousePressed(MouseEvent e) {
		if (e.getButton() != MouseEvent.BUTTON1)
			return;

		if (Main.ds.getSelected().size() == 0) {
			OsmPrimitive osm = Main.map.mapView.getNearest(e.getPoint(), (e.getModifiersEx() & MouseEvent.ALT_DOWN_MASK) != 0);
			if (osm != null)
				Main.ds.setSelected(osm);
			singleOsmPrimitive = osm;
			Main.map.mapView.repaint();
		} else
			singleOsmPrimitive = null;
		
		mousePos = e.getPoint();
		oldCursor = Main.map.mapView.getCursor();
		Main.map.mapView.setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
	}
	
	/**
	 * Restore the old mouse cursor.
	 */
	@Override public void mouseReleased(MouseEvent e) {
		Main.map.mapView.setCursor(oldCursor);
		if (singleOsmPrimitive != null) {
			Main.ds.clearSelection();
			Main.map.mapView.repaint();
		}
	}
}
