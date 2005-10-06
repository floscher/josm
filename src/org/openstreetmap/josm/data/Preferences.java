package org.openstreetmap.josm.data;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
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
	private LookAndFeelInfo laf = UIManager.getInstalledLookAndFeels()[0];
	/**
	 * The convertor used to translate lat/lon points to screen points.
	 */
	private Projection projection = new UTM();
	/**
	 * Whether nodes on the same place should be considered identical.
	 */
	private boolean mergeNodes = true;



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
	 * List of all available Projections.
	 */
	public static final Projection[] allProjections = new Projection[]{
		new UTM(),
		new LatitudeLongitude()
	};




	// listener stuff
	
	/**
	 * The event listener list
	 */
	private List<PropertyChangeListener> listener = new LinkedList<PropertyChangeListener>();
	/**
	 * If <code>listener != null</code>, add it to the listener list.
	 */
	public void addPropertyChangeListener(PropertyChangeListener listener) {
		if (listener != null)
			this.listener.add(listener);
	}
	/**
	 * If <code>listener != null</code>, remove it from the listener list.
	 */
	public void removePropertyChangeListener(PropertyChangeListener listener) {
		if (listener != null)
			this.listener.remove(listener);
	}
	/**
	 * Fires an event that the property has changed.
	 */
	private void firePropertyChanged(String propName, Object oldValue, Object newValue) {
		PropertyChangeEvent event = null;
		for (PropertyChangeListener l : listener) {
			if (event == null)
				event = new PropertyChangeEvent(this, propName, oldValue, newValue);
			l.propertyChange(event);
		}
	}

	
	
	/**
	 * Return the location of the preferences file
	 */
	public static String getPreferencesFile() {
		return System.getProperty("user.home")+"/.josm-preferences";
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
					setLaf(lafInfo);
					break;
				}
			if (getLaf() == null)
				throw new PreferencesException("Look and Feel not found.", null);

			// set projection
			Class<?> projectionClass = Class.forName(root.getChildText("projection"));
			projection = allProjections[0]; // defaults to UTM
			for (Projection p : allProjections) {
				if (p.getClass() == projectionClass) {
					projection = p;
					break;
				}
			}

			setMergeNodes(root.getChild("mergeNodes") != null);
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
		children.add(new Element("laf").setText(getLaf().getClassName()));
		children.add(new Element("projection").setText(getProjection().getClass().getName()));
		if (isMergeNodes())
			children.add(new Element("mergeNodes"));

		try {
			final FileWriter file = new FileWriter(getPreferencesFile());
			new XMLOutputter(Format.getPrettyFormat()).output(root, file);
			file.close();
		} catch (Exception e) {
			throw new PreferencesException("Could not write preferences", e);
		}
	}

	// getter / setter

	public void setProjection(Projection projection) {
		Projection old = this.projection;
		this.projection = projection;
		firePropertyChanged("projection", old, projection);
	}
	public Projection getProjection() {
		return projection;
	}
	public void setMergeNodes(boolean mergeNodes) {
		boolean old = this.mergeNodes;
		this.mergeNodes = mergeNodes;
		firePropertyChanged("mergeNodes", old, mergeNodes);
	}
	public boolean isMergeNodes() {
		return mergeNodes;
	}
	public void setLaf(LookAndFeelInfo laf) {
		LookAndFeelInfo old = this.laf;
		this.laf = laf;
		firePropertyChanged("laf", old, laf);
	}
	public LookAndFeelInfo getLaf() {
		return laf;
	}
}
