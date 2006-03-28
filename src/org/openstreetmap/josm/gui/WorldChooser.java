package org.openstreetmap.josm.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.beans.PropertyChangeListener;
import java.net.URL;

import javax.swing.ImageIcon;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.projection.Projection;
import org.openstreetmap.josm.gui.BookmarkList.Bookmark;
import org.openstreetmap.josm.gui.SelectionManager.SelectionEnded;

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
	 * Maximum scale level
	 */
	private double scaleMax;
	
	/**
	 * Mark this rectangle (lat/lon values) when painting.
	 */
	private EastNorth markerMin, markerMax;

	private Projection projection;
	
	/**
	 * Create the chooser component.
	 */
	public WorldChooser() {
		URL path = Main.class.getResource("/images/world.jpg");
		world = new ImageIcon(path);
		center = new EastNorth(world.getIconWidth()/2, world.getIconHeight()/2);
		setPreferredSize(new Dimension(200, 100));
		new MapMover(this);
		projection = new Projection() {
			public EastNorth latlon2eastNorth(LatLon p) {
				return new EastNorth(
						(p.lon()+180) / 360 * world.getIconWidth(),
						(p.lat()+90) / 180 * world.getIconHeight());
			}
			public LatLon eastNorth2latlon(EastNorth p) {
				return new LatLon(
						p.north()*180/world.getIconHeight() - 90,
						p.east()*360/world.getIconWidth() - 180);
			}
			@Override
			public String toString() {
				return "WorldChooser";
			}
		};
		setMinimumSize(new Dimension(350, 350/2));
	}


	/**
	 * Set the scale as well as the preferred size.
	 */
	@Override
	public void setPreferredSize(Dimension preferredSize) {
		super.setPreferredSize(preferredSize);
		scale = world.getIconWidth()/preferredSize.getWidth();
		scaleMax = scale;
	}


	/**
	 * Draw the current selected region.
	 */
	@Override
	public void paint(Graphics g) {
		EastNorth tl = getEastNorth(0,0);
		EastNorth br = getEastNorth(getWidth(),getHeight());
		g.drawImage(world.getImage(),0,0,getWidth(),getHeight(),(int)tl.east(),(int)tl.north(),(int)br.east(),(int)br.north(), null);

		// draw marker rect
		if (markerMin != null && markerMax != null) {
			Point p1 = getPoint(markerMin);
			Point p2 = getPoint(markerMax);
			double x = Math.min(p1.x, p2.x);
			double y = Math.min(p1.y, p2.y);
			double w = Math.max(p1.x, p2.x) - x;
			double h = Math.max(p1.y, p2.y) - y;
			if (w < 1)
				w = 1;
			if (h < 1)
				h = 1;
			g.setColor(Color.YELLOW);
			g.drawRect((int)x, (int)y, (int)w, (int)h);
		}
	}


	@Override
	public void zoomTo(EastNorth newCenter, double scale) {
		if (getWidth() != 0 && scale > scaleMax) {
			scale = scaleMax;
			newCenter = center;
		}
		super.zoomTo(newCenter, scale);
	}

	/**
	 * Show the selection bookmark in the world.
	 */
	public void addListMarker(final BookmarkList list) {
		list.addListSelectionListener(new ListSelectionListener(){
			public void valueChanged(ListSelectionEvent e) {
				Bookmark b = (Bookmark)list.getSelectedValue();
				if (b != null) {
					markerMin = getProjection().latlon2eastNorth(new LatLon(b.latlon[0],b.latlon[1]));
					markerMax = getProjection().latlon2eastNorth(new LatLon(b.latlon[2],b.latlon[3]));
				} else {
					markerMin = null;
					markerMax = null;
				}
				repaint();
			}
		});
	}

	/**
	 * Update edit fields and react upon changes.
	 * @param field Must have exactly 4 entries (min lat to max lon)
	 */
	public void addInputFields(final JTextField[] field, JTextField osmUrl, final KeyListener osmUrlRefresher) {
		// listener that invokes updateMarkerFromTextField after all
		// messages are dispatched and so text fields are updated.
		KeyListener listener = new KeyAdapter(){
			@Override
			public void keyTyped(KeyEvent e) {
				SwingUtilities.invokeLater(new Runnable(){
					public void run() {
						updateMarkerFromTextFields(field);
					}
				});
			}
		};

		for (JTextField f : field)
			f.addKeyListener(listener);
		osmUrl.addKeyListener(listener);

		SelectionEnded selListener = new SelectionEnded(){
			public void selectionEnded(Rectangle r, boolean alt, boolean shift, boolean ctrl) {
				markerMin = getEastNorth(r.x, r.y+r.height);
				markerMax = getEastNorth(r.x+r.width, r.y);
				LatLon min = getProjection().eastNorth2latlon(markerMin);
				LatLon max = getProjection().eastNorth2latlon(markerMax);
				field[0].setText(""+min.lat());
				field[1].setText(""+min.lon());
				field[2].setText(""+max.lat());
				field[3].setText(""+max.lon());
				for (JTextField f : field)
					f.setCaretPosition(0);
				osmUrlRefresher.keyTyped(null);
				repaint();
			}
			public void addPropertyChangeListener(PropertyChangeListener listener) {}
			public void removePropertyChangeListener(PropertyChangeListener listener) {}
		};
		SelectionManager sm = new SelectionManager(selListener, false, this);
		sm.register(this);
		updateMarkerFromTextFields(field);
	}

	/**
	 * Update the marker field from the values of the given textfields
	 */
	private void updateMarkerFromTextFields(JTextField[] field) {
		// try to read all values
		double v[] = new double[field.length];
		for (int i = 0; i < field.length; ++i) {
			try {
				v[i] = Double.parseDouble(field[i].getText());
			} catch (NumberFormatException nfe) {
				return;
			}
		}
		markerMin = getProjection().latlon2eastNorth(new LatLon(v[0], v[1]));
		markerMax = getProjection().latlon2eastNorth(new LatLon(v[2], v[3]));
		repaint();
	}

	/**
	 * Always use our image projection mode.
	 */
	@Override
	protected Projection getProjection() {
		return projection;
	}
}
