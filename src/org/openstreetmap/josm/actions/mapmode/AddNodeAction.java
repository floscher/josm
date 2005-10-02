package org.openstreetmap.josm.actions.mapmode;

import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;

import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.gui.MapFrame;

/**
 * This mode adds a new node to the dataset. The user clicks on a place to add
 * and there is it. Nothing more, nothing less.
 * 
 * Newly created nodes are selected. Shift modifier does not cancel the old 
 * selection as usual.
 * 
 * @author imi
 *
 */
public class AddNodeAction extends MapMode {

	/**
	 * Create an AddNodeAction. Mnemonic is 'a'
	 * @param mapFrame
	 */
	public AddNodeAction(MapFrame mapFrame) {
		super("Add nodes", "addnode", "Add new nodes to the map.", KeyEvent.VK_A, mapFrame);
	}

	@Override
	public void registerListener() {
		super.registerListener();
		mv.addMouseListener(this);
	}

	@Override
	public void unregisterListener() {
		super.unregisterListener();
		mv.removeMouseListener(this);
	}

	/**
	 * If user clicked with the left button, add a node at the current mouse
	 * position.
	 */
	@Override
	public void mouseClicked(MouseEvent e) {
		if (e.getButton() == MouseEvent.BUTTON1) {
			Node node = new Node();
			node.coor = mv.getPoint(e.getX(), e.getY(), true);
			ds.nodes.add(node);
			mv.repaint();
		}
	}
}
