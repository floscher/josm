package org.openstreetmap.josm.gui.dialogs;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.Border;

import org.openstreetmap.josm.data.Preferences;
import org.openstreetmap.josm.data.projection.Projection;
import org.openstreetmap.josm.gui.Main;
import org.openstreetmap.josm.gui.MapFrame;

/**
 * Open a Property dialog for the current visible map. When saving to own josm-
 * data format, the properties are saved along.
 * 
 * @author imi
 */
public class PropertiesDialog extends ToggleDialog {

	/**
	 * Create a new PropertiesDialog
	 * @param frame The mapFrame, this dialog is attached to.
	 */
	public PropertiesDialog(final MapFrame frame) {
		super("Properties of "+Main.main.getNameOfLoadedMapFrame(), "Properties Dialog", "properties", KeyEvent.VK_P, "Property page for this map.");
		putValue(MNEMONIC_KEY, KeyEvent.VK_P);

		final Border panelBorder = BorderFactory.createEmptyBorder(5,0,0,0);
		Box panel = Box.createVerticalBox();

		// making an array of all projections and the current one within
		Projection[] allProjections = Preferences.allProjections.clone();
		for (int i = 0; i < allProjections.length; ++i)
			if (allProjections[i].getClass() == frame.mapView.getProjection().getClass())
				allProjections[i] = frame.mapView.getProjection();
		
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
				frame.mapView.setProjection((Projection)projectionCombo.getSelectedItem());
				JComponent panel = frame.mapView.getProjection().getConfigurationPanel();
				if (panel != null) {
					panel.setBorder(panelBorder);
					configurationPanel.add(panel);
				}
				pack();
			}
		});
		panel.add(configurationPanel);
		projectionCombo.setSelectedItem(frame.mapView.getProjection());
		
		panel.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
		setContentPane(panel);
		pack();
		setResizable(false);
	}
}