package org.openstreetmap.josm.actions.mapmode;

import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;

import javax.swing.JOptionPane;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.command.AddCommand;
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

	public AddNodeAction(MapFrame mapFrame) {
		super("Add nodes", "addnode", "Add nodes to the map.", "N", KeyEvent.VK_N, mapFrame);
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
			Node node = new Node(mv.getLatLon(e.getX(), e.getY()));
			if (node.coor.isOutSideWorld()) {
				JOptionPane.showMessageDialog(Main.main, "Can not add a node outside of the world.");
				return;
			}
			mv.editLayer().add(new AddCommand(Main.main.ds, node));
			mv.repaint();
		}
	}
}
