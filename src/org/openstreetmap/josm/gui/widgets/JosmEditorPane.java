// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.widgets;

import java.io.IOException;
import java.net.URL;

import javax.swing.JEditorPane;

/**
 * Subclass of {@link JEditorPane} that adds a "native" context menu (cut/copy/paste/select all).
 * @since 5885
 */
public class JosmEditorPane extends JEditorPane {

    /**
     * Creates a new <code>JosmEditorPane</code>.
     * The document model is set to <code>null</code>.
     */
    public JosmEditorPane() {
        TextContextualPopupMenu.enableMenuFor(this);
    }

    /**
     * Creates a <code>JosmEditorPane</code> based on a specified URL for input.
     *
     * @param initialPage the URL
     * @exception IOException if the URL is <code>null</code> or cannot be accessed
     */
    public JosmEditorPane(URL initialPage) throws IOException {
        this();
        setPage(initialPage);
    }

    /**
     * Creates a <code>JosmEditorPane</code> based on a string containing
     * a URL specification.
     *
     * @param url the URL
     * @exception IOException if the URL is <code>null</code> or cannot be accessed
     */
    public JosmEditorPane(String url) throws IOException {
        this();
        setPage(url);
    }

    /**
     * Creates a <code>JosmEditorPane</code> that has been initialized
     * to the given text.  This is a convenience constructor that calls the
     * <code>setContentType</code> and <code>setText</code> methods.
     *
     * @param type mime type of the given text
     * @param text the text to initialize with; may be <code>null</code>
     * @exception NullPointerException if the <code>type</code> parameter
     *      is <code>null</code>
     */
    public JosmEditorPane(String type, String text) {
        this();
        setContentType(type);
        setText(text);
    }
}
