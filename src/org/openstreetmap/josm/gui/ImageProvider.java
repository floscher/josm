package org.openstreetmap.josm.gui;

import java.awt.Graphics;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.Transparency;
import java.awt.image.BufferedImage;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javax.swing.Icon;
import javax.swing.ImageIcon;

import org.openstreetmap.josm.Main;

/**
 * Helperclass to support the application with images.
 * @author imi
 */
public class ImageProvider {

	/**
	 * Position of an overlay icon
	 * @author imi
	 */
	public enum OverlayPosition {NORTHWEST, NORTHEAST, SOUTHWEST, SOUTHEAST}
	
	/**
	 * The icon cache
	 */
	private static Map<URL, ImageIcon> cache = new HashMap<URL, ImageIcon>();
	
	/**
	 * Return an image from the specified location.
	 *
	 * @param subdir	The position of the directory, e.g. "layer"
	 * @param name		The icons name (without the ending of ".png")
	 * @return	The requested ImageIcon.
	 */
	public static ImageIcon get(String subdir, String name) {
		if (subdir != "")
			subdir += "/";
		URL path = Main.class.getResource("/images/"+subdir+name+".png");
		if (path == null)
			throw new NullPointerException("/images/"+subdir+name+".png not found");
		ImageIcon icon = cache.get(path);
		if (icon == null) {
			icon = new ImageIcon(path);
			cache.put(path, icon);
		}
		return icon;
	}

	/**
	 * Shortcut for get("", name);
	 */
	public static ImageIcon get(String name) {
		return get("", name);
	}

	/**
	 * Return an icon that represent the overlay of the two given icons. The
	 * second icon is layed on the first relative to the given position.
	 *
	 * @param ground The ground icon (base)
	 * @param overlay The icon to put on top of the ground (overlay)
	 * @return The merged icon.
	 */
	public static Icon overlay(Icon ground, Icon overlay, OverlayPosition pos) {
		GraphicsConfiguration conf = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration();
		int w = ground.getIconWidth();
		int h = ground.getIconHeight();
		int wo = overlay.getIconWidth();
		int ho = overlay.getIconHeight();
		BufferedImage img = conf.createCompatibleImage(w,h, Transparency.TRANSLUCENT);
		Graphics g = img.createGraphics();
		ground.paintIcon(null, g, 0, 0);
		int x = 0, y = 0;
		switch (pos) {
		case NORTHWEST: x = 0;		y = 0;		break;
		case NORTHEAST: x = w-wo;	y = 0;		break;
		case SOUTHWEST: x = 0;		y = h-ho;	break;
		case SOUTHEAST: x = w-wo;	y = h-ho;	break;
		}
		overlay.paintIcon(null, g, x, y);
		return new ImageIcon(img);
	}
}
