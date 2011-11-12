// License: GPL. See LICENSE file for details.
package org.openstreetmap.josm.gui;

import static org.openstreetmap.josm.gui.help.HelpUtil.ht;
import static org.openstreetmap.josm.tools.I18n.marktr;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.KeyEvent;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;
import javax.swing.KeyStroke;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.AboutAction;
import org.openstreetmap.josm.actions.AddNodeAction;
import org.openstreetmap.josm.actions.AlignInCircleAction;
import org.openstreetmap.josm.actions.AlignInLineAction;
import org.openstreetmap.josm.actions.AutoScaleAction;
import org.openstreetmap.josm.actions.ChangesetManagerToggleAction;
import org.openstreetmap.josm.actions.CloseChangesetAction;
import org.openstreetmap.josm.actions.CombineWayAction;
import org.openstreetmap.josm.actions.CopyAction;
import org.openstreetmap.josm.actions.CopyCoordinatesAction;
import org.openstreetmap.josm.actions.CreateCircleAction;
import org.openstreetmap.josm.actions.CreateMultipolygonAction;
import org.openstreetmap.josm.actions.DeleteAction;
import org.openstreetmap.josm.actions.DistributeAction;
import org.openstreetmap.josm.actions.DownloadAction;
import org.openstreetmap.josm.actions.DownloadPrimitiveAction;
import org.openstreetmap.josm.actions.DownloadReferrersAction;
import org.openstreetmap.josm.actions.DuplicateAction;
import org.openstreetmap.josm.actions.ExitAction;
import org.openstreetmap.josm.actions.FollowLineAction;
import org.openstreetmap.josm.actions.FullscreenToggleAction;
import org.openstreetmap.josm.actions.GpxExportAction;
import org.openstreetmap.josm.actions.HelpAction;
import org.openstreetmap.josm.actions.HistoryInfoAction;
import org.openstreetmap.josm.actions.HistoryInfoWebAction;
import org.openstreetmap.josm.actions.InfoAction;
import org.openstreetmap.josm.actions.InfoWebAction;
import org.openstreetmap.josm.actions.JoinAreasAction;
import org.openstreetmap.josm.actions.JoinNodeWayAction;
import org.openstreetmap.josm.actions.JosmAction;
import org.openstreetmap.josm.actions.JumpToAction;
import org.openstreetmap.josm.actions.MergeLayerAction;
import org.openstreetmap.josm.actions.MergeNodesAction;
import org.openstreetmap.josm.actions.MergeSelectionAction;
import org.openstreetmap.josm.actions.MirrorAction;
import org.openstreetmap.josm.actions.MoveAction;
import org.openstreetmap.josm.actions.MoveNodeAction;
import org.openstreetmap.josm.actions.NewAction;
import org.openstreetmap.josm.actions.OpenFileAction;
import org.openstreetmap.josm.actions.OpenLocationAction;
import org.openstreetmap.josm.actions.OrthogonalizeAction;
import org.openstreetmap.josm.actions.PasteAction;
import org.openstreetmap.josm.actions.PasteTagsAction;
import org.openstreetmap.josm.actions.PreferencesAction;
import org.openstreetmap.josm.actions.PurgeAction;
import org.openstreetmap.josm.actions.RedoAction;
import org.openstreetmap.josm.actions.ReverseWayAction;
import org.openstreetmap.josm.actions.SaveAction;
import org.openstreetmap.josm.actions.SaveAsAction;
import org.openstreetmap.josm.actions.SelectAllAction;
import org.openstreetmap.josm.actions.ShowStatusReportAction;
import org.openstreetmap.josm.actions.SimplifyWayAction;
import org.openstreetmap.josm.actions.SplitWayAction;
import org.openstreetmap.josm.actions.ToggleGPXLinesAction;
import org.openstreetmap.josm.actions.UnGlueAction;
import org.openstreetmap.josm.actions.UndoAction;
import org.openstreetmap.josm.actions.UnselectAllAction;
import org.openstreetmap.josm.actions.UpdateDataAction;
import org.openstreetmap.josm.actions.UpdateModifiedAction;
import org.openstreetmap.josm.actions.UpdateSelectionAction;
import org.openstreetmap.josm.actions.UploadAction;
import org.openstreetmap.josm.actions.UploadSelectionAction;
import org.openstreetmap.josm.actions.ViewportFollowToggleAction;
import org.openstreetmap.josm.actions.WireframeToggleAction;
import org.openstreetmap.josm.actions.ZoomInAction;
import org.openstreetmap.josm.actions.ZoomOutAction;
import org.openstreetmap.josm.actions.OrthogonalizeAction.Undo;
import org.openstreetmap.josm.actions.audio.AudioBackAction;
import org.openstreetmap.josm.actions.audio.AudioFasterAction;
import org.openstreetmap.josm.actions.audio.AudioFwdAction;
import org.openstreetmap.josm.actions.audio.AudioNextAction;
import org.openstreetmap.josm.actions.audio.AudioPlayPauseAction;
import org.openstreetmap.josm.actions.audio.AudioPrevAction;
import org.openstreetmap.josm.actions.audio.AudioSlowerAction;
import org.openstreetmap.josm.actions.search.SearchAction;
import org.openstreetmap.josm.gui.io.RecentlyOpenedFilesMenu;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.tagging.TaggingPresetSearchAction;
import org.openstreetmap.josm.tools.Shortcut;

/**
 * This is the JOSM main menu bar. It is overwritten to initialize itself and provide all menu
 * entries as member variables (sort of collect them).
 *
 * It also provides possibilities to attach new menu entries (used by plugins).
 *
 * @author Immanuel.Scholz
 */
public class MainMenu extends JMenuBar {

    /* File menu */
    public final NewAction newAction = new NewAction();
    public final OpenFileAction openFile = new OpenFileAction();
    public final RecentlyOpenedFilesMenu recentlyOpened = new RecentlyOpenedFilesMenu();
    public final OpenLocationAction openLocation = new OpenLocationAction();
    public final JosmAction save = new SaveAction();
    public final JosmAction saveAs = new SaveAsAction();
    public final JosmAction gpxExport = new GpxExportAction();
    public final DownloadAction download = new DownloadAction();
    public final DownloadPrimitiveAction downloadPrimitive = new DownloadPrimitiveAction();
    public final DownloadReferrersAction downloadReferrers = new DownloadReferrersAction();
    public final CloseChangesetAction closeChangesetAction = new CloseChangesetAction();
    public final JosmAction update = new UpdateDataAction();
    public final JosmAction updateSelection = new UpdateSelectionAction();
    public final JosmAction updateModified = new UpdateModifiedAction();
    public final JosmAction upload = new UploadAction();
    public final JosmAction uploadSelection = new UploadSelectionAction();
    public final JosmAction exit = new ExitAction();

    /* Edit menu */
    public final UndoAction undo = new UndoAction();
    public final RedoAction redo = new RedoAction();
    public final JosmAction copy = new CopyAction();
    public final JosmAction copyCoordinates = new CopyCoordinatesAction();
    public final PasteAction paste = new PasteAction();
    public final JosmAction pasteTags = new PasteTagsAction();
    public final JosmAction duplicate = new DuplicateAction();
    public final JosmAction delete = new DeleteAction();
    public final JosmAction purge = new PurgeAction();
    public final JosmAction merge = new MergeLayerAction();
    public final JosmAction mergeSelected = new MergeSelectionAction();
    public final JosmAction selectAll = new SelectAllAction();
    public final JosmAction unselectAll = new UnselectAllAction();
    public final JosmAction search = new SearchAction();
    public final JosmAction preferences = new PreferencesAction();

    /* View menu */
    public final WireframeToggleAction wireFrameToggleAction = new WireframeToggleAction();
    public final JosmAction toggleGPXLines = new ToggleGPXLinesAction();
    public final InfoAction info = new InfoAction();
    public final InfoWebAction infoweb = new InfoWebAction();
    public final HistoryInfoAction historyinfo = new HistoryInfoAction();
    public final HistoryInfoWebAction historyinfoweb = new HistoryInfoWebAction();

    /* Tools menu */
    public final JosmAction splitWay = new SplitWayAction();
    public final JosmAction combineWay = new CombineWayAction();
    public final JosmAction reverseWay = new ReverseWayAction();
    public final JosmAction alignInCircle = new AlignInCircleAction();
    public final JosmAction alignInLine = new AlignInLineAction();
    public final JosmAction distribute = new DistributeAction();
    public final OrthogonalizeAction ortho = new OrthogonalizeAction();
    public final JosmAction orthoUndo = new Undo();  // action is not shown in the menu. Only triggered by shortcut
    public final JosmAction mirror = new MirrorAction();
    public final AddNodeAction addnode = new AddNodeAction();
    public final MoveNodeAction movenode = new MoveNodeAction();
    public final JosmAction createCircle = new CreateCircleAction();
    public final JosmAction mergeNodes = new MergeNodesAction();
    public final JosmAction joinNodeWay = new JoinNodeWayAction();
    public final JosmAction unglueNodes = new UnGlueAction();
    public final JosmAction simplifyWay = new SimplifyWayAction();
    public final JosmAction joinAreas = new JoinAreasAction();
    public final JosmAction createMultipolygon = new CreateMultipolygonAction();
    public final JosmAction followLine = new FollowLineAction();

    /* Audio menu */
    public final JosmAction audioPlayPause = new AudioPlayPauseAction();
    public final JosmAction audioNext = new AudioNextAction();
    public final JosmAction audioPrev = new AudioPrevAction();
    public final JosmAction audioFwd = new AudioFwdAction();
    public final JosmAction audioBack = new AudioBackAction();
    public final JosmAction audioFaster = new AudioFasterAction();
    public final JosmAction audioSlower = new AudioSlowerAction();

    /* Help menu */
    public final HelpAction help = new HelpAction();
    public final JosmAction about = new AboutAction();
    public final JosmAction statusreport = new ShowStatusReportAction();

    public final JMenu fileMenu = addMenu(marktr("File"), KeyEvent.VK_F, 0, ht("/Menu/File"));
    public final JMenu editMenu = addMenu(marktr("Edit"), KeyEvent.VK_E, 1, ht("/Menu/Edit"));
    public final JMenu viewMenu = addMenu(marktr("View"), KeyEvent.VK_V, 2, ht("/Menu/View"));
    public final JMenu toolsMenu = addMenu(marktr("Tools"), KeyEvent.VK_T, 3, ht("/Menu/Tools"));
    public final JMenu presetsMenu = addMenu(marktr("Presets"), KeyEvent.VK_P, 4, ht("/Menu/Presets"));
    public final ImageryMenu imageryMenu =
        (ImageryMenu)addMenu(new ImageryMenu(), marktr("Imagery"), KeyEvent.VK_I, 5, ht("/Menu/Imagery"));
    /** the window menu is split into several groups. The first is for windows that can be opened from
     * this menu any time, e.g. the changeset editor. The second group is for toggle dialogs and the third
     * group is for currently open windows that cannot be toggled, e.g. relation editors. It's recommended
     * to use WINDOW_MENU_GROUP to determine the group integer.
     */
    public final JMenu windowMenu = addMenu(marktr("Windows"), KeyEvent.VK_W, 6, ht("/Menu/Windows"));
    public static enum WINDOW_MENU_GROUP { ALWAYS, TOGGLE_DIALOG, VOLATILE }

    public JMenu audioMenu = null;
    public final JMenu helpMenu = addMenu(marktr("Help"), KeyEvent.VK_H, 7, ht("/Menu/Help"));
    public final int defaultMenuPos = 7;

    public final JosmAction moveUpAction = new MoveAction(MoveAction.Direction.UP);
    public final JosmAction moveDownAction = new MoveAction(MoveAction.Direction.DOWN);
    public final JosmAction moveLeftAction = new MoveAction(MoveAction.Direction.LEFT);
    public final JosmAction moveRightAction = new MoveAction(MoveAction.Direction.RIGHT);
    public final JumpToAction jumpToAct = new JumpToAction();

    public final TaggingPresetSearchAction presetSearchAction = new TaggingPresetSearchAction();
    public FullscreenToggleAction fullscreenToggleAction = null;

    /** this menu listener hides unnecessary JSeparators in a menu list but does not remove them.
     * If at a later time the separators are required, they will be made visible again. Intended
     * usage is make menus not look broken if separators are used to group the menu and some of
     * these groups are empty.
     */
    public final static MenuListener menuSeparatorHandler = new MenuListener() {
        @Override
        public void menuCanceled(MenuEvent arg0) {}
        @Override
        public void menuDeselected(MenuEvent arg0) {}
        @Override
        public void menuSelected(MenuEvent a) {
            if(!(a.getSource() instanceof JMenu))
                return;
            final JPopupMenu m = ((JMenu) a.getSource()).getPopupMenu();
            for(int i=0; i < m.getComponentCount()-1; i++) {
                if(!(m.getComponent(i) instanceof JSeparator)) {
                    continue;
                }
                // hide separator if the next menu item is one as well
                ((JSeparator) m.getComponent(i)).setVisible(!(m.getComponent(i+1) instanceof JSeparator));
            }
            // hide separator at the end of the menu
            if(m.getComponent(m.getComponentCount()-1) instanceof JSeparator) {
                ((JSeparator) m.getComponent(m.getComponentCount()-1)).setVisible(false);
            }
        }
    };

    /**
     * Add a JosmAction to a menu.
     *
     * This method handles all the shortcut handling. It also makes sure that actions that are
     * handled by the OS are not duplicated on the menu. Menu item will be added at the end of
     * the menu.
     * @param menu to add the action to
     * @param the action that should get a menu item
     */
    public static JMenuItem add(JMenu menu, JosmAction action) {
        if (action.getShortcut().getAutomatic())
            return null;
        JMenuItem menuitem = menu.add(action);
            KeyStroke ks = action.getShortcut().getKeyStroke();
            if (ks != null) {
                menuitem.setAccelerator(ks);
            }
        return menuitem;
    }

    /**
     * Add a JosmAction to a menu.
     *
     * This method handles all the shortcut handling. It also makes sure that actions that are
     * handled by the OS are not duplicated on the menu.
     * @param menu to add the action to
     * @param the action that should get a menu item
     * @param group the item should be added to. Groups are split by a separator.
     *        0 is the first group, -1 will add the item to the end.
     */
    public static <E extends Enum<E>> JMenuItem add(JMenu menu, JosmAction action, Enum<E> group) {
        if (action.getShortcut().getAutomatic())
            return null;
        int i = getInsertionIndexForGroup(menu, group.ordinal());
        JMenuItem menuitem = (JMenuItem) menu.add(new JMenuItem(action), i);
        KeyStroke ks = action.getShortcut().getKeyStroke();
        if (ks != null) {
            menuitem.setAccelerator(ks);
        }
        return menuitem;
    }

    /**
     * Add a JosmAction to a menu and automatically prints accelerator if available.
     * Also adds a checkbox that may be toggled.
     * @param menu to add the action to
     * @param the action that should get a menu item
     * @param group the item should be added to. Groups are split by a separator. Use
     *        one of the enums that are defined for some of the menus to tell in which
     *        group the item should go.
     */
    public static <E extends Enum<E>> JCheckBoxMenuItem addWithCheckbox(JMenu menu, JosmAction action, Enum<E> group) {
        int i = getInsertionIndexForGroup(menu, group.ordinal());
        final JCheckBoxMenuItem mi = (JCheckBoxMenuItem) menu.add(new JCheckBoxMenuItem(action), i);
        final KeyStroke ks = action.getShortcut().getKeyStroke();
        if (ks != null) {
            mi.setAccelerator(ks);
        }
        return mi;
    }

    /** finds the correct insertion index for a given group and adds separators if necessary */
    private static int getInsertionIndexForGroup(JMenu menu, int group) {
        if(group < 0)
            return -1;
        // look for separator that *ends* the group (or stop at end of menu)
        int i;
        for(i=0; i < menu.getItemCount() && group >= 0; i++) {
            if(menu.getItem(i) == null) {
                group--;
            }
        }
        // insert before separator that ends the group
        if(group < 0) {
            i--;
        }
        // not enough separators have been found, add them
        while(group > 0) {
            menu.addSeparator();
            group--;
            i++;
        }
        return i;
    }

    public JMenu addMenu(String name, int mnemonicKey, int position, String relativeHelpTopic) {
        return addMenu(new JMenu(tr(name)), name, mnemonicKey, position, relativeHelpTopic);
    }

    public JMenu addMenu(JMenu menu, String name, int mnemonicKey, int position, String relativeHelpTopic) {
        Shortcut.registerShortcut("menu:" + name, tr("Menu: {0}", tr(name)), mnemonicKey,
                Shortcut.GROUP_MNEMONIC).setMnemonic(menu);
        add(menu, position);
        menu.putClientProperty("help", relativeHelpTopic);
        return menu;
    }

    public MainMenu() {
        JMenuItem current;

        add(fileMenu, newAction);
        add(fileMenu, openFile);
        fileMenu.add(recentlyOpened);
        add(fileMenu, openLocation);
        fileMenu.addSeparator();
        add(fileMenu, save);
        add(fileMenu, saveAs);
        add(fileMenu, gpxExport);
        fileMenu.addSeparator();
        add(fileMenu, download);
        add(fileMenu, downloadPrimitive);
        add(fileMenu, downloadReferrers);
        add(fileMenu, update);
        add(fileMenu, updateSelection);
        add(fileMenu, updateModified);
        fileMenu.addSeparator();
        add(fileMenu, upload);
        add(fileMenu, uploadSelection);
        fileMenu.addSeparator();
        add(fileMenu, closeChangesetAction);
        fileMenu.addSeparator();
        add(fileMenu, exit);

        add(editMenu, undo);
        add(editMenu, redo);
        editMenu.addSeparator();
        add(editMenu, copy);
        add(editMenu, copyCoordinates);
        add(editMenu, paste);
        add(editMenu, pasteTags);
        add(editMenu, duplicate);
        add(editMenu, delete);
        add(editMenu, purge);
        editMenu.addSeparator();
        add(editMenu,merge);
        add(editMenu,mergeSelected);
        editMenu.addSeparator();
        add(editMenu, selectAll);
        add(editMenu, unselectAll);
        editMenu.addSeparator();
        add(editMenu, search);
        editMenu.addSeparator();
        add(editMenu, preferences);

        // -- wireframe toggle action
        final JCheckBoxMenuItem wireframe = new JCheckBoxMenuItem(wireFrameToggleAction);
        viewMenu.add(wireframe);
        wireframe.setAccelerator(wireFrameToggleAction.getShortcut().getKeyStroke());
        wireFrameToggleAction.addButtonModel(wireframe.getModel());

        viewMenu.addSeparator();
        add(viewMenu, new ZoomInAction());
        add(viewMenu, new ZoomOutAction());
        viewMenu.addSeparator();
        for (String mode : AutoScaleAction.MODES) {
            JosmAction autoScaleAction = new AutoScaleAction(mode);
            add(viewMenu, autoScaleAction);
        }

        // -- viewport follow toggle action
        ViewportFollowToggleAction viewportFollowToggleAction = new ViewportFollowToggleAction();
        final JCheckBoxMenuItem vft = new JCheckBoxMenuItem(viewportFollowToggleAction);
        viewMenu.add(vft);
        vft.setAccelerator(viewportFollowToggleAction.getShortcut().getKeyStroke());
        viewportFollowToggleAction.addButtonModel(vft.getModel());

        if(!Main.applet && Main.platform.canFullscreen()) {
            // -- fullscreen toggle action
            fullscreenToggleAction = new FullscreenToggleAction();
            final JCheckBoxMenuItem fullscreen = new JCheckBoxMenuItem(fullscreenToggleAction);
            viewMenu.addSeparator();
            viewMenu.add(fullscreen);
            fullscreen.setAccelerator(fullscreenToggleAction.getShortcut().getKeyStroke());
            fullscreenToggleAction.addButtonModel(fullscreen.getModel());
        }
        viewMenu.addSeparator();
        add(viewMenu, info);
        add(viewMenu, infoweb);
        add(viewMenu, historyinfo);
		add(viewMenu, historyinfoweb);

        add(presetsMenu, presetSearchAction);
        presetsMenu.addSeparator();

        add(toolsMenu, splitWay);
        add(toolsMenu, combineWay);
        toolsMenu.addSeparator();
        add(toolsMenu, reverseWay);
        add(toolsMenu, simplifyWay);
        toolsMenu.addSeparator();
        add(toolsMenu, alignInCircle);
        add(toolsMenu, alignInLine);
        add(toolsMenu, distribute);
        add(toolsMenu, ortho);
        add(toolsMenu, mirror);
        toolsMenu.addSeparator();
        add(toolsMenu, followLine);
        add(toolsMenu, addnode);
        add(toolsMenu, movenode);
        add(toolsMenu, createCircle);
        toolsMenu.addSeparator();
        add(toolsMenu, mergeNodes);
        add(toolsMenu, joinNodeWay);
        add(toolsMenu, unglueNodes);
        toolsMenu.addSeparator();
        add(toolsMenu, joinAreas);
        add(toolsMenu, createMultipolygon);

        // -- changeset manager toggle action
        ChangesetManagerToggleAction changesetManagerToggleAction = new ChangesetManagerToggleAction();
        final JCheckBoxMenuItem mi = MainMenu.addWithCheckbox(windowMenu, changesetManagerToggleAction,
                MainMenu.WINDOW_MENU_GROUP.ALWAYS);
        changesetManagerToggleAction.addButtonModel(mi.getModel());


        if (!Main.pref.getBoolean("audio.menuinvisible", false)) {
            audioMenu = addMenu(marktr("Audio"), KeyEvent.VK_U, defaultMenuPos, ht("/Menu/Audio"));
            add(audioMenu, audioPlayPause);
            add(audioMenu, audioNext);
            add(audioMenu, audioPrev);
            add(audioMenu, audioFwd);
            add(audioMenu, audioBack);
            add(audioMenu, audioSlower);
            add(audioMenu, audioFaster);
        }

        helpMenu.add(statusreport);

        current = helpMenu.add(help); // FIXME why is help not a JosmAction?
        current.setAccelerator(Shortcut.registerShortcut("system:help", tr("Help"), KeyEvent.VK_F1,
                Shortcut.GROUP_DIRECT).getKeyStroke());
        add(helpMenu, about);


        windowMenu.addMenuListener(menuSeparatorHandler);

        new PresetsMenuEnabler(presetsMenu).refreshEnabled();
    }

    static class PresetsMenuEnabler implements MapView.LayerChangeListener {
        private JMenu presetsMenu;
        public PresetsMenuEnabler(JMenu presetsMenu) {
            MapView.addLayerChangeListener(this);
            this.presetsMenu = presetsMenu;
        }
        /**
         * Refreshes the enabled state
         *
         */
        protected void refreshEnabled() {
            presetsMenu.setEnabled(Main.map != null
                    && Main.map.mapView !=null
                    && Main.map.mapView.getEditLayer() != null
            );
        }

        public void activeLayerChange(Layer oldLayer, Layer newLayer) {
            refreshEnabled();
        }

        public void layerAdded(Layer newLayer) {
            refreshEnabled();
        }

        public void layerRemoved(Layer oldLayer) {
            refreshEnabled();
        }
    }
}
