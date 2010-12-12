// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools;

import java.io.Serializable;
import java.util.Comparator;

import javax.swing.JMenuItem;

public class PresetTextComparator implements Comparator<JMenuItem>, Serializable {
    //TODO add error checking and stuff
    public int compare(JMenuItem arg0, JMenuItem arg1) {
        return arg0.getText().compareTo(arg1.getText());
    }

}
