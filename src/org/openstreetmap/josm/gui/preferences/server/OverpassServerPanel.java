// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.preferences.server;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.GridBagLayout;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.openstreetmap.josm.actions.ExpertToggleAction;
import org.openstreetmap.josm.gui.widgets.HistoryComboBox;
import org.openstreetmap.josm.io.OverpassDownloadReader;
import org.openstreetmap.josm.tools.GBC;

/**
 * Preferences related to Overpass API servers.
 *
 * @since 17162
 */
public class OverpassServerPanel extends JPanel {

    private final HistoryComboBox overpassServer = new HistoryComboBox();
    private final JCheckBox forMultiFetch = new JCheckBox(tr("Use Overpass server for object downloads"));

    OverpassServerPanel() {
        super(new GridBagLayout());
        ExpertToggleAction.addVisibilitySwitcher(this);
        final JPanel panel = this;
        panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        panel.add(new JLabel(tr("Overpass server: ")), GBC.std().insets(5, 5, 5, 5));
        panel.add(overpassServer, GBC.eop().fill(GBC.HORIZONTAL));
        panel.add(forMultiFetch, GBC.eop());
        panel.add(Box.createVerticalGlue(), GBC.eol().fill());
    }

    /**
     * Initializes the panel from preferences
     */
    public final void initFromPreferences() {
        overpassServer.setPossibleItems(OverpassDownloadReader.OVERPASS_SERVER_HISTORY.get());
        overpassServer.setText(OverpassDownloadReader.OVERPASS_SERVER.get());
        forMultiFetch.setSelected(OverpassDownloadReader.FOR_MULTI_FETCH.get());
    }

    /**
     * Saves the current values to the preferences
     */
    public final void saveToPreferences() {
        OverpassDownloadReader.OVERPASS_SERVER.put(overpassServer.getText());
        OverpassDownloadReader.OVERPASS_SERVER_HISTORY.put(overpassServer.getHistory());
        OverpassDownloadReader.FOR_MULTI_FETCH.put(forMultiFetch.isSelected());
    }
}
