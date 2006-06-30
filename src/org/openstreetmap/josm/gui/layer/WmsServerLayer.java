package org.openstreetmap.josm.gui.layer;

import java.awt.Component;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Point;

import javax.swing.Icon;
import javax.swing.JMenuItem;
import javax.swing.JSeparator;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.visitor.BoundingXYVisitor;
import org.openstreetmap.josm.data.projection.Projection;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.gui.dialogs.LayerList;
import org.openstreetmap.josm.gui.dialogs.LayerListPopup;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.TileCache;

/**
 * This is a layer that grabs the current screen from an WMS server. The data
 * fetched this way is tiled and cached to the disc to reduce server load.
 */
public class WmsServerLayer extends Layer {

	private static Icon icon = ImageProvider.get("layer/wms");

	private final TileCache cache;

	private final String url;


	public WmsServerLayer(String url) {
		super(url.indexOf('/') != -1 ? url.substring(url.indexOf('/')+1) : url);

		// to calculate the world dimension, we assume that the projection does
		// not have problems with translating longitude to a correct scale.
		// Next to that, the projection must be linear dependend on the lat/lon
		// unprojected scale.
		if (Projection.MAX_LON != 180)
			throw new IllegalArgumentException("Wrong longitude transformation for tile cache. Can't operate on "+Main.proj);

		this.url = url;
		cache = new TileCache(url);
	}

	@Override public Icon getIcon() {
		return icon;
	}

	@Override public String getToolTipText() {
		return "WMS layer: "+url;
	}

	@Override public boolean isMergable(Layer other) {
		return false;
	}

	@Override public void mergeFrom(Layer from) {
	}

	@Override public void paint(Graphics g, final MapView mv) {
//		EastNorth max = mv.getEastNorth(mv.getWidth(),0);
//		EastNorth min = mv.getEastNorth(0,mv.getHeight());
//		double width = max.east() - min.east();
//		double height = max.north() - min.north();
//		double tilesX = mv.getWidth() / TileCache.TILESIZE;
//		double tilesY = mv.getHeight() / TileCache.TILESIZE;

		// getting zoom level
		int zoom = 0;
		for (double w = mv.getScale(); w <= TileCache.worldDimension; w *= 2)
			zoom++;
		LatLon oneTile = Main.proj.eastNorth2latlon(new EastNorth(
				TileCache.TILESIZE * mv.getScale(),
				TileCache.TILESIZE * mv.getScale()));
		if (oneTile.lat() > Projection.MAX_LAT || oneTile.lon() > Projection.MAX_LON) {
			// just display the whole world
			Image img = cache.get(0,0);
			Point scr1 = mv.getPoint(Main.proj.latlon2eastNorth(new LatLon(Projection.MAX_LAT, -Projection.MAX_LON)));
			Point scr2 = mv.getPoint(Main.proj.latlon2eastNorth(new LatLon(-Projection.MAX_LAT, Projection.MAX_LON)));
			g.drawImage(img, scr1.x, scr1.y, scr2.x, scr2.y, 0, 0, TileCache.TILESIZE, TileCache.TILESIZE, null);
		}

//		TileCache.TileInformation info = TileCache.pos2tile(min, zoom);
		//System.out.println(url+"bbox="+info.min.lon()+","+info.min.lat()+","+info.max.lon()+","+info.max.lat());
	}

	@Override public void visitBoundingBox(BoundingXYVisitor v) {
		// doesn't have a bounding box
	}

	@Override public Object getInfoComponent() {
		return getToolTipText();
	}

	@Override public Component[] getMenuEntries() {
		return new Component[]{
				new JMenuItem(new LayerList.ShowHideLayerAction(this)),
				new JMenuItem(new LayerList.DeleteLayerAction(this)),
				new JSeparator(),
				new JMenuItem(new LayerListPopup.InfoAction(this))};
    }
}
