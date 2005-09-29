package org.openstreetmap.josm.actions.mapmode;

import java.awt.event.KeyEvent;

import org.openstreetmap.josm.gui.MapFrame;
import org.openstreetmap.josm.gui.MapView;

/**
 * Move is an action that can move all kind of OsmPrimitives (except Keys for now).
 * 
 * If any object is selected, all selected objects are moved. If no object is 
 * selected, only nodes can be moved. In this case, the Node which is nearest to
 * the mouse cursor when the left mouse button is pressed get selected and moved.
 * (Of course, all primitives, which use this node are moved somewhat too).
 * 
 * @author imi
 */
public class MoveAction extends MapMode {

	public MoveAction(MapFrame mapFrame) {
		super("Move", "move", "Move selected objects around", KeyEvent.VK_M, mapFrame);
	}

	@Override
	public void registerListener(MapView mapView) {
	}

	@Override
	public void unregisterListener(MapView mapView) {
	}

}
