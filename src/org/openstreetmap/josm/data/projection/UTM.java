package org.openstreetmap.josm.data.projection;

import java.awt.Font;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.GeoPoint;
import org.openstreetmap.josm.gui.GBC;

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

	public final static double k0 = 0.9996012717;
	
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

	private enum Hemisphere {north, south}

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

	/**
	 * Combobox with all ellipsoids for the configuration panel
	 */
	private JComboBox ellipsoidCombo;
	/**
	 * Spinner with all possible zones for the configuration panel
	 */
	JSpinner zoneSpinner;
	/**
	 * Hemisphere combo for the configuration panel
	 */
	JComboBox hemisphereCombo;

	
	@Override
	public void latlon2xy(GeoPoint p) {
		// converts lat/long to UTM coords. Equations from USGS Bulletin 1532
		// North latitudes are positive, South latitudes are negative
		// East longitudes are positive, West longitudes are negative
		// lat and long are in decimal degrees
		// Written by Chuck Gantz- chuck.gantz@globalstar.com
		// ported to Ruby by Ben Gimpert- ben@somethingmodern.com
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

	/**
	 * Helper class for the zone detection
	 * @author imi
	 */
	private static class ZoneData {
		int zone = 0;
		Hemisphere hemisphere = Hemisphere.north;
	}
	/**
	 * Try to autodetect the zone and hemisphere from the dataset.
	 * @return The zone data extrakted from the dataset.
	 */
	ZoneData autoDetect(Bounds b) {
		ZoneData zd = new ZoneData();
		if (b == null)
			return zd;
		GeoPoint center = b.centerLatLon();
		double lat = center.lat;
		double lon = center.lon;
		// make sure the longitude is between -180.00 .. 179.9
		double long_temp = (lon + 180) - (Math.floor((lon + 180) / 360) * 360) - 180;
		
		zd.zone = (int)((long_temp + 180) / 6) + 1;
		if ((lat >= 56.0) && (lat < 64.0) && (long_temp >= 3.0) && (long_temp < 12.0))
			zd.zone = 32; 
		// special zones for Svalbard
		if ((lat >= 72.0) && (lat < 84.0))
		{
			if ((long_temp >= 0.0) && (long_temp < 9.0))
				zd.zone = 31;
			else if ((long_temp >= 9.0) && (long_temp < 21.0))
				zd.zone = 33;
			else if ((long_temp >= 21.0) && (long_temp < 33.0))
				zd.zone = 35;
			else if ((long_temp >= 33.0) && (long_temp < 42.0))
				zd.zone = 37;
		}
		zd.hemisphere = lat > 0 ? Hemisphere.north : Hemisphere.south;
		return zd;
	}
	
	/**
	 * If the zone is not already set, calculate it from this dataset. 
	 * If the dataset span over more than one zone, take the middle one 
	 * (the zone of the middle lat/lon).
	 * Also, calculate the hemisphere (northern/southern).
	 */
	@Override
	public void init(Bounds b) {
		if (zone == 0) {
			ZoneData zd = autoDetect(b);
			zone = zd.zone;
			hemisphere = zd.hemisphere;
		}
	}

	@Override
	public JComponent getConfigurationPanel() {
		JPanel panel = new JPanel(new GridBagLayout());
		GBC gbc = GBC.std().insets(0,0,5,0);
		
		// ellipsoid
		if (ellipsoidCombo == null)
			ellipsoidCombo = new JComboBox(allEllipsoids);
		panel.add(new JLabel("Ellipsoid"), gbc);
		panel.add(ellipsoidCombo, GBC.eol());
		ellipsoidCombo.setSelectedItem(ellipsoid);
		
		// zone
		if (zoneSpinner == null)
			zoneSpinner = new JSpinner(new SpinnerNumberModel(1,1,60,1));
		panel.add(new JLabel("Zone"), gbc);
		panel.add(zoneSpinner, GBC.eol().insets(0,5,0,5));
		if (zone != 0)
			zoneSpinner.setValue(zone);
		
		// hemisphere
		if (hemisphereCombo == null)
			hemisphereCombo = new JComboBox(Hemisphere.values());
		panel.add(new JLabel("Hemisphere"), gbc);
		panel.add(hemisphereCombo, GBC.eop());
		hemisphereCombo.setSelectedItem(hemisphere);

		// Autodetect
		JButton autoDetect = new JButton("Detect");
		autoDetect.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				if (Main.main.getMapFrame() != null) {
					ZoneData zd = autoDetect(Main.main.ds.getBoundsLatLon());
					if (zd.zone == 0)
						JOptionPane.showMessageDialog(Main.main, "Autodetection failed. Maybe the data set contain too few information.");
					else {
						zoneSpinner.setValue(zd.zone);
						hemisphereCombo.setSelectedItem(zd.hemisphere);
					}
				} else {
					JOptionPane.showMessageDialog(Main.main, "No data loaded. Please open a data set first.");
				}
			}
		});
		JLabel descLabel = new JLabel("Autodetect parameter based on loaded data");
		descLabel.setFont(descLabel.getFont().deriveFont(Font.ITALIC));
		panel.add(descLabel, GBC.eol().fill(GBC.HORIZONTAL));
		panel.add(autoDetect, GBC.eol().anchor(GBC.CENTER));
		
		return panel;
	}

	@Override
	public void commitConfigurationPanel() {
		if (ellipsoidCombo != null && zoneSpinner != null && hemisphereCombo != null) {
			ellipsoid = (Ellipsoid)ellipsoidCombo.getSelectedItem();
			zone = (Integer)zoneSpinner.getValue();
			hemisphere = (Hemisphere)hemisphereCombo.getSelectedItem();
			fireStateChanged();
		}
	}
}
