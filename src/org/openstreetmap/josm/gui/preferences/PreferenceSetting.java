// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.gui.preferences;

public interface PreferenceSetting {
    /**
     * Add the GUI elements to the dialog. The elements should be initialized after
     * the current preferences.
     */
    void addGui(PreferenceTabbedPane gui);

    /**
     * Called when OK is pressed to save the setting in the preferences file.
     * Return true when restart is required.
     */
    boolean ok();
    
    /**
     * Called to know if the preferences tab has only to be displayed in expert mode.
     * @return true if the tab has only to be displayed in expert mode, false otherwise.
     */
    public boolean isExpert();
}
