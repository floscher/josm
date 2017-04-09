// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm;

import java.util.Date;

import org.openstreetmap.josm.tools.date.DateUtils;

/**
 * A comment in a public changeset discussion.
 * @since 7704
 */
public class ChangesetDiscussionComment {

    /** date this comment was posted at */
    private final Date date;
    /** the user who posted the comment */
    private final User user;
    /** comment text */
    private String text;

    /**
     * Constructs a new {@code ChangesetDiscussionComment}.
     * @param date date this comment was posted at
     * @param user the user who posted the comment
     */
    public ChangesetDiscussionComment(Date date, User user) {
        this.date = DateUtils.cloneDate(date);
        this.user = user;
    }

    /**
     * Replies comment text.
     * @return comment text
     */
    public final String getText() {
        return text;
    }

    /**
     * Sets comment text.
     * @param text comment text
     */
    public final void setText(String text) {
        this.text = text;
    }

    /**
     * Replies date this comment was posted at.
     * @return date this comment was posted at
     */
    public final Date getDate() {
        return DateUtils.cloneDate(date);
    }

    /**
     * Replies the user who posted the comment.
     * @return the user who posted the comment
     */
    public final User getUser() {
        return user;
    }

    @Override
    public String toString() {
        return "ChangesetDiscussionComment [date=" + date + ", user=" + user + ", text='" + text + "']";
    }
}
