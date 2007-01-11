package org.openstreetmap.josm.gui.annotation;

import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trn;

import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.command.ChangePropertyCommand;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.command.SequenceCommand;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.XmlObjectParser;
import org.xml.sax.SAXException;


/**
 * This class read encapsulate one annotation preset. A class method can
 * read in all predefined presets, either shipped with JOSM or that are
 * in the config directory.
 * 
 * It is also able to construct dialogs out of preset definitions.
 */
public class AnnotationPreset extends AbstractAction {

	private static interface Item {
		void addToPanel(JPanel p);
		void addCommands(Collection<OsmPrimitive> sel, List<Command> cmds);
	}
	
	public static class Text implements Item {
		public String key;
		public String text;
		public String default_;
		public boolean delete_if_empty = false;

		private JTextField value = new JTextField();

		public void addToPanel(JPanel p) {
			value.setText(default_ == null ? "" : default_);
			p.add(new JLabel(text), GBC.std().insets(0,0,10,0));
			p.add(value, GBC.eol().fill(GBC.HORIZONTAL));
		}
		public void addCommands(Collection<OsmPrimitive> sel, List<Command> cmds) {
			String v = value.getText();
			if (delete_if_empty && v.length() == 0)
				v = null;
			cmds.add(new ChangePropertyCommand(sel, key, v));
		}
	}

	public static class Check implements Item {
		public String key;
		public String text;
		public boolean default_ = false;
		
		private JCheckBox check = new JCheckBox();

		public void addToPanel(JPanel p) {
			check.setSelected(default_);
			check.setText(text);
			p.add(check, GBC.eol().fill(GBC.HORIZONTAL));
		}
		public void addCommands(Collection<OsmPrimitive> sel, List<Command> cmds) {
			cmds.add(new ChangePropertyCommand(sel, key, check.isSelected() ? "true" : null));
		}
	}

	public static class Combo implements Item {
		public String key;
		public String text;
		public String values;
		public String display_values = "";
		public String default_;
		public boolean delete_if_empty = false;
		public boolean editable = true;

		private JComboBox combo;

		public void addToPanel(JPanel p) {
			combo = new JComboBox(display_values.split(","));
			combo.setEditable(editable);
			combo.setSelectedItem(default_);
			p.add(new JLabel(text), GBC.std().insets(0,0,10,0));
			p.add(combo, GBC.eol().fill(GBC.HORIZONTAL));
		}
		public void addCommands(Collection<OsmPrimitive> sel, List<Command> cmds) {
			String v = combo.getSelectedIndex() == -1 ? null : values.split(",")[combo.getSelectedIndex()];
			String str = combo.isEditable()?combo.getEditor().getItem().toString() : v;
			if (delete_if_empty && str != null && str.length() == 0)
				str = null;
			cmds.add(new ChangePropertyCommand(sel, key, str));
		}
	}

	public static class Label implements Item {
		public String text;

		public void addToPanel(JPanel p) {
			p.add(new JLabel(text), GBC.eol());
		}
		public void addCommands(Collection<OsmPrimitive> sel, List<Command> cmds) {}
	}

	public static class Key implements Item {
		public String key;
		public String value;

		public void addToPanel(JPanel p) {}
		public void addCommands(Collection<OsmPrimitive> sel, List<Command> cmds) {
			cmds.add(new ChangePropertyCommand(sel, key, value != null && !value.equals("") ? value : null));
		}
	}

	/**
     * The types as preparsed collection.
     */
    public Collection<Class<?>> types;
	private List<Item> data = new LinkedList<Item>();

	/**
	 * Create an empty annotation preset. This will not have any items and
	 * will be an empty string as text. createPanel will return null.
	 * Use this as default item for "do not select anything".
	 */
	public AnnotationPreset() {}

	/**
	 * Called from the XML parser to set the name of the annotation preset
	 */
	public void setName(String name) {
		putValue(Action.NAME, name);
		putValue("toolbar", "annotation_"+name);
	}

	/**
	 * Called from the XML parser to set the icon
	 */
	public void setIcon(String icon) {
		putValue(Action.SMALL_ICON, new ImageIcon(new ImageIcon(icon).getImage().getScaledInstance(24, 24, Image.SCALE_SMOOTH)));
	}
	
	/**
	 * Called from the XML parser to set the types, this preset affects
	 */
	public void setType(String types) throws SAXException {
		try {
			for (String type : types.split(",")) {
				type = Character.toUpperCase(type.charAt(0))+type.substring(1);
				if (this.types == null)
					this.types = new LinkedList<Class<?>>();
				this.types.add(Class.forName("org.openstreetmap.josm.data.osm."+type));
			}
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			throw new SAXException(tr("Unknown type"));
		}
	}
	
	public static List<AnnotationPreset> readAll(InputStream inStream) throws SAXException {
		BufferedReader in = null;
		try {
			in = new BufferedReader(new InputStreamReader(inStream, "UTF-8"));
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			in = new BufferedReader(new InputStreamReader(inStream));
		}
		XmlObjectParser parser = new XmlObjectParser();
		parser.mapOnStart("item", AnnotationPreset.class);
		parser.map("text", Text.class);
		parser.map("check", Check.class);
		parser.map("combo", Combo.class);
		parser.map("label", Label.class);
		parser.map("key", Key.class);
		LinkedList<AnnotationPreset> all = new LinkedList<AnnotationPreset>();
		parser.start(in);
		while(parser.hasNext()) {
			Object o = parser.next();
			if (o instanceof AnnotationPreset) {
				all.add((AnnotationPreset)o);
				Main.toolbar.register((AnnotationPreset)o);
			} else
				all.getLast().data.add((Item)o);
		}
		return all;
	}

	public static Collection<AnnotationPreset> readFromPreferences() {
		LinkedList<AnnotationPreset> allPresets = new LinkedList<AnnotationPreset>();
		String allAnnotations = Main.pref.get("annotation.sources");
		StringTokenizer st = new StringTokenizer(allAnnotations, ";");
		while (st.hasMoreTokens()) {
			InputStream in = null;
			String source = st.nextToken();
			try {
				if (source.startsWith("http") || source.startsWith("ftp") || source.startsWith("file"))
					in = new URL(source).openStream();
				else if (source.startsWith("resource://"))
					in = Main.class.getResourceAsStream(source.substring("resource:/".length()));
				else
					in = new FileInputStream(source);
				allPresets.addAll(AnnotationPreset.readAll(in));
				in.close();
			} catch (IOException e) {
				e.printStackTrace();
				JOptionPane.showMessageDialog(Main.parent, tr("Could not read annotation preset source: {0}",source));
			} catch (SAXException e) {
				e.printStackTrace();
				JOptionPane.showMessageDialog(Main.parent, tr("Error parsing {0}: ", source)+e.getMessage());
			}
		}
		return allPresets;
	}


	public JPanel createPanel() {
		if (data == null)
			return null;
		JPanel p = new JPanel(new GridBagLayout());
		for (Item i : data)
			i.addToPanel(p);
		return p;
	}

	public void actionPerformed(ActionEvent e) {
		Collection<OsmPrimitive> sel = Main.ds.getSelected();
		JPanel p = createPanel();
		if (p == null)
			return;
		int answer;
		if (p.getComponentCount() == 0)
			answer = JOptionPane.OK_OPTION;
		else
			answer = JOptionPane.showConfirmDialog(Main.parent, p, trn("Change {0} object", "Change {0} objects", sel.size(), sel.size()), JOptionPane.OK_CANCEL_OPTION);
		if (answer == JOptionPane.OK_OPTION) {
			Command cmd = createCommand(Main.ds.getSelected());
			if (cmd != null)
				Main.main.editLayer().add(cmd);
		}
		Main.ds.setSelected(Main.ds.getSelected()); // force update
	}

	private Command createCommand(Collection<OsmPrimitive> participants) {
		Collection<OsmPrimitive> sel = new LinkedList<OsmPrimitive>();
		for (OsmPrimitive osm : participants)
			if (types == null || types.contains(osm.getClass()))
				sel.add(osm);
		if (sel.isEmpty())
			return null;

		List<Command> cmds = new LinkedList<Command>();
		for (Item i : data)
			i.addCommands(sel, cmds);
		if (cmds.size() == 0)
			return null;
		else if (cmds.size() == 1)
			return cmds.get(0);
		else
			return new SequenceCommand(tr("Change Properties"), cmds);
	}
}
