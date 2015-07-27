// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.layer;

import java.awt.image.BufferedImage;

/**
 * Processor that modifies images (sharpen, brightness, etc.).
 * This interface is used by {@link ImageryLayer}s to filter the
 * displayed images (implemented in plugins).
 *
 * @author Nipel-Crumple
 */
public interface ImageProcessor {

    /**
     * This method should process given image according to image processors
     * which is contained in the {@link Layer}
     *
     * @param image that should be processed
     *
     * @return processed image
     */
    public BufferedImage process(BufferedImage image);

}
