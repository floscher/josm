package org.openstreetmap.josm.data;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;

import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.openstreetmap.josm.data.projection.LatitudeLongitude;
import org.openstreetmap.josm.data.projection.Projection;
import org.openstreetmap.josm.data.projection.UTM;


/**
 * This class holds all preferences for JOSM.
 * 
 * @author imi
 */
public class Preferences {

	/**
	 * The look and feel. Classname of the look and feel class to use.
	 */
	public LookAndFeelInfo laf = UIManager.getInstalledLookAndFeels()[0];

	/**
	 * The convertor used to translate lat/lon points to screen points.
	 */
	public Projection projection = new UTM();

	/**
	 * The monitor geometry in meter per pixel. (How big is 1 pixel in meters)
	 * Example: 17" Sony flatscreen in 1280x1024 mode: 0.000264 ppm
	 * 
	 * Remember: ppm = dpi/25400
	 */
	public double ppm = 0.000264;

	
	public static final Projection[] allProjections = new Projection[]{
		new UTM(),
		new LatitudeLongitude()
	};

	/**
	 * Return the location of the preferences file
	 */
	public static String getPreferencesFile() {
		return System.getProperty("user.home")+"/.josm-preferences";
	}
	
	/**
	 * Exception thrown in case of any loading/saving error (including parse errors).
	 * @author imi
	 */
	public static class PreferencesException extends Exception {
		public PreferencesException(String message, Throwable cause) {
			super(message, cause);
		}
	}
	/**
	 * Load from disk.
	 * @throws PreferencesException Any loading error (parse errors as well)
	 */
	public void load() throws PreferencesException {
		File file = new File(System.getProperty("user.home")+"/.josm-preferences");
		Element root;
		try {
			root = new SAXBuilder().build(new FileReader(file)).getRootElement();

			// laf
			String lafClassName = root.getChildText("laf");
			for (LookAndFeelInfo lafInfo : UIManager.getInstalledLookAndFeels())
				if (lafInfo.getClassName().equals(lafClassName)) {
					laf = lafInfo;
					break;
				}
			if (laf == null)
				throw new PreferencesException("Look and Feel not found.", null);
			
			projection = (Projection)Class.forName(root.getChildText("projection")).newInstance();
		} catch (Exception e) {
			if (e instanceof PreferencesException)
				throw (PreferencesException)e;
			throw new PreferencesException("Could not load preferences", e);
		}
		
	}
	/**
	 * Save to disk.
	 * @throws PreferencesException Any saving error (exceeding disk space, etc..)
	 */
	@SuppressWarnings("unchecked")
	public void save() throws PreferencesException {
		Element root = new Element("josm-settings");
		
		root.getChildren().add(new Element("laf").setText(laf.getClassName()));
		root.getChildren().add(new Element("projection").setText(projection.getClass().getName()));

		try {
			final FileWriter file = new FileWriter(getPreferencesFile());
			new XMLOutputter(Format.getPrettyFormat()).output(root, file);
			file.close();
		} catch (Exception e) {
			throw new PreferencesException("Could not write preferences", e);
		}
	}
}
