package org.openstreetmap.josm.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.AbstractButton;
import javax.swing.ButtonGroup;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;

import org.openstreetmap.josm.actions.AutoScaleAction;
import org.openstreetmap.josm.actions.mapmode.AddLineSegmentAction;
import org.openstreetmap.josm.actions.mapmode.AddNodeAction;
import org.openstreetmap.josm.actions.mapmode.AddTrackAction;
import org.openstreetmap.josm.actions.mapmode.CombineAction;
import org.openstreetmap.josm.actions.mapmode.DeleteAction;
import org.openstreetmap.josm.actions.mapmode.MapMode;
import org.openstreetmap.josm.actions.mapmode.MoveAction;
import org.openstreetmap.josm.actions.mapmode.SelectionAction;
import org.openstreetmap.josm.actions.mapmode.ZoomAction;
import org.openstreetmap.josm.gui.dialogs.LayerList;
import org.openstreetmap.josm.gui.dialogs.PropertiesDialog;
import org.openstreetmap.josm.gui.dialogs.SelectionListDialog;
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
		toolBarActions.add(new IconToggleButton(this, new ZoomAction(this)));
		toolBarActions.add(new IconToggleButton(this, new SelectionAction(this)));
		toolBarActions.add(new IconToggleButton(this, new MoveAction(this)));
		toolBarActions.add(new IconToggleButton(this, new AddNodeAction(this)));
		toolBarActions.add(new IconToggleButton(this, new AddLineSegmentAction(this)));
		toolBarActions.add(new IconToggleButton(this, new AddTrackAction(this)));
		toolBarActions.add(new IconToggleButton(this, new CombineAction(this)));
		toolBarActions.add(new IconToggleButton(this, new DeleteAction(this)));

		// all map modes in one button group
		ButtonGroup toolGroup = new ButtonGroup();
		for (Component c : toolBarActions.getComponents())
			toolGroup.add((AbstractButton)c);
		toolGroup.setSelected(((AbstractButton)toolBarActions.getComponent(0)).getModel(), true);
		selectMapMode((MapMode)((AbstractButton)toolBarActions.getComponent(0)).getAction());

		// autoScale
		toolBarActions.addSeparator();
		final JToggleButton autoScaleButton = new IconToggleButton(this, new AutoScaleAction(this));
		toolBarActions.add(autoScaleButton);
		autoScaleButton.setText(null);
		autoScaleButton.setSelected(mapView.isAutoScale());
		mapView.addPropertyChangeListener(new PropertyChangeListener(){
			public void propertyChange(PropertyChangeEvent evt) {
				if (evt.getPropertyName().equals("autoScale"))
					autoScaleButton.setSelected(mapView.isAutoScale());
			}
		});

		// layer list
		toolBarActions.add(new IconToggleButton(this, new LayerList(this)));
		
		// properties
		toolBarActions.add(new IconToggleButton(this, new PropertiesDialog(this)));

		// selection dialog
		SelectionListDialog selectionList = new SelectionListDialog(this);
		final IconToggleButton buttonSelection = new IconToggleButton(this, selectionList);
		selectionList.addWindowListener(new WindowAdapter(){
			@Override
			public void windowClosing(WindowEvent e) {
				buttonSelection.setSelected(false);
			}
		});
		toolBarActions.add(buttonSelection);

		// status line below the map
		statusLine = new MapStatus(this);
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
