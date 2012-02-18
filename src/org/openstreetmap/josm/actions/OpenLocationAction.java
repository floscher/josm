// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.gui.help.HelpUtil.ht;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Future;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.downloadtasks.DownloadGpsTask;
import org.openstreetmap.josm.actions.downloadtasks.DownloadOsmChangeTask;
import org.openstreetmap.josm.actions.downloadtasks.DownloadOsmTask;
import org.openstreetmap.josm.actions.downloadtasks.DownloadTask;
import org.openstreetmap.josm.actions.downloadtasks.PostDownloadHandler;
import org.openstreetmap.josm.gui.ExtendedDialog;
import org.openstreetmap.josm.gui.progress.PleaseWaitProgressMonitor;
import org.openstreetmap.josm.gui.widgets.HistoryComboBox;
import org.openstreetmap.josm.tools.Shortcut;

/**
 * Open an URL input dialog and load data from the given URL.
 *
 * @author imi
 */
public class OpenLocationAction extends JosmAction {

    protected final List<Class<? extends DownloadTask>> downloadTasks;
    
    /**
     * Create an open action. The name is "Open a file".
     */
    public OpenLocationAction() {
        /* I18N: Command to download a specific location/URL */
        super(tr("Open Location..."), "openlocation", tr("Open an URL."),
                Shortcut.registerShortcut("system:open_location", tr("File: {0}", tr("Open Location...")), KeyEvent.VK_L, Shortcut.CTRL), true);
        putValue("help", ht("/Action/OpenLocation"));
        this.downloadTasks = new ArrayList<Class<? extends DownloadTask>>();
        addDownloadTaskClass(DownloadOsmTask.class);
        addDownloadTaskClass(DownloadGpsTask.class);
        addDownloadTaskClass(DownloadOsmChangeTask.class);
    }

    /**
     * Restore the current history from the preferences
     *
     * @param cbHistory
     */
    protected void restoreUploadAddressHistory(HistoryComboBox cbHistory) {
        List<String> cmtHistory = new LinkedList<String>(Main.pref.getCollection(getClass().getName() + ".uploadAddressHistory", new LinkedList<String>()));
        // we have to reverse the history, because ComboBoxHistory will reverse it again
        // in addElement()
        //
        Collections.reverse(cmtHistory);
        cbHistory.setPossibleItems(cmtHistory);
    }

    /**
     * Remind the current history in the preferences
     * @param cbHistory
     */
    protected void remindUploadAddressHistory(HistoryComboBox cbHistory) {
        cbHistory.addCurrentItemToHistory();
        Main.pref.putCollection(getClass().getName() + ".uploadAddressHistory", cbHistory.getHistory());
    }

    public void actionPerformed(ActionEvent e) {

        JCheckBox layer = new JCheckBox(tr("Separate Layer"));
        layer.setToolTipText(tr("Select if the data should be downloaded into a new layer"));
        layer.setSelected(Main.pref.getBoolean("download.newlayer"));
        JPanel all = new JPanel(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.weightx = 1.0;
        gc.anchor = GridBagConstraints.FIRST_LINE_START;
        all.add(new JLabel(tr("Enter URL to download:")), gc);
        HistoryComboBox uploadAddresses = new HistoryComboBox();
        uploadAddresses.setToolTipText(tr("Enter an URL from where data should be downloaded"));
        restoreUploadAddressHistory(uploadAddresses);
        gc.gridy = 1;
        all.add(uploadAddresses, gc);
        gc.gridy = 2;
        gc.fill = GridBagConstraints.BOTH;
        gc.weighty = 1.0;
        all.add(layer, gc);
        ExtendedDialog dialog = new ExtendedDialog(Main.parent,
                tr("Download Location"),
                new String[] {tr("Download URL"), tr("Cancel")}
        );
        dialog.setContent(all, false /* don't embedded content in JScrollpane  */);
        dialog.setButtonIcons(new String[] {"download.png", "cancel.png"});
        dialog.setToolTipTexts(new String[] {
                tr("Start downloading data"),
                tr("Close dialog and cancel downloading")
        });
        dialog.configureContextsensitiveHelp("/Action/OpenLocation", true /* show help button */);
        dialog.showDialog();
        if (dialog.getValue() != 1) return;
        remindUploadAddressHistory(uploadAddresses);
        openUrl(layer.isSelected(), uploadAddresses.getText());
    }

    /**
     * Open the given URL.
     */
    public void openUrl(boolean new_layer, final String url) {
        PleaseWaitProgressMonitor monitor = new PleaseWaitProgressMonitor(tr("Download Data"));
        DownloadTask task = null;
        Future<?> future = null;
        for (int i = 0; future == null && i < downloadTasks.size(); i++) {
            Class<? extends DownloadTask> taskClass = downloadTasks.get(i);
            if (taskClass != null) {
                try {
                    task = taskClass.getConstructor().newInstance();
                    if (task.acceptsUrl(url)) {
                        future = task.loadUrl(new_layer, url, monitor);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        if (future != null) {
            Main.worker.submit(new PostDownloadHandler(task, future));
        } else {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    JOptionPane.showMessageDialog(Main.parent, tr(
                            "<html>Cannot open URL ''{0}'' because no suitable download task is available.</html>",
                            url), tr("Download Location"), JOptionPane.ERROR_MESSAGE);
                }
            });
        }
    }
    
    public boolean addDownloadTaskClass(Class<? extends DownloadTask> taskClass) {
        return this.downloadTasks.add(taskClass);
    }
}
