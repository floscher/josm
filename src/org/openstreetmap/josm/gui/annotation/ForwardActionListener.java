package org.openstreetmap.josm.gui.annotation;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import org.openstreetmap.josm.gui.dialogs.PropertiesDialog;

/**
 * Just an ActionListener that forwards calls to actionPerformed to some other
 * listener doing some refresh stuff on the way.
 * @author imi
 */
public final class ForwardActionListener implements ActionListener {
	public final AnnotationPreset preset;

	private final PropertiesDialog propertiesDialog;

	public ForwardActionListener(PropertiesDialog propertiesDialog, AnnotationPreset preset) {
		this.propertiesDialog = propertiesDialog;
		this.preset = preset;
	}

	public void actionPerformed(ActionEvent e) {
		this.propertiesDialog.annotationPresets.setSelectedIndex(0);
		e.setSource(this);
		preset.actionPerformed(e);
	}
}
