package org.openstreetmap.josm.gui.preferences;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.StringTokenizer;

import javax.swing.Box;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.tools.GBC;

public class AnnotationPresetPreference implements PreferenceSetting {

	private JList annotationSources = new JList(new DefaultListModel());

	public void addGui(final PreferenceDialog gui) {
		String annos = Main.pref.get("annotation.sources");
		StringTokenizer st = new StringTokenizer(annos, ";");
		while (st.hasMoreTokens())
			((DefaultListModel)annotationSources.getModel()).addElement(st.nextToken());

		JButton addAnno = new JButton(tr("Add"));
		addAnno.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				String source = JOptionPane.showInputDialog(Main.parent, tr("Annotation preset source"));
				if (source == null)
					return;
				((DefaultListModel)annotationSources.getModel()).addElement(source);
				gui.requiresRestart = true;
			}
		});

		JButton editAnno = new JButton(tr("Edit"));
		editAnno.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				if (annotationSources.getSelectedIndex() == -1)
					JOptionPane.showMessageDialog(Main.parent, tr("Please select the row to edit."));
				else {
					String source = JOptionPane.showInputDialog(Main.parent, tr("Annotation preset source"), annotationSources.getSelectedValue());
					if (source == null)
						return;
					((DefaultListModel)annotationSources.getModel()).setElementAt(source, annotationSources.getSelectedIndex());
					gui.requiresRestart = true;
				}
			}
		});

		JButton deleteAnno = new JButton(tr("Delete"));
		deleteAnno.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				if (annotationSources.getSelectedIndex() == -1)
					JOptionPane.showMessageDialog(Main.parent, tr("Please select the row to delete."));
				else {
					((DefaultListModel)annotationSources.getModel()).remove(annotationSources.getSelectedIndex());
					gui.requiresRestart = true;
				}
			}
		});
		annotationSources.setVisibleRowCount(3);

		annotationSources.setToolTipText(tr("The sources (url or filename) of annotation preset definition files. See http://josm.eigenheimstrasse.de/wiki/AnnotationPresets for help."));
		addAnno.setToolTipText(tr("Add a new annotation preset source to the list."));
		deleteAnno.setToolTipText(tr("Delete the selected source from the list."));

		gui.map.add(new JLabel(tr("Annotation preset sources")), GBC.eol().insets(0,5,0,0));
		gui.map.add(new JScrollPane(annotationSources), GBC.eol().fill(GBC.BOTH));
		JPanel buttonPanel = new JPanel(new GridBagLayout());
		gui.map.add(buttonPanel, GBC.eol().fill(GBC.HORIZONTAL));
		buttonPanel.add(Box.createHorizontalGlue(), GBC.std().fill(GBC.HORIZONTAL));
		buttonPanel.add(addAnno, GBC.std().insets(0,5,0,0));
		buttonPanel.add(editAnno, GBC.std().insets(5,5,5,0));
		buttonPanel.add(deleteAnno, GBC.std().insets(0,5,0,0));
	}

	public void ok() {
		if (annotationSources.getModel().getSize() > 0) {
			StringBuilder sb = new StringBuilder();
			for (int i = 0; i < annotationSources.getModel().getSize(); ++i)
				sb.append(";"+annotationSources.getModel().getElementAt(i));
			Main.pref.put("annotation.sources", sb.toString().substring(1));
		} else
			Main.pref.put("annotation.sources", null);
	}
}
