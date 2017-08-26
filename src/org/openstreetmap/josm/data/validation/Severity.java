// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.validation;

import static org.openstreetmap.josm.tools.I18n.marktr;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Color;

import org.openstreetmap.josm.data.preferences.ColorProperty;
import org.openstreetmap.josm.tools.Logging;

/** The error severity */
public enum Severity {
    // CHECKSTYLE.OFF: SingleSpaceSeparator
    /** Error messages */
    ERROR(1, tr("Errors"), /* ICON(data/) */"error",       new ColorProperty(marktr("validation error"), Color.RED).get()),
    /** Warning messages */
    WARNING(2, tr("Warnings"), /* ICON(data/) */"warning", new ColorProperty(marktr("validation warning"), Color.YELLOW).get()),
    /** Other messages */
    OTHER(3, tr("Other"), /* ICON(data/) */"other",        new ColorProperty(marktr("validation other"), Color.CYAN).get());
    // CHECKSTYLE.ON: SingleSpaceSeparator

    /** Numeric ordering of the severities, 1 being major, 3 being minor */
    private final int level;

    /** Description of the severity code */
    private final String message;

    /** Associated icon */
    private final String icon;

    /** Associated color */
    private final Color color;

    /**
     * Constructor
     *
     * @param level Numeric ordering, 1 being major, 3 being minor
     * @param message Description
     * @param icon Associated icon
     * @param color The color of this severity
     */
    Severity(int level, String message, String icon, Color color) {
        this.level = level;
        this.message = message;
        this.icon = icon;
        this.color = color;
    }

    /**
     * Retrieves all colors once from the preferences to register them
     */
    public static void getColors() {
        for (Severity c : values()) {
            Logging.debug("{0}", c);
        }
    }

    @Override
    public String toString() {
        return message;
    }

    /**
     * Gets the associated icon
     * @return the associated icon
     */
    public String getIcon() {
        return icon;
    }

    /**
     * Gets the associated color
     * @return The associated color
     */
    public Color getColor() {
        return color;
    }

    /**
     * Gets the severity level, 1 being major, 3 being minor
     * @return The severity level
     * @since 12667
     */
    public int getLevel() {
        return level;
    }
}
