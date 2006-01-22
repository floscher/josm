package org.openstreetmap.josm.actions;

import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.Collection;
import java.util.LinkedList;

import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;

import org.jdom.JDOMException;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Track;
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
		boolean acceptedTracks = false;
		for (OsmPrimitive osm : Main.main.ds.allPrimitives()) {
			boolean doSomething = true;
			if (osm.id == 0 && !osm.isDeleted())
				add.add(osm);
			else if ((osm.modified || osm.modifiedProperties) && !osm.isDeleted())
				update.add(osm);
			else if (osm.isDeleted() && osm.id != 0)
				delete.add(osm);
			else
				doSomething = false;

			if (osm instanceof Track && doSomething && !acceptedTracks) {
				int answer = JOptionPane.showConfirmDialog(Main.main, 
						"The server currently does not understand the concept of Tracks.\n" +
						"All tracks will be ignored on upload. Continue anyway?",
						"No Track support", JOptionPane.YES_NO_OPTION);
				if (answer != JOptionPane.YES_OPTION)
					return;
				acceptedTracks = true;
			}
		}

		if (!displayUploadScreen(add, update, delete))
			return;

		final OsmServerWriter server = new OsmServerWriter();
		final Collection<OsmPrimitive> all = new LinkedList<OsmPrimitive>();
		all.addAll(add);
		all.addAll(update);
		all.addAll(delete);
		
		final JDialog dlg = createPleaseWaitDialog("Uploading data");
		new Thread(){
			@Override
			public void run() {
				try {
					server.uploadOsm(all);
				} catch (JDOMException x) {
					dlg.setVisible(false);
					x.printStackTrace();
					JOptionPane.showMessageDialog(Main.main, x.getMessage());
				} finally {
					dlg.setVisible(false);
				}
			}
		}.start();
		
		dlg.setVisible(true);
		
		// finished without errors -> clean dataset
		Main.main.getMapFrame().mapView.editLayer().cleanData();
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
