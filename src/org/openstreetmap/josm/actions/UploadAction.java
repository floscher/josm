package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.tools.I18n.tr;

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

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.gui.OsmPrimitivRenderer;
import org.openstreetmap.josm.gui.PleaseWaitRunnable;
import org.openstreetmap.josm.io.OsmServerWriter;
import org.openstreetmap.josm.tools.GBC;
import org.xml.sax.SAXException;

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
		super(tr("Upload to OSM"), "upload", tr("Upload all changes to the OSM server."), KeyEvent.VK_U, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK);
	}

	public void actionPerformed(ActionEvent e) {
		if (Main.map == null) {
			JOptionPane.showMessageDialog(Main.parent,tr("Nothing to upload. Get some data first."));
			return;
		}

		if (!Main.map.conflictDialog.conflicts.isEmpty()) {
			JOptionPane.showMessageDialog(Main.parent,tr("There are unresolved conflicts. You have to resolve these first."));
			Main.map.conflictDialog.action.button.setSelected(true);
			Main.map.conflictDialog.action.actionPerformed(null);
			return;
		}

		final Collection<OsmPrimitive> add = new LinkedList<OsmPrimitive>();
		final Collection<OsmPrimitive> update = new LinkedList<OsmPrimitive>();
		final Collection<OsmPrimitive> delete = new LinkedList<OsmPrimitive>();
		for (OsmPrimitive osm : Main.ds.allPrimitives()) {
			if (osm.get("josm/ignore") != null)
				continue;
			if (osm.id == 0 && !osm.deleted)
				add.add(osm);
			else if (osm.modified && !osm.deleted)
				update.add(osm);
			else if (osm.deleted && osm.id != 0)
				delete.add(osm);
		}

		if (!displayUploadScreen(add, update, delete))
			return;

		final OsmServerWriter server = new OsmServerWriter();
		final Collection<OsmPrimitive> all = new LinkedList<OsmPrimitive>();
		all.addAll(add);
		all.addAll(update);
		all.addAll(delete);

		PleaseWaitRunnable uploadTask = new PleaseWaitRunnable(tr("Uploading data")){
			@Override protected void realRun() throws SAXException {
				server.setProgressInformation(currentAction, progress);
				server.uploadOsm(all);
			}
			@Override protected void finish() {
				Main.main.editLayer().cleanData(server.processed, !add.isEmpty());
			}
			@Override protected void cancel() {
				server.cancel();
			}
		};
		Main.worker.execute(uploadTask);
		uploadTask.pleaseWaitDlg.setVisible(true);
	}

	/**
	 * Displays a screen where the actions that would be taken are displayed and
	 * give the user the possibility to cancel the upload.
	 * @return <code>true</code>, if the upload should continue. <code>false</code>
	 * 			if the user requested cancel.
	 */
	private boolean displayUploadScreen(Collection<OsmPrimitive> add, Collection<OsmPrimitive> update, Collection<OsmPrimitive> delete) {
		if (add.isEmpty() && update.isEmpty() && delete.isEmpty()) {
			JOptionPane.showMessageDialog(Main.parent,tr("No changes to upload."));
			return false;
		}

		JPanel p = new JPanel(new GridBagLayout());

		OsmPrimitivRenderer renderer = new OsmPrimitivRenderer();

		if (!add.isEmpty()) {
			p.add(new JLabel(tr("Objects to add:")), GBC.eol());
			JList l = new JList(add.toArray());
			l.setCellRenderer(renderer);
			l.setVisibleRowCount(l.getModel().getSize() < 6 ? l.getModel().getSize() : 10);
			p.add(new JScrollPane(l), GBC.eol().fill());
		}

		if (!update.isEmpty()) {
			p.add(new JLabel(tr("Objects to modify:")), GBC.eol());
			JList l = new JList(update.toArray());
			l.setCellRenderer(renderer);
			l.setVisibleRowCount(l.getModel().getSize() < 6 ? l.getModel().getSize() : 10);
			p.add(new JScrollPane(l), GBC.eol().fill());
		}

		if (!delete.isEmpty()) {
			p.add(new JLabel(tr("Objects to delete:")), GBC.eol());
			JList l = new JList(delete.toArray());
			l.setCellRenderer(renderer);
			l.setVisibleRowCount(l.getModel().getSize() < 6 ? l.getModel().getSize() : 10);
			p.add(new JScrollPane(l), GBC.eol().fill());
		}

		return JOptionPane.showConfirmDialog(Main.parent, p, tr("Upload this changes?"), 
				JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION;
	}
}
