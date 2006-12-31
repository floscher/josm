package org.openstreetmap.josm.gui;

import static org.openstreetmap.josm.tools.I18n.tr;

import javax.swing.Action;
import javax.swing.Box;
import javax.swing.JMenu;
import javax.swing.JMenuBar;

import org.openstreetmap.josm.actions.AboutAction;
import org.openstreetmap.josm.actions.AlignInCircleAction;
import org.openstreetmap.josm.actions.AlignInLineAction;
import org.openstreetmap.josm.actions.DownloadAction;
import org.openstreetmap.josm.actions.DownloadIncompleteAction;
import org.openstreetmap.josm.actions.ExitAction;
import org.openstreetmap.josm.actions.GpxExportAction;
import org.openstreetmap.josm.actions.HelpAction;
import org.openstreetmap.josm.actions.NewAction;
import org.openstreetmap.josm.actions.OpenAction;
import org.openstreetmap.josm.actions.PreferencesAction;
import org.openstreetmap.josm.actions.RedoAction;
import org.openstreetmap.josm.actions.ReorderAction;
import org.openstreetmap.josm.actions.ReverseSegmentAction;
import org.openstreetmap.josm.actions.SaveAction;
import org.openstreetmap.josm.actions.SaveAsAction;
import org.openstreetmap.josm.actions.SelectAllAction;
import org.openstreetmap.josm.actions.UndoAction;
import org.openstreetmap.josm.actions.UnselectAllAction;
import org.openstreetmap.josm.actions.UploadAction;

/**
 * This is the JOSM main menu bar. It is overwritten to initialize itself and provide
 * all menu entries as member variables (sort of collect them).
 *
 * It also provides possibilities to attach new menu entries (used by plugins).
 *
 * @author Immanuel.Scholz
 */
public class MainMenu extends JMenuBar {

	public final UndoAction undo = new UndoAction();
	public final RedoAction redo = new RedoAction();
	public final Action selectAll = new SelectAllAction();
	public final Action unselectAll = new UnselectAllAction();
	public final NewAction newAction = new NewAction();
	public final OpenAction open = new OpenAction();
	public final DownloadAction download = new DownloadAction();
	public final Action reverseSegment = new ReverseSegmentAction();
	public final Action alignInCircle = new AlignInCircleAction();
	public final Action alignInLine = new AlignInLineAction();
	public final Action reorder = new ReorderAction();
	public final Action upload = new UploadAction();
	public final Action save = new SaveAction();
	public final Action saveAs = new SaveAsAction();
	public final Action gpxExport = new GpxExportAction(null);
	public final Action exit = new ExitAction();
	public final Action preferences = new PreferencesAction();
	public final HelpAction help = new HelpAction();
	public final Action about = new AboutAction();

	public final JMenu layerMenu = new JMenu(tr("Layer"));
	public final JMenu editMenu = new JMenu(tr("Edit"));
	public final JMenu helpMenu = new JMenu(tr("Help"));
	public final JMenu fileMenu = new JMenu(tr("Files"));
	public final JMenu connectionMenu = new JMenu(tr("Connection"));
	private DownloadIncompleteAction downloadIncomplete = new DownloadIncompleteAction();



	public MainMenu() {
		fileMenu.setMnemonic('F');
		fileMenu.add(newAction);
		fileMenu.add(open);
		fileMenu.add(save);
		fileMenu.add(saveAs);
		fileMenu.add(gpxExport);
		fileMenu.addSeparator();
		fileMenu.add(exit);
		add(fileMenu);

		editMenu.setMnemonic('E');
		editMenu.add(undo);
		editMenu.add(redo);
		editMenu.addSeparator();
		editMenu.add(selectAll);
		editMenu.add(unselectAll);
		editMenu.addSeparator();
		editMenu.add(reverseSegment);
		editMenu.add(alignInCircle);
		editMenu.add(alignInLine);
		editMenu.add(reorder);
		editMenu.addSeparator();
		editMenu.add(preferences);
		add(editMenu);

		connectionMenu.setMnemonic('C');
		connectionMenu.add(download);
		connectionMenu.add(downloadIncomplete);
		connectionMenu.add(upload);
		add(connectionMenu);

		layerMenu.setMnemonic('L');
		add(layerMenu);
		layerMenu.setVisible(false);

		add(Box.createHorizontalGlue());

		helpMenu.setMnemonic('H');
		helpMenu.add(help);
		helpMenu.add(about);
		add(helpMenu);
    }
}
