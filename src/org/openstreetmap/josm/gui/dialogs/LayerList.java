package org.openstreetmap.josm.gui.dialogs;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.Collection;

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

import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.gui.ImageProvider;
import org.openstreetmap.josm.gui.Main;
import org.openstreetmap.josm.gui.MapFrame;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.gui.MapView.LayerChangeListener;
import org.openstreetmap.josm.gui.layer.Layer;

/**
 * A component that manages the list of all layers and react to selection changes
 * by setting the active layer in the mapview.
 *
 * @author imi
 */
public class LayerList extends ToggleDialog implements LayerChangeListener {

	/**
	 * The data model for the list component.
	 */
	private DefaultListModel model = new DefaultListModel();
	/**
	 * The list component holding all layers.
	 */
	private JList layers = new JList(model);
	/**
	 * The invisible icon blended over invisible layers.
	 */
	private static final Icon invisible = ImageProvider.get("layer", "invisible");

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
	private JButton deleteButton = new JButton(ImageProvider.get("dialogs", "delete"));
	
	/**
	 * Create an layerlist and attach it to the given mapView.
	 */
	public LayerList(MapFrame mapFrame) {
		super(mapFrame, "Layers", "List of all layers", "layerlist", KeyEvent.VK_L, "Open a list of all loaded layers.");
		setSize(250,256);
		setMinimumSize(new Dimension(70,70));
		add(new JScrollPane(layers), BorderLayout.CENTER);
		layers.setBackground(UIManager.getColor("Button.background"));
		layers.setCellRenderer(new DefaultListCellRenderer(){
			@Override
			public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
				Layer layer = (Layer)value;
				JLabel label = (JLabel)super.getListCellRendererComponent(list, 
						layer.getName(), index, isSelected, cellHasFocus);
				Icon icon = layer.getIcon();
				if (!layer.isVisible())
					icon = ImageProvider.overlay(icon, invisible, ImageProvider.OverlayPosition.SOUTHEAST);
				label.setIcon(icon);
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
		
		JButton visible = new JButton(ImageProvider.get("dialogs", "showhide"));
		visible.setToolTipText("Toggle visible state of the selected layer.");
		visible.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				Layer l = (Layer)layers.getSelectedValue();
				l.setVisible(!l.isVisible());
				mapView.repaint();
				layers.repaint();
			}
		});
		buttonPanel.add(visible);

		deleteButton.setToolTipText("Delete the selected layer.");
		deleteButton.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				if (model.size() == 1) {
					Main.main.setMapFrame(null, null);
				} else {
					int sel = layers.getSelectedIndex();
					mapView.removeLayer((Layer)layers.getSelectedValue());
					if (sel >= model.getSize())
						sel = model.getSize()-1;
					layers.setSelectedIndex(sel);
				}
			}
		});
		buttonPanel.add(deleteButton);

		mergeButton.setToolTipText("Merge the selected layer into the layer directly below.");
		mergeButton.addActionListener(new ActionListener(){
				public void actionPerformed(ActionEvent e) {
					Layer lFrom = (Layer)layers.getSelectedValue();
					DataSet dsFrom = lFrom.getDataSet();
					Layer lTo = (Layer)model.get(layers.getSelectedIndex()+1);
					DataSet dsTo = lTo.getDataSet();
					dsTo.mergeFrom(dsFrom, Main.pref.mergeNodes);
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
	private void updateButtonEnabled() {
		int sel = layers.getSelectedIndex();
		Layer l = (Layer)layers.getSelectedValue();
		boolean enable = model.getSize() > 1;
		enable = enable && sel < model.getSize()-1;
		enable = enable && l.getDataSet() != null;
		enable = enable && ((Layer)model.get(sel+1)).getDataSet() != null;
		enable = enable && l.isEditable() == ((Layer)model.get(sel+1)).isEditable();
		mergeButton.setEnabled(enable);
		upButton.setEnabled(sel > 0);
		downButton.setEnabled(sel < model.getSize()-1);
		deleteButton.setEnabled(!model.isEmpty());
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
}
