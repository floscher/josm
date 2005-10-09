package org.openstreetmap.josm.data;



/**
 * An point holding latitude/longitude and their corresponding north/east values, 
 * which may not be initialized.
 *
 * if x or y is "NaN", these are not initialized yet.
 *
 * @author imi
 */
public class GeoPoint implements Cloneable {

	/**
	 * Latitude/Longitude coordinates.
	 */
	public double lat = Double.NaN, lon = Double.NaN;

	/**
	 * East/North coordinates;
	 */
	public double x = Double.NaN, y = Double.NaN;

	/**
	 * Construct the point with latitude / longitude values.
	 * The x/y values are left uninitialized.
	 * 
	 * @param lat Latitude of the point.
	 * @param lon Longitude of the point.
	 */
	public GeoPoint(double lat, double lon) {
		this.lat = lat;
		this.lon = lon;
	}

	/**
	 * Construct the point with all values unset (set to NaN)
	 */
	public GeoPoint() {
	}

	@Override
	public GeoPoint clone() {
		try {return (GeoPoint)super.clone();} catch (CloneNotSupportedException e) {}
		return null;
	}

	/**
	 * Return the squared distance of the northing/easting values between 
	 * this and the argument.
	 *
	 * @param other The other point to calculate the distance to.
	 * @return The square of the distance between this and the other point,
	 * 		regarding to the x/y values.
	 */
	public double distanceXY(GeoPoint other) {
		return (x-other.x)*(x-other.x)+(y-other.y)*(y-other.y);
	}

	/**
	 * @return <code>true</code>, if the other GeoPoint has the same lat/lon values.
	 */
	public boolean equalsLatLon(GeoPoint other) {
		return lat == other.lat && lon == other.lon && 
				!Double.isNaN(lat) && !Double.isNaN(lon);
	}
}
