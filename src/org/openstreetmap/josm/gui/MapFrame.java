package org.openstreetmap.josm.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.AbstractButton;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;

import org.openstreetmap.josm.actions.AutoScaleAction;
import org.openstreetmap.josm.actions.mapmode.AddLineSegmentAction;
import org.openstreetmap.josm.actions.mapmode.AddNodeAction;
import org.openstreetmap.josm.actions.mapmode.AddWayAction;
import org.openstreetmap.josm.actions.mapmode.DeleteAction;
import org.openstreetmap.josm.actions.mapmode.MapMode;
import org.openstreetmap.josm.actions.mapmode.MoveAction;
import org.openstreetmap.josm.actions.mapmode.SelectionAction;
import org.openstreetmap.josm.actions.mapmode.ZoomAction;
import org.openstreetmap.josm.gui.dialogs.LayerList;
import org.openstreetmap.josm.gui.dialogs.PropertiesDialog;
import org.openstreetmap.josm.gui.dialogs.SelectionListDialog;
import org.openstreetmap.josm.gui.dialogs.ToggleDialog;
import org.openstreetmap.josm.gui.layer.Layer;

/**
 * One Map frame with one dataset behind. This is the container gui class whose
 * display can be set to the different views.
 * 
 * @author imi
 */
public class MapFrame extends JPanel {

	/**
	 * The current mode, this frame operates.
	 */
	public MapMode mapMode;
	/**
	 * The view control displayed.
	 */
	public MapView mapView;
	/**
	 * The toolbar with the action icons
	 */
	public JToolBar toolBarActions = new JToolBar(JToolBar.VERTICAL);
	/**
	 * The status line below the map
	 */
	public MapStatus statusLine;
	/**
	 * Construct a map with a given DataSet. The set cannot be replaced after 
	 * construction (but of course, the data can be altered using the map's
	 * editing features).
	 * 
	 * @param layer The first layer in the mapView. 
	 */
	public MapFrame(Layer layer) {
		setSize(400,400);
		setLayout(new BorderLayout());

		add(mapView = new MapView(layer), BorderLayout.CENTER);

		// toolbar
		toolBarActions.setFloatable(false);
		toolBarActions.add(new IconToggleButton(new ZoomAction(this)));
		final SelectionAction selectionAction = new SelectionAction(this);
		toolBarActions.add(new IconToggleButton(selectionAction));
		toolBarActions.add(new IconToggleButton(new MoveAction(this)));
		toolBarActions.add(new IconToggleButton(new AddNodeAction(this)));
		toolBarActions.add(new IconToggleButton(new AddLineSegmentAction(this)));
		toolBarActions.add(new IconToggleButton(new AddWayAction(this, selectionAction)));
		toolBarActions.add(new IconToggleButton(new DeleteAction(this)));

		// all map modes in one button group
		ButtonGroup toolGroup = new ButtonGroup();
		for (Component c : toolBarActions.getComponents())
			toolGroup.add((AbstractButton)c);
		toolGroup.setSelected(((AbstractButton)toolBarActions.getComponent(0)).getModel(), true);
		selectMapMode((MapMode)((AbstractButton)toolBarActions.getComponent(0)).getAction());

		// autoScale
		toolBarActions.addSeparator();
		final JToggleButton autoScaleButton = new IconToggleButton(new AutoScaleAction(this));
		toolBarActions.add(autoScaleButton);
		autoScaleButton.setText(null);
		autoScaleButton.setSelected(mapView.isAutoScale());
		mapView.addPropertyChangeListener(new PropertyChangeListener(){
			public void propertyChange(PropertyChangeEvent evt) {
				if (evt.getPropertyName().equals("autoScale"))
					autoScaleButton.setSelected(mapView.isAutoScale());
			}
		});

		JPanel toggleDialogs = new JPanel();
		add(toggleDialogs, BorderLayout.EAST);

		toggleDialogs.setLayout(new BoxLayout(toggleDialogs, BoxLayout.Y_AXIS));
		addIconToggle(toggleDialogs, new LayerList(this));
		addIconToggle(toggleDialogs, new PropertiesDialog(this));
		addIconToggle(toggleDialogs, new SelectionListDialog(this));

		// status line below the map
		statusLine = new MapStatus(this);
	}


	private void addIconToggle(JPanel toggleDialogs, ToggleDialog dlg) {
        toolBarActions.add(new IconToggleButton(dlg.action));
		toggleDialogs.add(dlg);
	}

	
	/**
	 * Fires an property changed event "visible".
	 */
	@Override
	public void setVisible(boolean aFlag) {
		boolean old = isVisible();
		super.setVisible(aFlag);
		if (old != aFlag)
			firePropertyChange("visible", old, aFlag);
	}



	/**
	 * Change the operating map mode for the view. Will call unregister on the
	 * old MapMode and register on the new one.
	 * @param mapMode	The new mode to set.
	 */
	public void selectMapMode(MapMode mapMode) {
		if (this.mapMode != null)
			this.mapMode.unregisterListener();
		this.mapMode = mapMode;
		mapMode.registerListener();
	}

	/**
	 * Fill the given panel by adding all necessary components to the different
	 * locations.
	 * 
	 * @param panel The container to fill. Must have an BorderLayout.
	 */
	public void fillPanel(Container panel) {
		panel.add(this, BorderLayout.CENTER);
		panel.add(toolBarActions, BorderLayout.WEST);
		panel.add(statusLine, BorderLayout.SOUTH);
	}
}
