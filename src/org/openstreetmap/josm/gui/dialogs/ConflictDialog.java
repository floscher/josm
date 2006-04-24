package org.openstreetmap.josm.gui.dialogs;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.command.ConflictResolveCommand;
import org.openstreetmap.josm.data.SelectionChangedListener;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Segment;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.visitor.SimplePaintVisitor;
import org.openstreetmap.josm.data.osm.visitor.Visitor;
import org.openstreetmap.josm.gui.ConflictResolver;
import org.openstreetmap.josm.gui.NavigatableComponent;
import org.openstreetmap.josm.gui.OsmPrimitivRenderer;
import org.openstreetmap.josm.tools.ImageProvider;

public final class ConflictDialog extends ToggleDialog {

	public final Map<OsmPrimitive, OsmPrimitive> conflicts = new HashMap<OsmPrimitive, OsmPrimitive>();
	private final DefaultListModel model = new DefaultListModel();
	private final JList displaylist = new JList(model);

	public ConflictDialog() {
		super("Conflict", "conflict", "Merging conflicts.", KeyEvent.VK_C);
		displaylist.setCellRenderer(new OsmPrimitivRenderer());
		displaylist.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		displaylist.addMouseListener(new MouseAdapter(){
			@Override public void mouseClicked(MouseEvent e) {
				if (e.getClickCount() >= 2)
					resolve();
			}
		});
		add(new JScrollPane(displaylist), BorderLayout.CENTER);

		JPanel buttonPanel = new JPanel(new GridLayout(1,2));
		JButton button = new JButton("Resolve", ImageProvider.get("dialogs", "conflict"));
		button.setToolTipText("Open a merge dialog of all selected items in the list above.");
		button.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				resolve();
			}
		});
		buttonPanel.add(button);

		button = new JButton("Select", ImageProvider.get("mapmode", "selection"));
		button.setToolTipText("Set the selected elements on the map to the selected items in the list above.");
		button.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				Collection<OsmPrimitive> sel = new LinkedList<OsmPrimitive>();
				for (Object o : displaylist.getSelectedValues())
					sel.add((OsmPrimitive)o);
				Main.ds.setSelected(sel);
			}
		});
		buttonPanel.add(button);

		add(buttonPanel, BorderLayout.SOUTH);

		Main.ds.addSelectionChangedListener(new SelectionChangedListener(){
			public void selectionChanged(Collection<? extends OsmPrimitive> newSelection) {
				displaylist.clearSelection();
				for (OsmPrimitive osm : newSelection) {
					if (conflicts.containsKey(osm)) {
						int pos = model.indexOf(osm);
						displaylist.addSelectionInterval(pos, pos);
					}
				}
			}
		});
		displaylist.getSelectionModel().addListSelectionListener(new ListSelectionListener(){
			public void valueChanged(ListSelectionEvent e) {
				Main.map.mapView.repaint();
			}
		});
	}

	private final void resolve() {
		if (displaylist.getSelectedIndex() == -1) {
			JOptionPane.showMessageDialog(Main.parent, "Please select something from the conflict list.");
			return;
		}
		Map<OsmPrimitive, OsmPrimitive> sel = new HashMap<OsmPrimitive, OsmPrimitive>();
		for (int i : displaylist.getSelectedIndices()) {
			OsmPrimitive s = (OsmPrimitive)model.get(i);
			sel.put(s, conflicts.get(s));
		}
		ConflictResolver resolver = new ConflictResolver(sel);
		int answer = JOptionPane.showConfirmDialog(Main.parent, resolver, "Resolve Conflicts", JOptionPane.OK_CANCEL_OPTION);
		if (answer != JOptionPane.OK_OPTION)
			return;
		Main.main.editLayer().add(new ConflictResolveCommand(resolver.conflicts, sel));
		Main.map.mapView.repaint();
	}

	public final void rebuildList() {
		model.removeAllElements();
		for (OsmPrimitive osm : this.conflicts.keySet())
			if (osm instanceof Node)
				model.addElement(osm);
		for (OsmPrimitive osm : this.conflicts.keySet())
			if (osm instanceof Segment)
				model.addElement(osm);
		for (OsmPrimitive osm : this.conflicts.keySet())
			if (osm instanceof Way)
				model.addElement(osm);
	}
	
	public final void add(Map<OsmPrimitive, OsmPrimitive> conflicts) {
		this.conflicts.putAll(conflicts);
		rebuildList();
	}

	/**
	 * Paint all conflicts that can be expressed on the main window. 
	 */
	public void paintConflicts(final Graphics g, final NavigatableComponent nc) {
		Color preferencesColor = SimplePaintVisitor.getPreferencesColor("conflict", Color.GRAY);
		if (preferencesColor.equals(Color.BLACK))
			return;
		g.setColor(preferencesColor);
		Visitor conflictPainter = new Visitor(){
			public void visit(Node n) {
				Point p = nc.getPoint(n.eastNorth);
				g.drawRect(p.x-1, p.y-1, 2, 2);
			}
			public void visit(Segment ls) {
				if (ls.incomplete)
					return;
				Point p1 = nc.getPoint(ls.from.eastNorth);
				Point p2 = nc.getPoint(ls.to.eastNorth);
				g.drawLine(p1.x, p1.y, p2.x, p2.y);
			}
			public void visit(Way w) {
				for (Segment ls : w.segments)
					visit(ls);
			}
		};
		for (Object o : displaylist.getSelectedValues())
			conflicts.get(o).visit(conflictPainter);
	}
}
