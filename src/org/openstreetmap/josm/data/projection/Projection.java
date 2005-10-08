package org.openstreetmap.josm.data.projection;

import java.util.LinkedList;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.openstreetmap.josm.data.GeoPoint;
import org.openstreetmap.josm.data.osm.DataSet;

/**
 * Classes subclass this are able to convert lat/lon values to 
 * planear screen coordinates.
 * 
 * @author imi
 */
abstract public class Projection implements Cloneable {

	/**
	 * The event list with all state chaned listener
	 */
	List<ChangeListener> listener = new LinkedList<ChangeListener>();
	
	/**
	 * Convert from lat/lon to northing/easting. 
	 * 
	 * @param p		The geo point to convert. x/y members of the point are filled.
	 */
	abstract public void latlon2xy(GeoPoint p);
	
	/**
	 * Convert from norting/easting to lat/lon.
	 * 
	 * @param p		The geo point to convert. lat/lon members of the point are filled.
	 */
	abstract public void xy2latlon(GeoPoint p);

	
	// description functions
	
	/**
	 * Describe the projection converter in one or two words.
	 */
	@Override
	abstract public String toString();
	
	// miscellous functions
	
	/**
	 * If the projection supports any configuration, this function return
	 * the configuration panel. If no configuration needed, 
	 * return <code>null</code>.
	 * 
	 * The items on the configuration panel should not update the configuration
	 * directly, but remember changed settings so a call to commitConfigurationPanel
	 * can set them.
	 * 
	 * This function also rolls back all changes to the configuration panel interna
	 * components.
	 */
	abstract public JComponent getConfigurationPanel();
	/**
	 * Commits any changes from components created by addToConfigurationPanel.
	 * The projection should now obtain the new settings. If any setting has
	 * changed, the implementation have to call to fireStateChanged to inform
	 * the listeners.
	 */
	abstract public void commitConfigurationPanel();

	/**
	 * Initialize itself with the given dataSet.
	 * 
	 * This function should initialize own parameters needed to do the
	 * projection at best effort.
	 * 
	 * Init must not fire an state changed event, since it is usually called
	 * during the initialization of the mapFrame.
	 *
	 * This implementation does nothing. It is provided only for subclasses
	 * to initialize their data members.
	 * 
	 * @param dataSet
	 *            The dataset, which will be displayed on screen. Later, all
	 *            projections should be relative to the given dataset. Any
	 *            reverse projections (xy2latlon) can be assumed to be in near
	 *            distance to nodes of this dataset (that means, it is ok, if
	 *            there is a conversion error, if the requested x/y to xy2latlon
	 *            is far away from any coordinate in the dataset)
	 */
	public void init(DataSet dataSet) {}
	
	/**
	 * Add an event listener to the state changed event queue. If passed 
	 * <code>null</code>, nothing happens.
	 */
	public final void addChangeListener(ChangeListener l) {
		if (l != null)
			listener.add(l);
	}
	/**
	 * Remove an event listener from the event queue. If passed 
	 * <code>null</code>, nothing happens.
	 */
	public final void removeChangeListener(ChangeListener l) {
		listener.remove(l);
	}
	/**
	 * Fire an ChangeEvent to every listener on the queue.
	 */
	public final void fireStateChanged() {
		ChangeEvent e = null;
		for(ChangeListener l : listener) {
			if (e == null)
				e = new ChangeEvent(this);
			l.stateChanged(e);
		}
	}
}
