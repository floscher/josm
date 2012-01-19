// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools;

import com.kitfox.svg.SVGDiagram;

import java.awt.Dimension;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import javax.swing.ImageIcon;

import org.openstreetmap.josm.tools.ImageProvider.SanitizeMode;

/**
 * Holds data for one particular image.
 * It can be backed by a svg or raster image.
 * 
 * In the first case, 'svg' is not null and in the latter case, 'imgCache' has 
 * at least one entry for the key DEFAULT_DIMENSION.
 */
class ImageResource {
    
    /**
     * Caches the image data for resized versions of the same image.
     */
    private HashMap<Dimension, ImageWrapper> imgCache = new HashMap<Dimension, ImageWrapper>();
    private SVGDiagram svg;
    public static final Dimension DEFAULT_DIMENSION = new Dimension(-1, -1);
 
    /**
     * remember whether the image has been sanitized
     */
    private static class ImageWrapper {
        Image img;
        boolean sanitized;

        public ImageWrapper(Image img, boolean sanitized) {
            CheckParameterUtil.ensureParameterNotNull(img);
            this.img = img;
            this.sanitized = sanitized;
        }
    }
    
    public ImageResource(Image img, boolean sanitized) {
        CheckParameterUtil.ensureParameterNotNull(img);
        imgCache.put(DEFAULT_DIMENSION, new ImageWrapper(img, sanitized));
    }

    public ImageResource(SVGDiagram svg) {
        CheckParameterUtil.ensureParameterNotNull(svg);
        this.svg = svg;
    }

    public ImageIcon getImageIcon() {
        return getImageIcon(DEFAULT_DIMENSION, SanitizeMode.OFF);
    }

    /**
     * Get an ImageIcon object for the image of this resource
     * @param   dim The requested dimensions. Use (-1,-1) for the original size
     *          and (width, -1) to set the width, but otherwise scale the image
     *          proportionally.
     * @param sanitize Whether the returned image should be copied to a BufferedImage
     *          to avoid certain problem with native image formats.
     */
    public ImageIcon getImageIcon(Dimension dim, SanitizeMode sanitize) {
        if (dim.width < -1 || dim.width == 0 || dim.height < -1 || dim.height == 0)
            throw new IllegalArgumentException();
        ImageWrapper iw = imgCache.get(dim);
        if (iw != null) {
            if (!iw.sanitized) {
                if (sanitize == SanitizeMode.ALWAYS || (sanitize == SanitizeMode.MAKE_BUFFEREDIMAGE && !(iw.img instanceof BufferedImage))) {
                    iw.img = ImageProvider.sanitize(iw.img);
                    iw.sanitized = true;
                }
            }
            return new ImageIcon(iw.img);
        }
        if (svg != null) {
            Image img = ImageProvider.createImageFromSvg(svg, dim);
            imgCache.put(dim, new ImageWrapper(img, true));
            return new ImageIcon(img);
        } else {
            ImageWrapper base = imgCache.get(DEFAULT_DIMENSION);
            if (base == null) throw new AssertionError();
            
            int width = dim.width;
            int height = dim.height;
            ImageIcon icon = new ImageIcon(base.img);
            if (width == -1) {
                width = icon.getIconWidth() * height / icon.getIconHeight();
            } else if (height == -1) {
                height = icon.getIconHeight() * width / icon.getIconWidth();
            }
            Image img;
            if (width == dim.width && height == dim.height) {
                img = icon.getImage();
            } else {
                img = icon.getImage().getScaledInstance(width, height, Image.SCALE_SMOOTH);
            }
            boolean sanitized = false;
            if (sanitize == SanitizeMode.ALWAYS || (sanitize == SanitizeMode.MAKE_BUFFEREDIMAGE && !(img instanceof BufferedImage))) {
                img = ImageProvider.sanitize(img);
                sanitized = true;
            }
            imgCache.put(dim, new ImageWrapper(img, sanitized));
            return new ImageIcon(img);
        }
    }

    /**
     * Get image icon with a certain maximum size. The image is scaled down
     * to fit maximum dimensions. (Keeps aspect ratio)
     *
     * @param maxSize The maximum size. One of the dimensions (widht or height) can be -1,
     * which means it is not bounded.
     */
    public ImageIcon getImageIconBounded(Dimension maxSize, SanitizeMode sanitize) {
        if (maxSize.width < -1 || maxSize.width == 0 || maxSize.height < -1 || maxSize.height == 0)
            throw new IllegalArgumentException();
        float realWidth;
        float realHeight;
        if (svg != null) {
            realWidth = svg.getWidth();
            realHeight = svg.getHeight();
        } else {
            ImageWrapper base = imgCache.get(DEFAULT_DIMENSION);
            if (base == null) throw new AssertionError();
            ImageIcon icon = new ImageIcon(base.img);
            realWidth = icon.getIconWidth();
            realHeight = icon.getIconHeight();
        }
        int maxWidth = maxSize.width;
        int maxHeight = maxSize.height;

        if (realWidth <= maxWidth) {
            maxWidth = -1;
        }
        if (realHeight <= maxHeight) {
            maxHeight = -1;
        }

        if (maxWidth == -1 && maxHeight == -1)
            return getImageIcon(DEFAULT_DIMENSION, sanitize);
        else if (maxWidth == -1)
            return getImageIcon(new Dimension(-1, maxHeight), sanitize);
        else if (maxHeight == -1)
            return getImageIcon(new Dimension(maxWidth, -1), sanitize);
        else
            if (realWidth / maxWidth > realHeight / maxHeight)
                return getImageIcon(new Dimension(maxWidth, -1), sanitize);
            else
                return getImageIcon(new Dimension(-1, maxHeight), sanitize);
   }
}
