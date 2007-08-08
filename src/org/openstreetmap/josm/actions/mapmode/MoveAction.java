// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.actions.mapmode;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Cursor;
import java.awt.Point;
import java.awt.Rectangle;
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
import org.openstreetmap.josm.gui.SelectionManager;
import org.openstreetmap.josm.gui.SelectionManager.SelectionEnded;
import org.openstreetmap.josm.tools.ImageProvider;
/**
 * Move is an action that can move all kind of OsmPrimitives (except Keys for now).
 *
 * If an selected object is under the mouse when dragging, move all selected objects.
 * If an unselected object is under the mouse when dragging, it becomes selected
 * and will be moved.
 * If no object is under the mouse, move all selected objects (if any)
 * 
 * @author imi
 */
public class MoveAction extends MapMode implements SelectionEnded {
	/**
	 * The old cursor before the user pressed the mouse button.
	 */
	private Cursor oldCursor;
	/**
	 * The position of the mouse before the user moves a node.
	 */
	private Point mousePos;
	private SelectionManager selectionManager;
	private boolean selectionMode = false;

	/**
	 * Create a new MoveAction
	 * @param mapFrame The MapFrame, this action belongs to.
	 */
	public MoveAction(MapFrame mapFrame) {
		super(tr("Move"), 
				"move",
				tr("Move around objects that are under the mouse or selected."), 
				KeyEvent.VK_M,
				mapFrame,
				ImageProvider.getCursor("normal", "move"));
		selectionManager = new SelectionManager(this, false, mapFrame.mapView);
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
	 * objects (if one of them is under the mouse) or the current one under the
	 * mouse (which will become selected).
	 */
	@Override public void mouseDragged(MouseEvent e) {
		if ((e.getModifiersEx() & MouseEvent.BUTTON1_DOWN_MASK) == 0)
			return;

		if (selectionMode)
			return;

		if (mousePos == null)
			mousePos = e.getPoint();

		EastNorth mouseEN = Main.map.mapView.getEastNorth(e.getX(), e.getY());
		EastNorth mouseStartEN = Main.map.mapView.getEastNorth(mousePos.x, mousePos.y);
		double dx = mouseEN.east() - mouseStartEN.east();
		double dy = mouseEN.north() - mouseStartEN.north();
		if (dx == 0 && dy == 0)
			return;

		Collection<OsmPrimitive> selection = Main.ds.getSelected();
		Collection<Node> affectedNodes = AllNodesVisitor.getAllNodes(selection);

		// check if any coordinate would be outside the world
		for (OsmPrimitive osm : affectedNodes) {
			if (osm instanceof Node && ((Node)osm).coor.isOutSideWorld()) {
				JOptionPane.showMessageDialog(Main.parent,tr("Cannot move objects outside of the world."));
				return;
			}
		}

		Command c = !Main.main.undoRedo.commands.isEmpty() ? Main.main.undoRedo.commands.getLast() : null;
		if (c instanceof MoveCommand && affectedNodes.equals(((MoveCommand)c).objects))
			((MoveCommand)c).moveAgain(dx,dy);
		else
			Main.main.undoRedo.add(new MoveCommand(selection, dx, dy));

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

		Collection<OsmPrimitive> sel = Main.ds.getSelected();
		OsmPrimitive osm = Main.map.mapView.getNearest(e.getPoint(), (e.getModifiersEx() & MouseEvent.ALT_DOWN_MASK) != 0);
		if (osm != null) {
			if (!sel.contains(osm))
				Main.ds.setSelected(osm);
			oldCursor = Main.map.mapView.getCursor();
			Main.map.mapView.setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
		} else {
			selectionMode = true;
			selectionManager.register(Main.map.mapView);
		}

		Main.map.mapView.repaint();

		mousePos = e.getPoint();
	}

	/**
	 * Restore the old mouse cursor.
	 */
	@Override public void mouseReleased(MouseEvent e) {
		if (selectionMode) {
			selectionManager.unregister(Main.map.mapView);
			selectionMode = false;
		} else
			Main.map.mapView.setCursor(oldCursor);
	}


	public void selectionEnded(Rectangle r, boolean alt, boolean shift, boolean ctrl) {
		SelectionAction.selectEverythingInRectangle(selectionManager, r, alt, shift, ctrl);
	}
}
