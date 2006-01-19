package org.openstreetmap.josm.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.net.URL;

import javax.swing.ImageIcon;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.GeoPoint;
import org.openstreetmap.josm.gui.BookmarkList.Bookmark;

/**
 * A component that let the user select a lat/lon bounding box from zooming
 * into the world as a picture and selecting a region.
 *
 * The component has to be of the aspect ration 2:1 to look good.
 *
 * @author imi
 */
public class WorldChooser extends NavigatableComponent {

	/**
	 * The world as picture.
	 */
	private ImageIcon world;

	/**
	 * Mark this rectangle (lat/lon values) when painting.
	 */
	protected double[] markerRect;

	/**
	 * Create the chooser component.
	 */
	public WorldChooser() {
		URL path = Main.class.getResource("/images/world.jpg");
		world = new ImageIcon(path);
		center = new GeoPoint(0,0,30,165);
		setPreferredSize(new Dimension(200, 100));
		//new MapMover(this);
	}


	/**
	 * Set the scale as well as the preferred size.
	 */
	@Override
	public void setPreferredSize(Dimension preferredSize) {
		super.setPreferredSize(preferredSize);
		scale = 60/preferredSize.getWidth();
	}


	/**
	 * Draw the current selected region.
	 */
	@Override
	public void paint(Graphics g) {
		int x1 = getScreenPoint(new GeoPoint(0,0,0,180)).x;
		int y1 = getScreenPoint(new GeoPoint(0,0,0,180)).y;
		int x2 = getScreenPoint(new GeoPoint(0,0,360,0)).x;
		int y2 = getScreenPoint(new GeoPoint(0,0,360,0)).y;
		System.out.println(x1+" "+y1+" "+x2+" "+y2);
		System.out.println(center.x+" "+center.y+" "+(scale*getWidth()));
		g.drawImage(world.getImage(),0,0,getWidth(),getHeight(),x1,y1,x2,y2, null);
		g.setColor(Color.WHITE);
		
		// draw marker rect
		//TODO
	}


	@Override
	public void zoomTo(GeoPoint newCenter, double scale) {
		if (getWidth() != 0 && scale < 60.0/getWidth())
			scale = 60.0/getWidth();
		super.zoomTo(newCenter, scale);
	}
	
	/**
	 * Show the selection bookmark in the world.
	 */
	public void addListMarker(final BookmarkList list) {
		list.addListSelectionListener(new ListSelectionListener(){
			public void valueChanged(ListSelectionEvent e) {
				Bookmark b = (Bookmark)list.getSelectedValue();
				if (b != null)
					markerRect = b.latlon;
				else
					markerRect = null;
				repaint();
			}
		});
	}
}
