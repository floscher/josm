// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.cache;

import java.io.Serializable;
import java.util.Arrays;

/**
 * @author Wiktor Niesiobędzki
 *
 * Class that will hold JCS cache entries
 *
 */
public class CacheEntry implements Serializable {
    private static final long serialVersionUID = 1L; //version
    protected byte[] content;

    /**
     * @param content of the cache entry
     */
    public CacheEntry(byte[] content) {
        this.content = Arrays.copyOf(content, content.length);
    }

    /**
     * @return cache entry content
     */
    public byte[] getContent() {
        return Arrays.copyOf(content, content.length);
    }
}
