// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.gui.preferences;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.List;

import javax.swing.JPanel;
import javax.swing.table.AbstractTableModel;

import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.Shortcut;

public class ShortcutPreference implements PreferenceSetting {

    public static class Factory implements PreferenceSettingFactory {
        public PreferenceSetting createPreferenceSetting() {
            return new ShortcutPreference();
        }
    }

    public void addGui(PreferenceTabbedPane gui) {
        // icon source: http://www.iconfinder.net/index.php?q=key&page=icondetails&iconid=8553&size=128&q=key&s12=on&s16=on&s22=on&s32=on&s48=on&s64=on&s128=on
        // icon licence: GPL
        // icon designer: Paolino, http://www.paolinoland.it/
        // icon original filename: keyboard.png
        // icon original size: 128x128
        // modifications: icon was cropped, then resized
        JPanel p = gui.createPreferenceTab("shortcuts", tr("Shortcut Preferences"),
                tr("Changing keyboard shortcuts manually."), false);

        PrefJPanel prefpanel = new PrefJPanel(new scListModel());
        p.add(prefpanel, GBC.eol().fill(GBC.BOTH));

    }

    public boolean ok() {
        return Shortcut.savePrefs();
    }

    // Maybe move this to prefPanel? There's no need for it to be here.
    private static class scListModel extends AbstractTableModel {
        private String[] columnNames = new String[]{tr("Action"), tr("Shortcut")};
        private List<Shortcut> data;

        public scListModel() {
            data = Shortcut.listAll();
        }
        public int getColumnCount() {
            return columnNames.length;
        }
        public int getRowCount() {
            return data.size();
        }
        @Override
        public String getColumnName(int col) {
            return columnNames[col];
        }
        public Object getValueAt(int row, int col) {
            Shortcut sc = data.get(row);
            if (col == 0)
                return sc.getLongText();
            else if (col == 1)
                return sc.getKeyText();
            else
                // This is a kind of hack that allows the actions on the editing controls
                // to access the underlying shortcut object without introducing another
                // method. I opted to stay within the interface.
                return sc;
        }
        @Override
        public boolean isCellEditable(int row, int col) {
            return false;
        }
    }
}
