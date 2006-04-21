package org.openstreetmap.josm.gui.dialogs;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.tools.ImageProvider;

/**
 * Popup menu handler for the layer list.
 */
public class LayerListPopup extends JPopupMenu {

	public final static class InfoAction extends AbstractAction {
	    private final Layer layer;
	    public InfoAction(Layer layer) {
	    	super("Info", ImageProvider.get("info"));
		    this.layer = layer;
	    }
	    public void actionPerformed(ActionEvent e) {
	    	JOptionPane.showMessageDialog(Main.main, layer.getInfoComponent());
	    }
    }

	public LayerListPopup(final JList layers, final Layer layer) {
		add(new LayerList.ShowHideLayerAction(layers, layer));
		add(new LayerList.DeleteLayerAction(layers, layer));
		addSeparator();

		layer.addMenuEntries(this);
	}
}
