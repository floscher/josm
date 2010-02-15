// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.relation;

import static org.openstreetmap.josm.gui.dialogs.relation.WayConnectionType.Direction.NONE;
import static org.openstreetmap.josm.tools.I18n.tr;

public class WayConnectionType {

    /** True, if the corresponding primitive is not a way or the way is incomplete */
    private final boolean invalid;

    /** True, if linked to the previous / next member.  */
    public final boolean linkPrev;
    public final boolean linkNext;

    /**
     * direction is FORWARD if the first node of this way is connected to the previous way
     * and / or the last node of this way is connected to the next way.
     * direction is BACKWARD if it is the other way around.
     * direction has a ROUNDABOUT value, if it is tagged as such and it is somehow
     * connected to the previous / next member.
     * If there is no connection to the previous or next member, then
     * direction has the value NONE.
     */
    public final Direction direction;

    public enum Direction {
        FORWARD, BACKWARD, ROUNDABOUT_LEFT, ROUNDABOUT_RIGHT, NONE;

        public boolean isRoundabout() {
            return this == ROUNDABOUT_RIGHT || this == ROUNDABOUT_LEFT;
        }
    }

    /** True, if the element is part of a closed loop of ways. */
    public boolean isLoop;

    public boolean isRoundabout = false;

    public WayConnectionType(boolean linkPrev, boolean linkNext, Direction direction) {
        this.linkPrev = linkPrev;
        this.linkNext = linkNext;
        this.isLoop = false;
        this.direction = direction;
        invalid = false;
    }

    /** construct invalid instance */
    public WayConnectionType() {
        this.linkPrev = false;
        this.linkNext = false;
        this.isLoop = false;
        this.direction = NONE;
        invalid = true;
    }

    public boolean isValid() {
        return !invalid;
    }

    @Override
    public String toString() {
        return "[P "+linkPrev+" ;N "+linkNext+" ;D "+direction+" ;L "+isLoop+"]";
    }

    public String getToolTip() {
        if (!isValid())
            return "";
        else if (linkPrev && linkNext)
            return tr("way is connected");
        else if (linkPrev)
            return tr("way is connected to previous relation member");
        else if (linkNext)
            return tr("way is connected to next relation member");
        else
            return tr("way is not connected to previous or next relation member");
    }
}
