package org.openstreetmap.josm.data.projection;

import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.coor.EastNorth;

/**
 * Classes subclass this are able to convert lat/lon values to 
 * planear screen coordinates.
 * 
 * @author imi
 */
public interface Projection {

	public static double MAX_LAT = 85.05112877980659; // Mercator squares the world
	public static double MAX_LON = 180;
	public static final double MAX_SERVER_PRECISION = 1e12;

	/**
	 * List of all available Projections.
	 */
	public static final Projection[] allProjections = new Projection[]{
		new Epsg4326(),
		new Mercator()
	};
	
	/**
	 * Convert from lat/lon to northing/easting. 
	 * 
	 * @param p		The geo point to convert. x/y members of the point are filled.
	 */
	EastNorth latlon2eastNorth(LatLon p);
	
	/**
	 * Convert from norting/easting to lat/lon.
	 * 
	 * @param p		The geo point to convert. lat/lon members of the point are filled.
	 */
	LatLon eastNorth2latlon(EastNorth p);

	/**
	 * Describe the projection converter in one or two words.
	 */
	String toString();
    
    /**
     * Get a filename compatible string (for the cache directory)
     */
    String getCacheDirectoryName();
}
