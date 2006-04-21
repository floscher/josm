package org.openstreetmap.josm.data.projection;

import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.coor.EastNorth;

/**
 * Directly use latitude / longitude values as x/y.
 *
 * @author imi
 */
public class Epsg4326 implements Projection {

	public EastNorth latlon2eastNorth(LatLon p) {
		return new EastNorth(p.lon(), p.lat());
	}

	public LatLon eastNorth2latlon(EastNorth p) {
		return new LatLon(p.north(), p.east());
	}

	@Override public String toString() {
		return "EPSG:4326";
	}

    public String getCacheDirectoryName() {
        return "epsg4326";
    }
}
