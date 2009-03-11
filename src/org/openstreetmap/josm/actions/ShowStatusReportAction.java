// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.gui.ExtendedDialog;
import org.openstreetmap.josm.plugins.PluginHandler;
import org.openstreetmap.josm.tools.Shortcut;

/**
 * @author xeen
 *
 * Opens a dialog with useful status information like version numbers for Java, JOSM and plugins
 * Also includes preferences with stripped username and password
 */
public final class ShowStatusReportAction extends JosmAction {
    public ShowStatusReportAction() {
        super(
                tr("Show Status Report"),
                "clock",
                tr("Show status report with useful information that can be attached to bugs"),
                Shortcut.registerShortcut("help:showstatusreport", tr("Help: {0}",
                        tr("Show Status Report")), KeyEvent.VK_R, Shortcut.GROUP_NONE), true);

    }

    public void actionPerformed(ActionEvent e) {
        StringBuilder text = new StringBuilder();
        text.append(AboutAction.getTextBlock());
        text.append("\n");
        text.append("Memory Usage: ");
        text.append(Runtime.getRuntime().totalMemory()/1024/1024);
        text.append(" MB / ");
        text.append(Runtime.getRuntime().maxMemory()/1024/1024);
        text.append(" MB (");
        text.append(Runtime.getRuntime().freeMemory()/1024/1024);
        text.append(" MB allocated, but free)");
        text.append("\n");
        text.append("Java version: " + System.getProperty("java.version"));
        text.append("\n\n");
        text.append(PluginHandler.getBugReportText());
        text.append("\n\n");
        try {
            BufferedReader input = new BufferedReader(new FileReader(Main.pref
                    .getPreferencesDirFile()
                    + File.separator + "preferences"));
            try {
                String line = null;

                while ((line = input.readLine()) != null) {
                    // Skip potential private information
                    if (line.trim().toLowerCase().startsWith("osm-server.username"))
                        continue;
                    if (line.trim().toLowerCase().startsWith("osm-server.password"))
                        continue;
                    if (line.trim().toLowerCase().startsWith("marker.show"))
                        continue;

                    text.append(line);
                    text.append("\n");
                }
            } finally {
                input.close();
            }
        } catch (Exception x) {
            x.printStackTrace();
        }

        JTextArea ta = new JTextArea(text.toString());
        ta.setWrapStyleWord(true);
        ta.setLineWrap(true);
        ta.setEditable(false);
        JScrollPane sp = new JScrollPane(ta);
        sp.setPreferredSize(new Dimension(600, 500));

        int result = new ExtendedDialog(Main.parent, tr(tr("Status Report")), sp,
                new String[] {tr("Copy to clipboard and close"), tr("Close") },
                new String[] {"copy.png", "cancel.png" }).getValue();

        if(result != 1) return;
        try {
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(
                    new StringSelection(text.toString()), new ClipboardOwner() {
                        public void lostOwnership(Clipboard clipboard, Transferable contents) {}
                    }
             );
        }
        catch (RuntimeException x) {}
    }
}
