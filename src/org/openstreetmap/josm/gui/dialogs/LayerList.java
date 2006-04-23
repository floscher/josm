package org.openstreetmap.josm.gui.dialogs;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Collection;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.UIManager;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.gui.MapFrame;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.gui.MapView.LayerChangeListener;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.ImageProvider.OverlayPosition;

/**
 * A component that manages the list of all layers and react to selection changes
 * by setting the active layer in the mapview.
 *
 * @author imi
 */
public class LayerList extends ToggleDialog implements LayerChangeListener {

	public final static class DeleteLayerAction extends AbstractAction {

        private final JList layers;
        private final Layer layer;

        public DeleteLayerAction(JList layers, Layer layer) {
            super("Delete", ImageProvider.get("dialogs", "delete"));
            putValue(SHORT_DESCRIPTION, "Delete the selected layer.");
            this.layers = layers;
            this.layer = layer;
        }

        public void actionPerformed(ActionEvent e) {
        	if (layers.getModel().getSize() == 1) {
        		Main.main.setMapFrame(null);
        		Main.ds = new DataSet();
        	} else {
        	    int sel = layers.getSelectedIndex();
                if (layer != null)
                    Main.main.getMapFrame().mapView.removeLayer(layer);
                else
                    Main.main.getMapFrame().mapView.removeLayer((Layer)layers.getSelectedValue());
                if (sel >= layers.getModel().getSize())
                    sel = layers.getModel().getSize()-1;
                if (layers.getSelectedValue() == null)
                    layers.setSelectedIndex(sel);
        	}
        }
    }

    public final static class ShowHideLayerAction extends AbstractAction {
        private final Layer layer;
        private final JList layers;

        public ShowHideLayerAction(JList layers, Layer layer) {
            super("Show/Hide", ImageProvider.get("dialogs", "showhide"));
            putValue(SHORT_DESCRIPTION, "Toggle visible state of the selected layer.");
            this.layer = layer;
            this.layers = layers;
        }

        public void actionPerformed(ActionEvent e) {
            Layer l = layer == null ? (Layer)layers.getSelectedValue() : layer;
            l.visible = !l.visible;
        	Main.main.getMapFrame().mapView.repaint();
        	layers.repaint();
        }
    }

    /**
	 * The data model for the list component.
	 */
	DefaultListModel model = new DefaultListModel();
	/**
	 * The list component holding all layers.
	 */
	JList layers = new JList(model);
	/**
	 * The merge action. This is only called, if the current selection and its
	 * item below are editable datasets and the merge button is clicked. 
	 */
	private final JButton mergeButton = new JButton(ImageProvider.get("dialogs", "mergedown"));
	/**
	 * Button for moving layer up.
	 */
	private JButton upButton = new JButton(ImageProvider.get("dialogs", "up"));
	/**
	 * Button for moving layer down.
	 */
	private JButton downButton = new JButton(ImageProvider.get("dialogs", "down"));
	/**
	 * Button for delete layer.
	 */
	private Action deleteAction = new DeleteLayerAction(layers, null);

	/**
	 * Create an layerlist and attach it to the given mapView.
	 */
	public LayerList(MapFrame mapFrame) {
		super("Layers", "layerlist", "Open a list of all loaded layers.", KeyEvent.VK_L);
		setPreferredSize(new Dimension(320,100));
		add(new JScrollPane(layers), BorderLayout.CENTER);
		layers.setBackground(UIManager.getColor("Button.background"));
		layers.setCellRenderer(new DefaultListCellRenderer(){
			@Override public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
				Layer layer = (Layer)value;
				JLabel label = (JLabel)super.getListCellRendererComponent(list, 
						layer.name, index, isSelected, cellHasFocus);
				Icon icon = layer.getIcon();
				if (!layer.visible)
					icon = ImageProvider.overlay(icon, "invisible", OverlayPosition.SOUTHEAST);
				label.setIcon(icon);
				label.setToolTipText(layer.getToolTipText());
				return label;
			}
		});

		final MapView mapView = mapFrame.mapView;

		Collection<Layer> data = mapView.getAllLayers();
		for (Layer l : data)
			model.addElement(l);

		layers.setSelectedValue(mapView.getActiveLayer(), true);
		layers.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		layers.addListSelectionListener(new ListSelectionListener(){
			public void valueChanged(ListSelectionEvent e) {
				if (layers.getSelectedIndex() == -1)
					layers.setSelectedIndex(e.getFirstIndex());
				mapView.setActiveLayer((Layer)layers.getSelectedValue());
			}
		});
		mapView.addLayerChangeListener(this);

		layers.addMouseListener(new MouseAdapter(){
			private void openPopup(MouseEvent e) {
				int index = layers.locationToIndex(e.getPoint());
				Layer layer = (Layer)layers.getModel().getElementAt(index);
				LayerListPopup menu = new LayerListPopup(layers, layer);
				menu.show(LayerList.this, e.getX(), e.getY());
			}
			@Override public void mousePressed(MouseEvent e) {
				if (e.isPopupTrigger())
					openPopup(e);
			}
			@Override public void mouseReleased(MouseEvent e) {
				if (e.isPopupTrigger())
					openPopup(e);
			}
		});
		
		
		// Buttons
		JPanel buttonPanel = new JPanel(new GridLayout(1, 5));

		ActionListener upDown = new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				Layer l = (Layer)layers.getSelectedValue();
				int sel = layers.getSelectedIndex();
				int selDest = e.getActionCommand().equals("up") ? sel-1 : sel+1;
				mapView.moveLayer(l, selDest);
				model.set(sel, model.get(selDest));
				model.set(selDest, l);
				layers.setSelectedIndex(selDest);
				updateButtonEnabled();
				mapView.repaint();
			}
		};

		upButton.setToolTipText("Move the selected layer one row up.");
		upButton.addActionListener(upDown);
		upButton.setActionCommand("up");
		buttonPanel.add(upButton);
		
		downButton.setToolTipText("Move the selected layer one row down.");
		downButton.addActionListener(upDown);
		downButton.setActionCommand("down");
		buttonPanel.add(downButton);
		
		JButton showHideButton = new JButton(new ShowHideLayerAction(layers, null));
        showHideButton.setText("");
        buttonPanel.add(showHideButton);
		
        JButton deleteButton = new JButton(deleteAction);
        deleteButton.setText("");
        buttonPanel.add(deleteButton);

		mergeButton.setToolTipText("Merge the selected layer into the layer directly below.");
		mergeButton.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				Layer lFrom = (Layer)layers.getSelectedValue();
				Layer lTo = (Layer)model.get(layers.getSelectedIndex()+1);
				lTo.mergeFrom(lFrom);
				layers.setSelectedValue(lTo, true);
				mapView.removeLayer(lFrom);
			}
		});		
		buttonPanel.add(mergeButton);

		add(buttonPanel, BorderLayout.SOUTH);
		
		updateButtonEnabled();
	}

	/**
	 * Updates the state of the Buttons.
	 */
	void updateButtonEnabled() {
		int sel = layers.getSelectedIndex();
		Layer l = (Layer)layers.getSelectedValue();
		boolean enable = model.getSize() > 1;
		enable = enable && sel < model.getSize()-1;
		enable = enable && l.isMergable((Layer)model.get(sel+1));
		mergeButton.setEnabled(enable);
		upButton.setEnabled(sel > 0);
		downButton.setEnabled(sel < model.getSize()-1);
		deleteAction.setEnabled(!model.isEmpty());
	}

	/**
	 * Add the new layer to the list.
	 */
	public void layerAdded(Layer newLayer) {
		model.add(0, newLayer);
		updateButtonEnabled();
	}

	public void layerRemoved(Layer oldLayer) {
		model.removeElement(oldLayer);
		if (layers.getSelectedIndex() == -1)
			layers.setSelectedIndex(0);
		updateButtonEnabled();
	}

	/**
	 * If the newLayer is not the actual selection, select it.
	 */
	public void activeLayerChange(Layer oldLayer, Layer newLayer) {
		if (newLayer != layers.getSelectedValue())
			layers.setSelectedValue(newLayer, true);
		updateButtonEnabled();
	}

	public void layerMoved(Layer layer, int newPosition) {}
}
