package org.openstreetmap.josm.data.projection;

import org.openstreetmap.josm.data.GeoPoint;

/**
 * Classes subclass this are able to convert lat/lon values to 
 * planear screen coordinates.
 * 
 * @author imi
 */
public interface Projection {

	public static double MAX_LAT = 85;
	public static double MAX_LON = 180;
	
	/**
	 * Convert from lat/lon to northing/easting. 
	 * 
	 * @param p		The geo point to convert. x/y members of the point are filled.
	 */
	void latlon2xy(GeoPoint p);
	
	/**
	 * Convert from norting/easting to lat/lon.
	 * 
	 * @param p		The geo point to convert. lat/lon members of the point are filled.
	 */
	void xy2latlon(GeoPoint p);

	
	// description functions
	
	/**
	 * Describe the projection converter in one or two words.
	 */
	String toString();
}
