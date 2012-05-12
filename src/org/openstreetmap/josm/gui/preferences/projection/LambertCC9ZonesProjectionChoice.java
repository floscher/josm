// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.preferences.projection;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionListener;
import java.util.Collection;
import java.util.Collections;

import javax.swing.JLabel;
import javax.swing.JPanel;

import org.openstreetmap.josm.data.projection.LambertCC9Zones;
import org.openstreetmap.josm.data.projection.Projection;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.ImageProvider;

public class LambertCC9ZonesProjectionChoice extends ListProjectionChoice implements Alias {

    private static String[] lambert9zones = {
        tr("{0} ({1} to {2} degrees)", 1,41,43),
        tr("{0} ({1} to {2} degrees)", 2,42,44),
        tr("{0} ({1} to {2} degrees)", 3,43,45),
        tr("{0} ({1} to {2} degrees)", 4,44,46),
        tr("{0} ({1} to {2} degrees)", 5,45,47),
        tr("{0} ({1} to {2} degrees)", 6,46,48),
        tr("{0} ({1} to {2} degrees)", 7,47,49),
        tr("{0} ({1} to {2} degrees)", 8,48,50),
        tr("{0} ({1} to {2} degrees)", 9,49,51)
    };

    public LambertCC9ZonesProjectionChoice() {
        super("core:lambertcc9", tr("Lambert CC9 Zone (France)"), lambert9zones, tr("Lambert CC Zone"));
    }

    private class LambertCC9CBPanel extends CBPanel {
        public LambertCC9CBPanel(Object[] entries, int initialIndex, String label, ActionListener listener) {
            super(entries, initialIndex, label, listener);
            this.add(new JLabel(ImageProvider.get("data/projection", "LambertCC9Zones.png")), GBC.eol().fill(GBC.HORIZONTAL));
            this.add(GBC.glue(1, 1), GBC.eol().fill(GBC.BOTH));
        }
    }

    @Override
    public JPanel getPreferencePanel(ActionListener listener) {
        return new LambertCC9CBPanel(entries, index, label, listener);
    }

    @Override
    public Projection getProjection() {
        return new LambertCC9Zones(index);
    }

    @Override
    public String[] allCodes() {
        String[] codes = new String[9];
        for (int zone = 0; zone < 9; zone++) {
            codes[zone] = "EPSG:" + (3942 + zone);
        }
        return codes;
    }

    @Override
    public Collection<String> getPreferencesFromCode(String code) {
        //zone 1=CC42=EPSG:3942 up to zone 9=CC50=EPSG:3950
        if (code.startsWith("EPSG:39") && code.length() == 9) {
            try {
                String zonestring = code.substring(5,9);
                int zoneval = Integer.parseInt(zonestring)-3942;
                if(zoneval >= 0 && zoneval <= 8)
                    return Collections.singleton(String.valueOf(zoneval+1));
            } catch(NumberFormatException ex) {}
        }
        return null;
    }
    
    @Override
    protected int indexToZone(int index) {
        return index + 1;
    }

    @Override
    protected int zoneToIndex(int zone) {
        return zone - 1;
    }

    @Override
    public String getAlias() {
        return LambertCC9Zones.class.getName();
    }

}
