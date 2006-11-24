package org.openstreetmap.josm.gui.annotation;

import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trn;

import java.awt.GridBagLayout;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.openstreetmap.josm.command.ChangePropertyCommand;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.command.SequenceCommand;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.tools.GBC;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import uk.co.wilson.xml.MinML2;


/**
 * This class read encapsulate one annotation preset. A class method can
 * read in all predefined presets, either shipped with JOSM or that are
 * in the config directory.
 * 
 * It is also able to construct dialogs out of preset definitions.
 */
public class AnnotationPreset {

	private static interface Item {
		void addToPanel(JPanel p);
		void addCommands(Collection<OsmPrimitive> sel, List<Command> cmds);
	}

	public static class Text implements Item {
		private String key;
		private String label;
		private JTextField value = new JTextField();
		private boolean deleteIfEmpty;

		public void addToPanel(JPanel p) {
			p.add(new JLabel(label), GBC.std().insets(0,0,10,0));
			p.add(value, GBC.eol().fill(GBC.HORIZONTAL));
		}
		public Text(String key, String label, String value, boolean deleteIfEmpty) {
			this.key = key;
			this.label = label;
			this.value.setText(value == null ? "" : value);
			this.deleteIfEmpty = deleteIfEmpty;
		}
		public void addCommands(Collection<OsmPrimitive> sel, List<Command> cmds) {
			String v = value.getText();
			if (deleteIfEmpty && v.length() == 0)
				v = null;
			cmds.add(new ChangePropertyCommand(sel, key, v));
		}
	}

	public static class Check implements Item {
		private String key;
		private JCheckBox check = new JCheckBox();

		public void addToPanel(JPanel p) {
			p.add(check, GBC.eol().fill(GBC.HORIZONTAL));
		}
		public Check(String key, String label, boolean check) {
			this.key = key;
			this.check.setText(label);
			this.check.setSelected(check);
		}
		public void addCommands(Collection<OsmPrimitive> sel, List<Command> cmds) {
			cmds.add(new ChangePropertyCommand(sel, key, check.isSelected() ? "true" : null));
		}
	}

	public static class Combo implements Item {
		private String key;
		private String label;
		private JComboBox combo;
		private final String[] values;
		private boolean deleteIfEmpty;

		public void addToPanel(JPanel p) {
			p.add(new JLabel(label), GBC.std().insets(0,0,10,0));
			p.add(combo, GBC.eol().fill(GBC.HORIZONTAL));
		}
		public Combo(String key, String label, String def, String[] values, String[] displayedValues, boolean editable, boolean deleteIfEmpty) {
			this.key = key;
			this.label = label;
			this.values = values;
			this.deleteIfEmpty = deleteIfEmpty;
			combo = new JComboBox(displayedValues);
			combo.setEditable(editable);
			combo.setSelectedItem(def);
		}
		public void addCommands(Collection<OsmPrimitive> sel, List<Command> cmds) {
			String v = combo.getSelectedIndex() == -1 ? null : values[combo.getSelectedIndex()];
			String str = combo.isEditable()?combo.getEditor().getItem().toString() : v;
			if (deleteIfEmpty && str != null && str.length() == 0)
				str = null;
			cmds.add(new ChangePropertyCommand(sel, key, str));
		}
	}

	public static class Label implements Item {
		private String text;

		public void addToPanel(JPanel p) {
			p.add(new JLabel(text), GBC.eol());
		}
		public Label(String text) {
			this.text = text;
		}
		public void addCommands(Collection<OsmPrimitive> sel, List<Command> cmds) {}
	}

	public static class Key implements Item {
		private String key;
		private String value;

		public void addToPanel(JPanel p) {}
		public Key(String key, String value) {
			this.key = key;
			this.value = value;
		}
		public void addCommands(Collection<OsmPrimitive> sel, List<Command> cmds) {
			cmds.add(new ChangePropertyCommand(sel, key, value != null && !value.equals("") ? value : null));
		}
	}

	private static class Parser extends MinML2 {
		List<AnnotationPreset> data = new LinkedList<AnnotationPreset>();
		List<Item> current;
		String currentName;
		Collection<Class<?>> currentTypes;
		private static int unknownCounter = 1;

		@Override public void startElement(String ns, String lname, String qname, Attributes a) throws SAXException {
			if (qname.equals("annotations"))
				return;
			if (qname.equals("item")) {
				current = new LinkedList<Item>();
				currentName = a.getValue("name");
				if (currentName == null)
					currentName = "Unnamed Preset #"+(unknownCounter++);
				if (a.getValue("type") != null) {
					String s = a.getValue("type");
					try {
						for (String type : s.split(",")) {
							type = Character.toUpperCase(type.charAt(0))+type.substring(1);
							if (currentTypes == null)
								currentTypes = new LinkedList<Class<?>>();
							currentTypes.add(Class.forName("org.openstreetmap.josm.data.osm."+type));
						}
					} catch (ClassNotFoundException e) {
						e.printStackTrace();
						throw new SAXException(tr("Unknown type at line {0}", getLineNumber()));
					}
				}
			} else if (qname.equals("text"))
				current.add(new Text(a.getValue("key"), a.getValue("text"), a.getValue("default"), parseBoolean(a.getValue("delete_if_empty"))));
			else if (qname.equals("check")) {
				String s = a.getValue("default");
				boolean clear = parseBoolean(s);
				current.add(new Check(a.getValue("key"), a.getValue("text"), !clear));
			} else if (qname.equals("label"))
				current.add(new Label(a.getValue("text")));
			else if (qname.equals("combo")) {
				String[] values = a.getValue("values").split(",");
				String s = a.getValue("readonly");
				String dvstr = a.getValue("display_values");
				boolean editable = parseBoolean(s);
				if (dvstr != null) {
					if (editable && s != null)
						throw new SAXException(tr("Cannot have a writable combobox with default values (line {0})", getLineNumber()));
					editable = false; // for combos with display_value readonly default to false
				}
				String[] displayValues = dvstr == null ? values : dvstr.split(",");
				if (displayValues.length != values.length)
					throw new SAXException(tr("display_values ({0}) and values ({1}) must be of same number of elements.",
							displayValues.length+" "+trn("element", "elements", displayValues.length),
							values.length+" "+trn("element", "elements", values.length)));
				current.add(new Combo(a.getValue("key"), a.getValue("text"), a.getValue("default"), values, displayValues, editable, parseBoolean(a.getValue("delete_if_empty"))));
			} else if (qname.equals("key"))
				current.add(new Key(a.getValue("key"), a.getValue("value")));
			else
				throw new SAXException(tr("Unknown annotation object {0} at line {1} column {2}", qname, getLineNumber(), getColumnNumber()));
		}

		private boolean parseBoolean(String s) {
	        return s == null || s.equals("0") || s.startsWith("off") || s.startsWith("false") || s.startsWith("no");
        }

		@Override public void endElement(String ns, String lname, String qname) {
			if (qname.equals("item")) {
				data.add(new AnnotationPreset(current, currentName, currentTypes));
				current = null;
				currentName = null;
				currentTypes = null;
			}
		}
	}

	private List<Item> data;
	public String name;
	public Collection<Class<?>> types;

	public AnnotationPreset(List<Item> data, String name, Collection<Class<?>> currentTypes) {
		this.data = data;
		this.name = name;
		this.types = currentTypes;
	}

	/**
	 * Create an empty annotation preset. This will not have any items and
	 * will be an empty string as text. createPanel will return null.
	 * Use this as default item for "do not select anything".
	 */
	public AnnotationPreset() {}

	public static List<AnnotationPreset> readAll(InputStream inStream) throws IOException, SAXException {
		BufferedReader in = null;
		try {
			in = new BufferedReader(new InputStreamReader(inStream, "UTF-8"));
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			in = new BufferedReader(new InputStreamReader(inStream));
		}
		Parser p = new Parser();
		p.parse(in);
		return p.data;
	}

	public JPanel createPanel() {
		if (data == null)
			return null;
		JPanel p = new JPanel(new GridBagLayout());
		for (Item i : data)
			i.addToPanel(p);
		return p;
	}

	public Command createCommand(Collection<OsmPrimitive> participants) {
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
