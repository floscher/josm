package org.openstreetmap.josm.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;

import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.border.Border;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.openstreetmap.josm.actions.DebugAction;
import org.openstreetmap.josm.actions.MapMode;
import org.openstreetmap.josm.actions.SelectionAction;
import org.openstreetmap.josm.actions.ZoomAction;
import org.openstreetmap.josm.data.Preferences;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.projection.Projection;

/**
 * One Map frame with one dataset behind. This is the container gui class whose
 * display can be set to the different views.
 * 
 * @author imi
 */
public class MapFrame extends JComponent {

	/**
	 * Open the properties page
	 * @author imi
	 */
	public class PropertiesAction extends AbstractAction {
		private JDialog dlg;
		public PropertiesAction() {
			super("Properties", new ImageIcon("images/properties.png"));
			putValue(MNEMONIC_KEY, KeyEvent.VK_P);
		}
		public void actionPerformed(ActionEvent e) {
			if (dlg != null) {
				dlg.setVisible(true);
				dlg.requestFocus();
				return;
			}
			dlg = new JDialog(Main.main, "Properties of "+Main.main.getNameOfLoadedMapFrame(), false);
			final Border panelBorder = BorderFactory.createEmptyBorder(5,0,0,0);
			Box panel = Box.createVerticalBox();

			// making an array of all projections and the current one within
			Projection[] allProjections = Preferences.allProjections.clone();
			for (int i = 0; i < allProjections.length; ++i)
				if (allProjections[i].getClass() == mapView.getProjection().getClass())
					allProjections[i] = mapView.getProjection();
			
			// projection
			Box projectionPanel = Box.createHorizontalBox();
			projectionPanel.setBorder(panelBorder);
			projectionPanel.add(new JLabel("Projection"));
			final JComboBox projectionCombo = new JComboBox(allProjections);
			projectionPanel.add(projectionCombo);
			panel.add(projectionPanel);
			final JPanel configurationPanel = new JPanel();
			configurationPanel.setLayout(new BoxLayout(configurationPanel, BoxLayout.X_AXIS));
			
			// projections details
			projectionCombo.addActionListener(new ActionListener(){
				public void actionPerformed(ActionEvent e) {
					configurationPanel.removeAll();
					mapView.setProjection((Projection)projectionCombo.getSelectedItem());
					JComponent panel = mapView.getProjection().getConfigurationPanel();
					if (panel != null) {
						panel.setBorder(panelBorder);
						configurationPanel.add(panel);
					}
					dlg.pack();
				}
			});
			panel.add(configurationPanel);
			projectionCombo.setSelectedItem(mapView.getProjection());
			
			panel.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
			dlg.setContentPane(panel);
			dlg.pack();
			dlg.setResizable(false);
			dlg.setVisible(true);
		}
	}

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
	private JToolBar toolBarActions = new JToolBar(JToolBar.VERTICAL);

	/**
	 * Construct a map with a given DataSet. The set cannot be replaced after construction
	 * (but of course, the data can be altered using the map's editing features). 
	 */
	public MapFrame(DataSet dataSet) {
		setSize(400,400);
		setLayout(new BorderLayout());

		add(mapView = new MapView(dataSet), BorderLayout.CENTER);
		
		toolBarActions.setFloatable(false);
		toolBarActions.add(new IconToggleButton(new ZoomAction(this)));
		toolBarActions.add(new IconToggleButton(new SelectionAction(this)));
		toolBarActions.add(new IconToggleButton(new DebugAction(this)));

		// all map modes in one button group
		ButtonGroup toolGroup = new ButtonGroup();
		for (Component c : toolBarActions.getComponents())
			toolGroup.add((AbstractButton)c);
		toolGroup.setSelected(((AbstractButton)toolBarActions.getComponent(0)).getModel(), true);
		selectMapMode((MapMode)((AbstractButton)toolBarActions.getComponent(0)).getAction());
		
		// autoScale
		toolBarActions.addSeparator();
		final JToggleButton autoScaleButton = new IconToggleButton(mapView.new AutoScaleAction());
		toolBarActions.add(autoScaleButton);
		autoScaleButton.setText(null);
		autoScaleButton.setSelected(mapView.isAutoScale());
		mapView.addChangeListener(new ChangeListener(){
			public void stateChanged(ChangeEvent e) {
				autoScaleButton.setSelected(mapView.isAutoScale());
			}
		});

		// properties
		toolBarActions.add(new IconToggleButton(new PropertiesAction()));
	}

	/**
	 * Change the operating map mode for the view. Will call unregister on the
	 * old MapMode and register on the new one.
	 * @param mapMode	The new mode to set.
	 */
	public void selectMapMode(MapMode mapMode) {
		if (this.mapMode != null)
			this.mapMode.unregisterListener(mapView);
		this.mapMode = mapMode;
		mapMode.registerListener(mapView);
	}

	/**
	 * @return Returns the toolBarActions.
	 */
	public JToolBar getToolBarActions() {
		return toolBarActions;
	}
}
