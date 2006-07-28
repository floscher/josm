package org.openstreetmap.josm.gui.dialogs;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.BorderLayout;
import java.awt.Component;
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

	/**
	 * The last layerlist created. Used to update the list in the Show/Hide and Delete actions.
	 * TODO: Replace with Listener-Pattern.
	 */
	static JList instance;

	public final static class DeleteLayerAction extends AbstractAction {

		private final Layer layer;

		public DeleteLayerAction(Layer layer) {
			super(tr("Delete"), ImageProvider.get("dialogs", "delete"));
			putValue(SHORT_DESCRIPTION, tr("Delete the selected layer."));
			this.layer = layer;
		}

		public void actionPerformed(ActionEvent e) {
			int sel = instance.getSelectedIndex();
			if (layer != null)
				Main.main.removeLayer(layer);
			else
				Main.main.removeLayer((Layer)instance.getSelectedValue());
			if (sel >= instance.getModel().getSize())
				sel = instance.getModel().getSize()-1;
			if (instance.getSelectedValue() == null)
				instance.setSelectedIndex(sel);
		}
	}

	public final static class ShowHideLayerAction extends AbstractAction {
		private final Layer layer;

		public ShowHideLayerAction(Layer layer) {
			super(tr("Show/Hide"), ImageProvider.get("dialogs", "showhide"));
			putValue(SHORT_DESCRIPTION, tr("Toggle visible state of the selected layer."));
			this.layer = layer;
		}

		public void actionPerformed(ActionEvent e) {
			Layer l = layer == null ? (Layer)instance.getSelectedValue() : layer;
			l.visible = !l.visible;
			Main.map.mapView.repaint();
			instance.repaint();
		}
	}

	/**
	 * The data model for the list component.
	 */
	DefaultListModel model = new DefaultListModel();
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
	private Action deleteAction = new DeleteLayerAction(null);

	/**
	 * Create an layerlist and attach it to the given mapView.
	 */
	public LayerList(MapFrame mapFrame) {
		super(tr("Layers"), "layerlist", tr("Open a list of all loaded layers."), KeyEvent.VK_L, 100);
		instance = new JList(model);
		add(new JScrollPane(instance), BorderLayout.CENTER);
		instance.setBackground(UIManager.getColor("Button.background"));
		instance.setCellRenderer(new DefaultListCellRenderer(){
			@Override public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
				Layer layer = (Layer)value;
				JLabel label = (JLabel)super.getListCellRendererComponent(list, 
						layer.name, index, isSelected, cellHasFocus);
				Icon icon = layer.getIcon();
				if (!layer.visible)
					icon = ImageProvider.overlay(icon, "overlay/invisible", OverlayPosition.SOUTHEAST);
				label.setIcon(icon);
				label.setToolTipText(layer.getToolTipText());
				return label;
			}
		});

		final MapView mapView = mapFrame.mapView;

		Collection<Layer> data = mapView.getAllLayers();
		for (Layer l : data)
			model.addElement(l);

		instance.setSelectedValue(mapView.getActiveLayer(), true);
		instance.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		instance.addListSelectionListener(new ListSelectionListener(){
			public void valueChanged(ListSelectionEvent e) {
				if (instance.getModel().getSize() == 0)
					return;
				if (instance.getSelectedIndex() == -1)
					instance.setSelectedIndex(e.getFirstIndex());
				mapView.setActiveLayer((Layer)instance.getSelectedValue());
			}
		});
		mapView.addLayerChangeListener(this);

		instance.addMouseListener(new MouseAdapter(){
			private void openPopup(MouseEvent e) {
				int index = instance.locationToIndex(e.getPoint());
				Layer layer = (Layer)instance.getModel().getElementAt(index);
				LayerListPopup menu = new LayerListPopup(instance, layer);
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
				Layer l = (Layer)instance.getSelectedValue();
				int sel = instance.getSelectedIndex();
				int selDest = e.getActionCommand().equals("up") ? sel-1 : sel+1;
				mapView.moveLayer(l, selDest);
				model.set(sel, model.get(selDest));
				model.set(selDest, l);
				instance.setSelectedIndex(selDest);
				updateButtonEnabled();
				mapView.repaint();
			}
		};

		upButton.setToolTipText(tr("Move the selected layer one row up."));
		upButton.addActionListener(upDown);
		upButton.setActionCommand("up");
		buttonPanel.add(upButton);

		downButton.setToolTipText(tr("Move the selected layer one row down."));
		downButton.addActionListener(upDown);
		downButton.setActionCommand("down");
		buttonPanel.add(downButton);

		JButton showHideButton = new JButton(new ShowHideLayerAction(null));
		showHideButton.setText("");
		buttonPanel.add(showHideButton);

		JButton deleteButton = new JButton(deleteAction);
		deleteButton.setText("");
		buttonPanel.add(deleteButton);

		mergeButton.setToolTipText(tr("Merge the selected layer into the layer directly below."));
		mergeButton.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				Layer lFrom = (Layer)instance.getSelectedValue();
				Layer lTo = (Layer)model.get(instance.getSelectedIndex()+1);
				lTo.mergeFrom(lFrom);
				instance.setSelectedValue(lTo, true);
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
		int sel = instance.getSelectedIndex();
		Layer l = (Layer)instance.getSelectedValue();
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
		if (instance.getSelectedIndex() == -1)
			instance.setSelectedIndex(0);
		updateButtonEnabled();
	}

	/**
	 * If the newLayer is not the actual selection, select it.
	 */
	public void activeLayerChange(Layer oldLayer, Layer newLayer) {
		if (newLayer != instance.getSelectedValue())
			instance.setSelectedValue(newLayer, true);
		updateButtonEnabled();
	}

	public void layerMoved(Layer layer, int newPosition) {}
}
