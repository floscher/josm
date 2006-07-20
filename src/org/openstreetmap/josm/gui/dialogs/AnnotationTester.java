package org.openstreetmap.josm.gui.dialogs;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import org.xml.sax.SAXException;

public class AnnotationTester extends JFrame {

	private JComboBox annotationPresets;
	private final String[] args;
	private JPanel annotationPanel = new JPanel(new BorderLayout());
	private JPanel panel = new JPanel(new BorderLayout());

	public void reload() {
		Vector<AnnotationPreset> allPresets = new Vector<AnnotationPreset>();
		for (String source : args) {
			InputStream in = null;
			try {
				if (source.startsWith("http") || source.startsWith("ftp") || source.startsWith("file"))
					in = new URL(source).openStream();
				else if (source.startsWith("resource://"))
					in = AnnotationTester.class.getResourceAsStream(source.substring("resource:/".length()));
				else
					in = new FileInputStream(source);
				allPresets.addAll(AnnotationPreset.readAll(in));
			} catch (IOException e) {
				e.printStackTrace();
				JOptionPane.showMessageDialog(null, "Could not read annotation preset source: "+source);
			} catch (SAXException e) {
				e.printStackTrace();
				JOptionPane.showMessageDialog(null, "Error parsing "+source+": "+e.getMessage());
			}
		}
		annotationPresets.setModel(new DefaultComboBoxModel(allPresets));
	}

	public void reselect() {
		annotationPanel.removeAll();
		AnnotationPreset preset = (AnnotationPreset)annotationPresets.getSelectedItem();
		JPanel p = preset.createPanel();
		p.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
		if (p != null)
			annotationPanel.add(p, BorderLayout.NORTH);
		panel.validate();
		panel.repaint();
	}
	
	public AnnotationTester(String[] args) {
		super("Annotation Preset Tester");
		this.args = args;
		annotationPresets = new JComboBox();
		reload();

		panel.add(annotationPresets, BorderLayout.NORTH);
		panel.add(annotationPanel, BorderLayout.CENTER);
		annotationPresets.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				reselect();
			}
		});
		reselect();

		JButton b = new JButton("Reload");
		b.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				int i = annotationPresets.getSelectedIndex();
				reload();
				annotationPresets.setSelectedIndex(i);
			}
		});
		panel.add(b, BorderLayout.SOUTH);

		setContentPane(panel);
		setSize(300,500);
		setVisible(true);
	}

	public static void main(String[] args) {
		JFrame f = new AnnotationTester(args);
		f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	}
}
