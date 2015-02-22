// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.image.BufferedImage;

import javax.swing.ImageIcon;

/** class to describe how image overlay
 * @since 8095
 */
public class ImageOverlay {
    /** the image ressource to use as overlay */
    public ImageProvider image;
    /** offset of the image from left border, values between 0 and 1 */
    private double offsetLeft;
    /** offset of the image from top border, values between 0 and 1 */
    private double offsetRight;
    /** offset of the image from right border, values between 0 and 1*/
    private double offsetTop;
    /** offset of the image from bottom border, values between 0 and 1 */
    private double offsetBottom;
    
    /**
     * Create an overlay info. All values are relative sizes between 0 and 1. Size of the image
     * is the result of the difference between left/right and top/bottom.
     *
     * @param image imager provider for the overlay icon
     * @param offsetLeft offset of the image from left border, values between 0 and 1, -1 for auto-calculation
     * @param offsetTop offset of the image from top border, values between 0 and 1, -1 for auto-calculation
     * @param offsetRight offset of the image from right border, values between 0 and 1, -1 for auto-calculation
     * @param offsetBottom offset of the image from bottom border, values between 0 and 1, -1 for auto-calculation
     * @since 8095
     */
    public ImageOverlay(ImageProvider image, double offsetLeft, double offsetTop, double offsetRight, double offsetBottom) {
        this.image = image;
        this.offsetLeft = offsetLeft;
        this.offsetTop = offsetTop;
        this.offsetRight = offsetRight;
        this.offsetBottom = offsetBottom;
    }
    
    /**
     * Create an overlay in southeast corner. All values are relative sizes between 0 and 1.
     * Size of the image is the result of the difference between left/right and top/bottom.
     * Right and bottom values are set to 1.
     *
     * @param image imager provider for the overlay icon
     * @see #ImageOverlay(ImageProvider, double, double, double, double)
     * @since 8095
     */
    public ImageOverlay(ImageProvider image) {
        this.image = image;
        this.offsetLeft = -1.0;
        this.offsetTop = -1.0;
        this.offsetRight = 1.0;
        this.offsetBottom = 1.0;
    }

    /**
     * Handle overlay. The image passed as argument is modified!
     *
     * @param ground the base image for the overlay (gets modified!)
     * @return the modified image (same as argument)
     * @since 8095
     */
    public BufferedImage apply(BufferedImage ground) {
        /* get base dimensions for calculation */
        int w = ground.getWidth();
        int h = ground.getHeight();
        int width = -1;
        int height = -1;
        if (offsetRight > 0 && offsetLeft > 0) {
            width = new Double(w*(offsetRight-offsetLeft)).intValue();
        }
        if (offsetTop > 0 && offsetBottom > 0) {
            width = new Double(h*(offsetBottom-offsetTop)).intValue();
        }
        ImageIcon overlay;
        if(width != -1 || height != -1) {
            /* Don't modify ImageProvider, but apply maximum size, probably cloning the ImageProvider
               would be a better approach. */
            overlay = image.getResource().getImageIconBounded(new Dimension(width, height));
        } else {
            overlay = image.get();
        }
        int x, y;
        if (width == -1 && offsetLeft < 0) {
            x = new Double(w*offsetRight).intValue() - overlay.getIconWidth();
        } else {
            x = new Double(w*offsetLeft).intValue();
        }
        if (height == -1 && offsetTop < 0) {
            y = new Double(h*offsetBottom).intValue() - overlay.getIconHeight();
        } else {
            y = new Double(h*offsetTop).intValue();
        }
        overlay.paintIcon(null, ground.getGraphics(), x, y);
        return ground;
    }
}
