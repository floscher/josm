// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.gui.download;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.Preferences;
import org.openstreetmap.josm.gui.BookmarkList;
import org.openstreetmap.josm.gui.OptionPaneUtil;
import org.openstreetmap.josm.tools.GBC;

/**
 * Bookmark selector.
 *
 * Provides selection, creation and deletion of bookmarks.
 * Extracted from old DownloadAction.
 *
 * @author Frederik Ramm <frederik@remote.org>
 *
 */
public class BookmarkSelection implements DownloadSelection {

    private Preferences.Bookmark tempBookmark = null;
    private BookmarkList bookmarks;

    public void addGui(final DownloadDialog gui) {

        JPanel dlg = new JPanel(new GridBagLayout());
        gui.tabpane.addTab(tr("Bookmarks"), dlg);

        bookmarks = new BookmarkList();

        /* add a handler for "double click" mouse events */
        MouseListener mouseListener = new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    //int index = bookmarks.locationToIndex(e.getPoint());
                    gui.closeDownloadDialog(true);
                }
            }
        };
        bookmarks.addMouseListener(mouseListener);

        bookmarks.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                Preferences.Bookmark b = (Preferences.Bookmark)bookmarks.getSelectedValue();
                if (b != null) {
                    gui.minlat = b.latlon[0];
                    gui.minlon = b.latlon[1];
                    gui.maxlat = b.latlon[2];
                    gui.maxlon = b.latlon[3];
                    gui.boundingBoxChanged(BookmarkSelection.this);
                }
            }
        });
        //wc.addListMarker(bookmarks);
        dlg.add(new JScrollPane(bookmarks), GBC.eol().fill());

        JPanel buttons = new JPanel(new GridLayout(1,2));
        JButton add = new JButton(tr("Add"));
        add.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e) {

                if (tempBookmark == null) {
                    OptionPaneUtil.showMessageDialog(
                            Main.parent,
                            tr("Please enter the desired coordinates first."),
                            tr("Information"),
                            JOptionPane.INFORMATION_MESSAGE
                    );
                    return;
                }
                tempBookmark.name = OptionPaneUtil.showInputDialog(
                        Main.parent,tr("Please enter a name for the location."),
                        tr("Name of location"),
                        JOptionPane.QUESTION_MESSAGE
                );
                if (tempBookmark.name != null && !tempBookmark.name.equals("")) {
                    ((DefaultListModel)bookmarks.getModel()).addElement(tempBookmark);
                    bookmarks.save();
                }
            }
        });
        buttons.add(add);
        JButton remove = new JButton(tr("Remove"));
        remove.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e) {
                Object sel = bookmarks.getSelectedValue();
                if (sel == null) {
                    OptionPaneUtil.showMessageDialog(
                            Main.parent,
                            tr("Select a bookmark first."),
                            tr("Information"),
                            JOptionPane.INFORMATION_MESSAGE
                    );
                    return;
                }
                ((DefaultListModel)bookmarks.getModel()).removeElement(sel);
                bookmarks.save();
            }
        });
        buttons.add(remove);
        dlg.add(buttons, GBC.eop().fill(GBC.HORIZONTAL));
    }

    public void boundingBoxChanged(DownloadDialog gui) {
        tempBookmark = new Preferences.Bookmark();
        tempBookmark.latlon[0] = gui.minlat;
        tempBookmark.latlon[1] = gui.minlon;
        tempBookmark.latlon[2] = gui.maxlat;
        tempBookmark.latlon[3] = gui.maxlon;
        bookmarks.clearSelection();
    }


}
