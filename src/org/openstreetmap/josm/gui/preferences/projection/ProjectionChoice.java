// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.preferences.projection;

import java.awt.event.ActionListener;
import java.util.Collection;

import javax.swing.JPanel;

import org.openstreetmap.josm.data.projection.Projection;

/**
 * This class offers a choice of projections to the user.
 *
 * It can display a GUI panel, in order to select the parameters.
 */
public interface ProjectionChoice {

    /**
     * Get a unique id for the projection choice.
     */
    String getId();

    /**
     * Set the internal state to match the preferences.
     *
     * Will be called before getPreferencePanel and when the
     * listener from getPreferencePanel is invoked.
     *
     * Argument may be null to reset everything.
     */
    void setPreferences(Collection<String> args);

    /**
     * Get the projection that matches the internal state.
     */
    Projection getProjection();

    /**
     * Generate and provide the GUI.
     *
     * It will be displayed to the user. Call the listener, when the user makes
     * changes in the GUI, so the projection info in the top panel gets updated.
     *
     * @param listener   listener for any change of preferences
     * @return the GUI panel
     */
    JPanel getPreferencePanel(ActionListener listener);

    /**
     * Extract preferences from the GUI.
     *
     * Will be called when the preference dialog is dismissed or
     * when the listener from getPreferencePanel is invoked.
     */
    Collection<String> getPreferences(JPanel panel);

    /**
     * Return all projection codes supported by this projection class.
     */
    String[] allCodes();

    /**
     * Get Preferences from projection code.
     * 
     * @return null when code is not part of this projection choice.
     * An empty Collection as return value indicates, that the code is supported,
     * but no preferences are required to set it up.
     */
    Collection<String> getPreferencesFromCode(String code);

}
