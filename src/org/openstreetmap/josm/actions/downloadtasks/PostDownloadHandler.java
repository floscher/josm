// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions.downloadtasks;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.GraphicsEnvironment;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.Future;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.gui.ExceptionDialogUtil;
import org.openstreetmap.josm.gui.Notification;
import org.openstreetmap.josm.tools.ExceptionUtil;
import org.openstreetmap.josm.tools.Utils;

public class PostDownloadHandler implements Runnable {
    private final DownloadTask task;
    private final Future<?> future;

    /**
     * constructor
     * @param task the asynchronous download task
     * @param future the future on which the completion of the download task can be synchronized
     */
    public PostDownloadHandler(DownloadTask task, Future<?> future) {
        this.task = task;
        this.future = future;
    }

    @Override
    public void run() {
        // wait for downloads task to finish (by waiting for the future to return a value)
        //
        try {
            future.get();
        } catch (Exception e) {
            Main.error(e);
            return;
        }

        // make sure errors are reported only once
        //
        Set<Object> errors = new LinkedHashSet<>(task.getErrorObjects());
        if (errors.isEmpty())
            return;

        // just one error object?
        //
        if (errors.size() == 1) {
            final Object error = errors.iterator().next();
            if (!GraphicsEnvironment.isHeadless()) {
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        if (error instanceof Exception) {
                            ExceptionDialogUtil.explainException((Exception) error);
                        } else if (tr("No data found in this area.").equals(error)) {
                            new Notification(error.toString()).setIcon(JOptionPane.WARNING_MESSAGE).show();
                        } else {
                            JOptionPane.showMessageDialog(
                                    Main.parent,
                                    error.toString(),
                                    tr("Error during download"),
                                    JOptionPane.ERROR_MESSAGE);
                        }
                    }
                });
            }
            return;
        }

        // multiple error object? prepare a HTML list
        //
        if (!errors.isEmpty()) {
            final Collection<String> items = new ArrayList<>();
            for (Object error : errors) {
                if (error instanceof String) {
                    items.add((String) error);
                } else if (error instanceof Exception) {
                    items.add(ExceptionUtil.explainException((Exception) error));
                }
            }

            if (!GraphicsEnvironment.isHeadless()) {
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        JOptionPane.showMessageDialog(
                                Main.parent,
                                "<html>"+Utils.joinAsHtmlUnorderedList(items)+"</html>",
                                tr("Errors during download"),
                                JOptionPane.ERROR_MESSAGE);
                    }
                });
            }
            return;
        }
    }
}
