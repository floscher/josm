package org.openstreetmap.josm.data;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.List;

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
	 * Whether nodes on the same place should be considered identical.
	 */
	public boolean mergeNodes = true;
	
	

	/**
	 * List of all available Projections.
	 */
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
			mergeNodes = root.getChild("mergeNodes") != null;
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
		
		List children = root.getChildren();
		children.add(new Element("laf").setText(laf.getClassName()));
		children.add(new Element("projection").setText(projection.getClass().getName()));
		if (mergeNodes)
			children.add(new Element("mergeNodes"));

		try {
			final FileWriter file = new FileWriter(getPreferencesFile());
			new XMLOutputter(Format.getPrettyFormat()).output(root, file);
			file.close();
		} catch (Exception e) {
			throw new PreferencesException("Could not write preferences", e);
		}
	}
}
