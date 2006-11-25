package org.openstreetmap.josm.gui.preferences;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JCheckBox;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.tools.GBC;

public class DrawingPreference implements PreferenceSetting {

	private JCheckBox drawRawGpsLines = new JCheckBox(tr("Draw lines between raw gps points."));
	private JCheckBox forceRawGpsLines = new JCheckBox(tr("Force lines if no segments imported."));
	private JCheckBox largeGpsPoints = new JCheckBox(tr("Draw large GPS points."));
	private JCheckBox directionHint = new JCheckBox(tr("Draw Direction Arrows"));

	public void addGui(PreferenceDialog gui) {
		// drawRawGpsLines
		drawRawGpsLines.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				if (!drawRawGpsLines.isSelected())
					forceRawGpsLines.setSelected(false);
				forceRawGpsLines.setEnabled(drawRawGpsLines.isSelected());
			}
		});
		drawRawGpsLines.setSelected(Main.pref.getBoolean("draw.rawgps.lines"));
		drawRawGpsLines.setToolTipText(tr("If your gps device draw to few lines, select this to draw lines along your way."));
		gui.display.add(drawRawGpsLines, GBC.eol().insets(20,0,0,0));

		// forceRawGpsLines
		forceRawGpsLines.setToolTipText(tr("Force drawing of lines if the imported data contain no line information."));
		forceRawGpsLines.setSelected(Main.pref.getBoolean("draw.rawgps.lines.force"));
		forceRawGpsLines.setEnabled(drawRawGpsLines.isSelected());
		gui.display.add(forceRawGpsLines, GBC.eop().insets(40,0,0,0));
		
		// largeGpsPoints
		largeGpsPoints.setSelected(Main.pref.getBoolean("draw.rawgps.large"));
		largeGpsPoints.setToolTipText(tr("Draw larger dots for the GPS points."));
		gui.display.add(largeGpsPoints, GBC.eop().insets(20,0,0,0));
		
		// directionHint
		directionHint.setToolTipText(tr("Draw direction hints for all segments."));
		directionHint.setSelected(Main.pref.getBoolean("draw.segment.direction"));
		gui.display.add(directionHint, GBC.eop().insets(20,0,0,0));
	}

	public void ok() {
		Main.pref.put("draw.rawgps.lines", drawRawGpsLines.isSelected());
		Main.pref.put("draw.rawgps.lines.force", forceRawGpsLines.isSelected());
		Main.pref.put("draw.rawgps.large", largeGpsPoints.isSelected());
		Main.pref.put("draw.segment.direction", directionHint.isSelected());
    }
}
