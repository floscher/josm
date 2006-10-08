package org.openstreetmap.josm.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JPanel;
import javax.swing.JToolBar;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.AutoScaleAction;
import org.openstreetmap.josm.actions.mapmode.AddSegmentAction;
import org.openstreetmap.josm.actions.mapmode.AddWayAction;
import org.openstreetmap.josm.actions.mapmode.DeleteAction;
import org.openstreetmap.josm.actions.mapmode.MapMode;
import org.openstreetmap.josm.actions.mapmode.MoveAction;
import org.openstreetmap.josm.actions.mapmode.SelectionAction;
import org.openstreetmap.josm.actions.mapmode.ZoomAction;
import org.openstreetmap.josm.actions.mapmode.AddNodeAction.AddNodeGroup;
import org.openstreetmap.josm.gui.dialogs.CommandStackDialog;
import org.openstreetmap.josm.gui.dialogs.ConflictDialog;
import org.openstreetmap.josm.gui.dialogs.HistoryDialog;
import org.openstreetmap.josm.gui.dialogs.LayerListDialog;
import org.openstreetmap.josm.gui.dialogs.PropertiesDialog;
import org.openstreetmap.josm.gui.dialogs.SelectionListDialog;
import org.openstreetmap.josm.gui.dialogs.ToggleDialog;

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
	
	public ConflictDialog conflictDialog;
	private JPanel toggleDialogs = new JPanel();
	
	/**
	 * Construct a map with a given DataSet. The set cannot be replaced after 
	 * construction (but of course, the data can be altered using the map's
	 * editing features).
	 * 
	 * @param layer The first layer in the mapView. 
	 */
	public MapFrame() {
		setSize(400,400);
		setLayout(new BorderLayout());

		final AutoScaleAction autoScaleAction = new AutoScaleAction(this);
		add(mapView = new MapView(autoScaleAction), BorderLayout.CENTER);

		// toolbar
		toolBarActions.setFloatable(false);
		toolBarActions.add(new IconToggleButton(new ZoomAction(this)));
		final Action selectionAction = new SelectionAction.Group(this);
		toolBarActions.add(new IconToggleButton(selectionAction));
		toolBarActions.add(new IconToggleButton(new MoveAction(this)));
		toolBarActions.add(new IconToggleButton(new AddNodeGroup(this)));
		toolBarActions.add(new IconToggleButton(new AddSegmentAction(this)));
		toolBarActions.add(new IconToggleButton(new AddWayAction(this)));
		toolBarActions.add(new IconToggleButton(new DeleteAction(this)));

		// all map modes in one button group
		ButtonGroup toolGroup = new ButtonGroup();
		for (Component c : toolBarActions.getComponents())
			toolGroup.add((AbstractButton)c);
		toolGroup.setSelected(((AbstractButton)toolBarActions.getComponent(0)).getModel(), true);

		// autoScale
		toolBarActions.addSeparator();
		final IconToggleButton autoScaleButton = new IconToggleButton(autoScaleAction);
		toolBarActions.add(autoScaleButton);
		autoScaleButton.setText(null);
		autoScaleButton.setSelected(mapView.isAutoScale());
		autoScaleAction.putValue("active", true);
		mapView.addPropertyChangeListener(new PropertyChangeListener(){
			public void propertyChange(PropertyChangeEvent evt) {
				if (evt.getPropertyName().equals("autoScale")) {
					autoScaleAction.putValue("active", evt.getNewValue());
					autoScaleButton.setSelected((Boolean)evt.getNewValue());
				}
			}
		});
		autoScaleButton.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				if (!autoScaleButton.groupbutton)
					autoScaleButton.setSelected(true);
            }
		});

		add(toggleDialogs, BorderLayout.EAST);
		toggleDialogs.setLayout(new BoxLayout(toggleDialogs, BoxLayout.Y_AXIS));

		addIconToggle(toggleDialogs, new LayerListDialog(this));
		addIconToggle(toggleDialogs, new PropertiesDialog(this));
		addIconToggle(toggleDialogs, new HistoryDialog());
		addIconToggle(toggleDialogs, new SelectionListDialog());
		addIconToggle(toggleDialogs, conflictDialog = new ConflictDialog());
		addIconToggle(toggleDialogs, new CommandStackDialog(this));

		// status line below the map
		statusLine = new MapStatus(this);
	}

	public Action getDefaultButtonAction() {
	    return ((AbstractButton)toolBarActions.getComponent(0)).getAction();
    }

	/**
	 * Open all ToggleDialogs that have their preferences property set. Close all others.
	 */
	public void setVisibleDialogs() {
		for (Component c : toggleDialogs.getComponents()) {
			if (c instanceof ToggleDialog) {
				boolean sel = Main.pref.getBoolean(((ToggleDialog)c).prefName+".visible");
				((ToggleDialog)c).action.button.setSelected(sel);
				c.setVisible(sel);
			}
		}
	}

	private void addIconToggle(JPanel toggleDialogs, ToggleDialog dlg) {
        IconToggleButton button = new IconToggleButton(dlg.action);
        dlg.action.button = button;
		toolBarActions.add(button);
		toggleDialogs.add(dlg);
	}

	
	/**
	 * Fires an property changed event "visible".
	 */
	@Override public void setVisible(boolean aFlag) {
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
		if (mapMode == this.mapMode)
			return;
		if (this.mapMode != null)
			this.mapMode.exitMode();
		this.mapMode = mapMode;
		mapMode.enterMode();
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
