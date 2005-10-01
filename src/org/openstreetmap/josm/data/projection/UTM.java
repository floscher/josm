package org.openstreetmap.josm.data.projection;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.border.Border;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.GeoPoint;
import org.openstreetmap.josm.data.osm.DataSet;

/**
 * A java port of a ruby port of a C port of a Projection from the 
 * Defense Mapping agency. ;-)
 * 
 * The whole dataset is interpreted as beeing in one zone. This 
 * zone is initially taken from the center lat/lon value but can
 * be configured to any other zone. Same is for the hemisphere.
 * 
 * C code by Chuck Gantz
 * Ruby port by Ben Gimpert
 * modified Java port by imi (myself)
 *
 * @author imi
 */
public class UTM extends Projection {

	public final static double DEG_TO_RAD = Math.PI / 180;
	public final static double RAD_TO_DEG = 180 / Math.PI;

	/**
	 * A reference ellipsoid used in Projections
	 */
	public static class Ellipsoid {
		String name;
		double a, ecc_squared;
		Ellipsoid(String name, double a, double ecc_squared) {
			this.name = name;
			this.a = a;
			this.ecc_squared = ecc_squared;
		}
		@Override
		public String toString() {
			return name;
		}
	}

	/**
	 * All available reference ellipsoids.
	 */
	public final static Ellipsoid[] allEllipsoids = new Ellipsoid[] {
		new Ellipsoid("Airy", 6377563, 0.00667054),
		new Ellipsoid("Australian National", 6378160, 0.006694542),
		new Ellipsoid("Bessel 1841", 6377397, 0.006674372),
		new Ellipsoid("Clarke 1866", 6378206, 0.006768658),
		new Ellipsoid("Clarke 1880", 6378249, 0.006803511),
		new Ellipsoid("Everest", 6377276, 0.006637847),
		new Ellipsoid("Fischer Mercury 1960", 6378166, 0.006693422),
		new Ellipsoid("Fischer 1968", 6378150, 0.006693422),
		new Ellipsoid("GRS 1967", 6378160, 0.006694605),
		new Ellipsoid("GRS 1980", 6378137, 0.00669438),
		new Ellipsoid("Helmert 1906", 6378200, 0.006693422),
		new Ellipsoid("Hough", 6378270, 0.00672267),
		new Ellipsoid("Krassovsky", 6378245, 0.006693422),
		new Ellipsoid("WGS-60", 6378165, 0.006693422),
		new Ellipsoid("WGS-66", 6378145, 0.006694542),
		new Ellipsoid("WGS-72", 6378135, 0.006694318),
		new Ellipsoid("WGS-84", 6378137, 0.00669438)
	};

	private enum Hemisphere {north, south};

	/**
	 * What hemisphere the whole map is in.
	 */
	private Hemisphere hemisphere = Hemisphere.north;
	/**
	 * What zone the whole map is in.
	 */
	private int zone = 0; // 0 means not initialized
	/**
	 * Reference ellipsoid used in projection
	 */
	protected Ellipsoid ellipsoid = allEllipsoids[allEllipsoids.length-1];

	
	@Override
	public void latlon2xy(GeoPoint p) {
		// converts lat/long to UTM coords. Equations from USGS Bulletin 1532
		// North latitudes are positive, South latitudes are negative
		// East longitudes are positive, West longitudes are negative
		// lat and long are in decimal degrees
		// Written by Chuck Gantz- chuck.gantz@globalstar.com
		// ported to Ruby by Ben Gimpert- ben@somethingmodern.com
		double k0 = 0.9996012717;
		
		double lat_rad = p.lat * DEG_TO_RAD;
		double long_temp = (p.lon + 180) - (Math.floor((p.lon + 180) / 360) * 360) - 180;
		double long_rad = long_temp * DEG_TO_RAD;
		
		double long_origin = (zone - 1)*6 - 180 + 3;  // +3 puts origin in middle of zone
		double long_origin_rad = long_origin * DEG_TO_RAD;
		
		double ecc_prime_squared = ellipsoid.ecc_squared / (1 - ellipsoid.ecc_squared);
		
		double n = ellipsoid.a / Math.sqrt(1 - ellipsoid.ecc_squared * Math.sin(lat_rad) * Math.sin(lat_rad));
		double t = Math.tan(lat_rad) * Math.tan(lat_rad);
		double c = ecc_prime_squared * Math.cos(lat_rad) * Math.cos(lat_rad);
		double a = Math.cos(lat_rad) * (long_rad - long_origin_rad);

		double e2 = ellipsoid.ecc_squared*ellipsoid.ecc_squared;
		double e3 = e2*ellipsoid.ecc_squared;
		double m = ellipsoid.a * (((1 - ellipsoid.ecc_squared/4 - 3*e2/64 - 5*e3/256)*lat_rad) - ((3*ellipsoid.ecc_squared/8 + 3*e2/32 + 45*e3/1024)*Math.sin(2*lat_rad)) + ((15*e2/256 + 45*e3/1024)*Math.sin(4*lat_rad)) - ((35*e3/3072)*Math.sin(6*lat_rad)));

		p.x = k0*n*(a+(1-t+c)*a*a*a/6 + (5-18*t+t*t+72*c-58*ecc_prime_squared)*a*a*a*a*a/120) + 500000.0;
		p.y = k0*(m+n*Math.tan(lat_rad)*(a*a/2+(5-t+9*c+4*c*c)*a*a*a*a/24 + (61-58*t+t*t+600*c-330*ecc_prime_squared)*a*a*a*a*a*a/720));
		if (p.lat < 0)
			p.y += 10000000.0; // offset for southern hemisphere
    }

	@Override
	public void xy2latlon(GeoPoint p) {
		// converts UTM coords to lat/long.  Equations from USGS Bulletin 1532 
		// East longitudes are positive, West longitudes are negative. 
		// North latitudes are positive, South latitudes are negative
		// lat and long are in decimal degrees. 
		// Written by Chuck Gantz- chuck.gantz@globalstar.com
		// ported to Ruby by Ben Gimpert- ben@somethingmodern.com
		double k0 = 0.9996;
		double e1 = (1-Math.sqrt(1-ellipsoid.ecc_squared))/(1+Math.sqrt(1-ellipsoid.ecc_squared));
		double x = p.x - 500000.0;
		double y = p.y;
		if (hemisphere == Hemisphere.south)
			y -= 10000000.0;
		
		double long_origin = (zone - 1)*6 - 180 + 3;  // +3 puts origin in middle of zone
		double ecc_prime_squared = ellipsoid.ecc_squared / (1 - ellipsoid.ecc_squared);
		double m = y / k0;
		double mu = m / (ellipsoid.a*(1-ellipsoid.ecc_squared/4-3*ellipsoid.ecc_squared*ellipsoid.ecc_squared/64-5*ellipsoid.ecc_squared*ellipsoid.ecc_squared*ellipsoid.ecc_squared/256));
		
		double phi1_rad = mu + (3*e1/2-27*e1*e1*e1/32)*Math.sin(2*mu) + (21*e1*e1/16-55*e1*e1*e1*e1/32)*Math.sin(4*mu) +(151*e1*e1*e1/96)*Math.sin(6*mu);
		
		double n1 = ellipsoid.a/Math.sqrt(1-ellipsoid.ecc_squared*Math.sin(phi1_rad)*Math.sin(phi1_rad));
		double t1 = Math.tan(phi1_rad)*Math.tan(phi1_rad);
		double c1 = ecc_prime_squared*Math.cos(phi1_rad)*Math.cos(phi1_rad);
		double r1 = ellipsoid.a*(1-ellipsoid.ecc_squared)/Math.pow((1-ellipsoid.ecc_squared*Math.sin(phi1_rad)*Math.sin(phi1_rad)), 1.5);
		double d = x / (n1*k0);
		
		p.lat = phi1_rad - (n1*Math.tan(phi1_rad)/r1)*(d*d/2-(5+3*t1+10*c1-4*c1*c1-9*ecc_prime_squared)*d*d*d*d/24 + (61+90*t1+298*c1+45*t1*t1-252*ecc_prime_squared-3*c1*c1)*d*d*d*d*d*d/720);
		p.lat = p.lat * RAD_TO_DEG;
		
		p.lon = (d-(1+2*t1+c1)*d*d*d/6+(5-2*c1+28*t1-3*c1*c1+8*ecc_prime_squared+24*t1*t1)*d*d*d*d*d/120)/Math.cos(phi1_rad);
		p.lon = long_origin + (p.lon * RAD_TO_DEG);
	}

	@Override
	public String toString() {
		return "UTM";
	}

	@Override
	public String description() {
		return "UTM projection ported from Ben Gimpert's ruby port.\n" +
			"http://www.openstreetmap.org/websvn/filedetails.php?repname=" +
			"OpenStreetMap&path=%2Futils%2Ftiger_import%2Ftiger%2Futm.rb";
	}

	/**
	 * If the zone is not already set, calculate it from this dataset. 
	 * If the dataset span over more than one zone, take the middle one 
	 * (the zone of the middle lat/lon).
	 * Also, calculate the hemisphere (northern/southern).
	 */
	@Override
	public void init(DataSet dataSet) {
		if (zone == 0) {
			Bounds b = dataSet.getBoundsLatLon();
			if (b == null)
				return;
			GeoPoint center = b.centerLatLon();
			double lat = center.lat;
			double lon = center.lon;
			// make sure the longitude is between -180.00 .. 179.9
			double long_temp = (lon + 180) - (Math.floor((lon + 180) / 360) * 360) - 180;
			
			zone = (int)((long_temp + 180) / 6) + 1;
			if ((lat >= 56.0) && (lat < 64.0) && (long_temp >= 3.0) && (long_temp < 12.0))
				zone = 32; 
			// special zones for Svalbard
			if ((lat >= 72.0) && (lat < 84.0))
			{
				if ((long_temp >= 0.0) && (long_temp < 9.0))
					zone = 31;
				else if ((long_temp >= 9.0) && (long_temp < 21.0))
					zone = 33;
				else if ((long_temp >= 21.0) && (long_temp < 33.0))
					zone = 35;
				else if ((long_temp >= 33.0) && (long_temp < 42.0))
					zone = 37;
			}
			hemisphere = lat > 0 ? Hemisphere.north : Hemisphere.south;
		}
		super.init(dataSet);
	}

	@Override
	public JComponent getConfigurationPanel() {
		Border border = BorderFactory.createEmptyBorder(5,0,0,0);
		Box panel = Box.createVerticalBox();

		// ellipsoid
		Box ellipsoidPanel = Box.createHorizontalBox();
		ellipsoidPanel.add(new JLabel("Ellipsoid"));
		final JComboBox ellipsoidCombo = new JComboBox(allEllipsoids);
		ellipsoidPanel.add(ellipsoidCombo);
		ellipsoidCombo.setSelectedItem(ellipsoid);
		ellipsoidCombo.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				ellipsoid = (Ellipsoid)ellipsoidCombo.getSelectedItem();
				fireStateChanged();
			}
		});
		ellipsoidPanel.setBorder(border);
		panel.add(ellipsoidPanel);
		
		// zone
		Box zonePanel = Box.createHorizontalBox();
		zonePanel.add(new JLabel("Zone"));
		final JSpinner zoneSpinner = new JSpinner(new SpinnerNumberModel(zone,1,60,1));
		zonePanel.add(zoneSpinner);
		zoneSpinner.addChangeListener(new ChangeListener(){
			public void stateChanged(ChangeEvent e) {
				zone = (Integer)zoneSpinner.getValue();
				fireStateChanged();
			}
		});
		zonePanel.setBorder(border);
		panel.add(zonePanel);
		
		// hemisphere
		Box hemispherePanel = Box.createHorizontalBox();
		hemispherePanel.add(new JLabel("Hemisphere"));
		final JComboBox hemisphereCombo = new JComboBox(Hemisphere.values());
		hemispherePanel.add(hemisphereCombo);
		hemisphereCombo.setSelectedItem(hemisphere);
		hemisphereCombo.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				hemisphere = (Hemisphere)hemisphereCombo.getSelectedItem();
				fireStateChanged();
			}
		});
		hemispherePanel.setBorder(border);
		panel.add(hemispherePanel);

		return panel;
	}
}
