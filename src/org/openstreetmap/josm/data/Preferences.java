package org.openstreetmap.josm.data;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.Collection;
import java.util.LinkedList;
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
	private Projection projection = new UTM();


	/**
	 * Whether lines should be drawn between track points of raw gps data.
	 */
	private boolean drawRawGpsLines = false;
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
			
			// projection
			Class<?> projectionClass = Class.forName(root.getChildText("projection"));
			projection = allProjections[0]; // defaults to UTM
			for (Projection p : allProjections) {
				if (p.getClass() == projectionClass) {
					projection = p;
					break;
				}
			}

			mergeNodes = root.getChild("mergeNodes") != null;
			drawRawGpsLines = root.getChild("drawRawGpsLines") != null;
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
		children.add(new Element("projection").setText(getProjection().getClass().getName()));
		if (mergeNodes)
			children.add(new Element("mergeNodes"));
		if (drawRawGpsLines)
			children.add(new Element("drawRawGpsLines"));

		try {
			final FileWriter file = new FileWriter(getPreferencesFile());
			new XMLOutputter(Format.getPrettyFormat()).output(root, file);
			file.close();
		} catch (Exception e) {
			throw new PreferencesException("Could not write preferences", e);
		}
	}

	
	// projection change listener stuff
	
	/**
	 * The list of all listeners to projection changes.
	 */
	private Collection<PropertyChangeListener> listener = new LinkedList<PropertyChangeListener>();
	/**
	 * Add a listener of projection changes to the list of listeners.
	 * @param listener The listerner to add.
	 */
	public void addPropertyChangeListener(PropertyChangeListener listener) {
		if (listener != null)
			this.listener.add(listener);
	}
	/**
	 * Remove the listener from the list.
	 */
	public void removePropertyChangeListener(PropertyChangeListener listener) {
		this.listener.remove(listener);
	}
	/**
	 * Fires a PropertyChangeEvent if the old value differs from the new value.
	 */
	private <T> void firePropertyChanged(String name, T oldValue, T newValue) {
		if (oldValue == newValue)
			return;
		PropertyChangeEvent evt = null;
		for (PropertyChangeListener l : listener) {
			if (evt == null)
				evt = new PropertyChangeEvent(this, name, oldValue, newValue);
			l.propertyChange(evt);
		}
	}

	// getter / setter
	
	/**
	 * Set the projection and fire an event to all ProjectionChangeListener
	 * @param projection The new Projection.
	 */
	public void setProjection(Projection projection) {
		Projection old = this.projection;
		this.projection = projection;
		firePropertyChanged("projection", old, projection);
	}
	/**
	 * Get the current projection.
	 * @return The current projection set.
	 */
	public Projection getProjection() {
		return projection;
	}
	public void setDrawRawGpsLines(boolean drawRawGpsLines) {
		boolean old = this.drawRawGpsLines;
		this.drawRawGpsLines = drawRawGpsLines;
		firePropertyChanged("drawRawGpsLines", old, drawRawGpsLines);
	}
	public boolean isDrawRawGpsLines() {
		return drawRawGpsLines;
	}
}
