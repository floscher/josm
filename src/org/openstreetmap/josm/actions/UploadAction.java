package org.openstreetmap.josm.actions;

import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.Collection;
import java.util.LinkedList;

import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;

import org.jdom.JDOMException;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.gui.GBC;
import org.openstreetmap.josm.gui.OsmPrimitivRenderer;
import org.openstreetmap.josm.io.OsmServerWriter;

/**
 * Action that opens a connection to the osm server and upload all changes.
 * 
 * An dialog is displayed asking the user to specify a rectangle to grab.
 * The url and account settings from the preferences are used.
 *  
 * @author imi
 */
public class UploadAction extends JosmAction {

	public UploadAction() {
		super("Upload to OSM", "upload", "Upload all changes to the OSM server.", KeyEvent.VK_U, 
				KeyStroke.getAWTKeyStroke(KeyEvent.VK_U, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK));
	}

	public void actionPerformed(ActionEvent e) {
		Collection<OsmPrimitive> add = new LinkedList<OsmPrimitive>();
		Collection<OsmPrimitive> update = new LinkedList<OsmPrimitive>();
		Collection<OsmPrimitive> delete = new LinkedList<OsmPrimitive>();
		for (OsmPrimitive osm : Main.main.ds.nodes)
			if (osm.id == 0)
				add.add(osm);
			else if (osm.modified)
				update.add(osm);
		for (OsmPrimitive osm : Main.main.ds.lineSegments)
			if (osm.id == 0)
				add.add(osm);
			else if (osm.modified)
				update.add(osm);
		for (OsmPrimitive osm : Main.main.ds.tracks)
			if (osm.id == 0)
				add.add(osm);
			else if (osm.modified)
				update.add(osm);
		for (OsmPrimitive osm : Main.main.ds.deleted)
			if (osm.id != 0)
				delete.add(osm);

		if (!displayUploadScreen(add, update, delete))
			return;
		
		OsmServerWriter server = new OsmServerWriter();
		try {
			Collection<OsmPrimitive> all = new LinkedList<OsmPrimitive>();
			all.addAll(add);
			all.addAll(update);
			all.addAll(delete);
			server.uploadOsm(all);
		} catch (JDOMException x) {
			x.printStackTrace();
			JOptionPane.showMessageDialog(Main.main, x.getMessage());
		}
	}
	
	/**
	 * Displays a screen where the actions that would be taken are displayed and
	 * give the user the possibility to cancel the upload.
	 * @return <code>true</code>, if the upload should continue. <code>false</code>
	 * 			if the user requested cancel.
	 */
	private boolean displayUploadScreen(Collection<OsmPrimitive> add, Collection<OsmPrimitive> update, Collection<OsmPrimitive> delete) {
		if (add.isEmpty() && update.isEmpty() && delete.isEmpty()) {
			JOptionPane.showMessageDialog(Main.main, "No changes to upload.");
			return false;
		}
		
		JPanel p = new JPanel(new GridBagLayout());

		OsmPrimitivRenderer renderer = new OsmPrimitivRenderer();

		if (!add.isEmpty()) {
			p.add(new JLabel("Objects to add:"), GBC.eol());
			JList l = new JList(add.toArray());
			l.setCellRenderer(renderer);
			l.setVisibleRowCount(l.getModel().getSize() < 6 ? l.getModel().getSize() : 10);
			p.add(new JScrollPane(l), GBC.eol().fill());
		}

		if (!update.isEmpty()) {
			p.add(new JLabel("Objects to modify:"), GBC.eol());
			JList l = new JList(update.toArray());
			l.setCellRenderer(renderer);
			l.setVisibleRowCount(l.getModel().getSize() < 6 ? l.getModel().getSize() : 10);
			p.add(new JScrollPane(l), GBC.eol().fill());
		}

		if (!delete.isEmpty()) {
			p.add(new JLabel("Objects to delete:"), GBC.eol());
			JList l = new JList(delete.toArray());
			l.setCellRenderer(renderer);
			l.setVisibleRowCount(l.getModel().getSize() < 6 ? l.getModel().getSize() : 10);
			p.add(new JScrollPane(l), GBC.eol().fill());
		}

		return JOptionPane.showConfirmDialog(Main.main, p, "Upload this changes?", 
				JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION;
	}
}
