package org.openstreetmap.josm.tools;

import java.awt.Image;
import java.awt.Toolkit;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.Vector;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.Preferences;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.coor.LatLon;

/**
 * A tile cache utility class to calculate tiles from lat/lon values.
 * 
 * One tile is a prerendered image of 512x512 pixels in size. The Zoom level of
 * a tile means, how big is the partion of the world, this tile occupy. On Mercator,
 * zoom=0 is exact the whole world, while zoom=1 is one quarter, zoom=2 is one of 
 * sixteen and so on.
 * 
 * The tile identifier is an 32-bit integer whose bits are ordered in pairs of two.
 * If bit0 (the LSB) is 0, then the tile is right of the equator (0 grad). If bit1 
 * is 0, it is in the northern hemisphere. The next two bits representing the 
 * quarter within the first quarter, so bit2 equals to 0 means, it is in the right 
 * half of the quarter, represented by bit0 and bit1.
 */
public class TileCache {

    public static final int TILESIZE = 512;
    
    /**
     * This is the dimension of the world on x or y axis (both are the same).  Calculated in the constructor from the current projection, so that the  tiles are square.
     */
    public static final double worldDimension;
    
    static {
        EastNorth en = Main.proj.latlon2eastNorth(new LatLon(0,180));
        worldDimension = en.east();
    }
    
    
	public static class TileInformation {
    	/**
    	 * The encoded tile id for that point
    	 */
    	public final int tileId;
    	/**
    	 * Offset of the point within the tile (after projection)
    	 */
    	public final double offsetX, offsetY;
    	/**
    	 * OSM zoom factor (0 = earth, 1=quarter of earth, 2=sixteenth of earth...)
    	 */
    	public final int zoom;
    	/**
    	 * Projected point 
    	 */
    	public final EastNorth pos;
    	/**
    	 * Unprojected lower left corner of the tile
    	 */
    	public final LatLon min;
    	/**
    	 * Unprojected upper right corner of the tile
    	 */
    	public final LatLon max;
    	
    	public TileInformation(int id, double x, double y, int z, EastNorth p, LatLon mi, LatLon ma) {
    		tileId = id; offsetX = x; offsetY = y; zoom = z; pos = p; min = mi; max = ma;
    	}
    }

	private final static Image loading = ImageProvider.get("loading.jpg").getImage();
    
    private Map<Integer, Map<Integer, Image>> cache = new TreeMap<Integer, Map<Integer, Image>>();
    private final String urlBase;

    
    private Vector<Integer> currentlyLoading = new Vector<Integer>();


	public TileCache(String urlBase) {
        this.urlBase = urlBase;
	}
    
    
    /**
     * Get the tile's information where the coordinate is in.
     */
    public static TileInformation pos2tile(EastNorth pos, int zoom) {
        int id = 0;
        double pivotE = 0, pivotN = 0;
        double size = worldDimension;
        int bit = 1;
        for (int i = 0; i < zoom; ++i) {
            size /= 2;

            if (pos.east() < pivotE) {
                id |= bit;
                pivotE -= size;
            } else
                pivotE += size;
            bit *= 2;

            if (pos.north() < pivotN) {
                id |= bit;
                pivotN -= size;
            } else
                pivotN += size;
            bit *= 2;
        }
        return new TileCache.TileInformation(id, pos.east()-pivotE, pos.north()-pivotN, zoom, pos, Main.proj.eastNorth2latlon(new EastNorth(pivotE-size, pivotN-size)), Main.proj.eastNorth2latlon(new EastNorth(pivotE+size, pivotN+size)));
    }

    /**
     * Get the surrounding bounds of the tile.
     */
    public static Bounds tile2pos(int tileId, int zoom) {
        int id = 0;
        double pivotE = 0, pivotN = 0;
        double size = worldDimension;
        int bit = 1;
        for (int i = 0; i < zoom; ++i) {
            size /= 2;
            
            if ((id | bit) != 0)
                pivotE += size;
            else
                pivotE -= size;
            bit *= 2;
            
            if ((id | bit) != 0)
                pivotN += size;
            else
                pivotN -= size;
            bit *= 2;
        }
        return new Bounds(
                Main.proj.eastNorth2latlon(new EastNorth(pivotE-size, pivotN-size)),
                Main.proj.eastNorth2latlon(new EastNorth(pivotE+size, pivotN+size)));
    }

    /**
     * Get the image for the specified tile.
     */
    public synchronized Image get(final int tileId, final int zoom) {
        final File cacheDir = new File(Main.pref.get("cache.directory", Preferences.getPreferencesDir()+"cache")+"/"+Main.proj.getCacheDirectoryName()+"/"+zoom);
        if (!cache.containsKey(zoom)) {
            HashMap<Integer, Image> map = new HashMap<Integer, Image>();
            if (!cacheDir.exists())
                cacheDir.mkdirs();
            for (File f : cacheDir.listFiles())
                map.put(Integer.parseInt(f.getName()), loading);
            cache.put(zoom, map);
        }
        final Map<Integer, Image> map = cache.get(zoom);
        Image img = map.get(tileId);
        if (img == loading) {
            // load from disk
            System.out.println("loading from disk "+cacheDir.getPath()+"/"+tileId);
            img = Toolkit.getDefaultToolkit().createImage(cacheDir.getPath()+"/"+tileId);
            map.put(tileId, img);
        } else if (img == null) {
            img = loading;
            if (!currentlyLoading.contains(tileId*256+zoom)) {
                currentlyLoading.add(tileId*256+zoom);
                // load from network
                new Thread(){
                    @Override
                    public void run() {
                        try {
                            Bounds b = tile2pos(tileId, zoom);
                            URL url = new URL(urlBase+"bbox="+b.min.lon()+","+b.min.lat()+","+b.max.lon()+","+b.max.lat());
                            System.out.println("loading from net "+url);
                            InputStream in = url.openStream();
                            OutputStream out = new FileOutputStream(cacheDir+"/"+tileId);
                            byte[] buf = new byte[8192];
                            for (int read = in.read(buf); read > 0; read = in.read(buf))
                                out.write(buf, 0, read);
                            in.close();
                            out.close();
                            map.put(tileId, loading);
                            currentlyLoading.remove(tileId*256+zoom);
                            Main.main.repaint();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }        
                    }
                }.start();
            }
        } else {
            System.out.println("providing from cache "+img.getWidth(null));
        }
        System.out.println(img.getWidth(null));
        return img;
    }
}
