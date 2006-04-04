package org.openstreetmap.josm.data;

import java.util.Collection;
import java.util.LinkedList;

import javax.swing.SwingUtilities;

import org.openstreetmap.josm.data.osm.OsmPrimitive;

/**
 * This class is to help the DataSet collecting and fire selectionChanged events.
 * For more, @see org.openstreetmap.josm.data.SelectionChangedListener
 * 
 * @author imi
 */
abstract public class SelectionTracker {

	/**
	 * Collects the selction changed events. The first collector that runs in 
	 * one queue, starts the purger.
	 * @author imi
	 */
	private final class Collector implements Runnable {
		public void run() {
			switch (state) {
			case WAITING:
				throw new IllegalStateException();
			case COLLECTING:
				state = SelectionEventState.PURGING;
				SwingUtilities.invokeLater(new Purger());
				break;
			case PURGING:
				break; // still purging events.
			}
		}
	}

	/**
	 * Informs the listener clients and go back to waiting state.
	 * @author imi
	 */
	private final class Purger implements Runnable {
		public void run() {
			if (state != SelectionEventState.PURGING)
				throw new IllegalStateException();
			state = SelectionEventState.WAITING;
			Collection<OsmPrimitive> sel = getSelected();
			for (SelectionChangedListener l : listeners)
				l.selectionChanged(sel);
		}
	}

	/**
	 * The event state for the selection dispatching. WAITING means we are
	 * waiting for any fireSelectionChanged event. COLLECTING means, we have
	 * already some events in the EventQueue and are now collecting more events.
	 * PURGING means, the collecting phase is over and we wait now for the finish
	 * event to just contact the listeners.
	 * @author imi
	 */
	private enum SelectionEventState {WAITING, COLLECTING, PURGING}

	/**
	 * The state, regarding to the selection changing that we are in.
	 */
	transient SelectionEventState state = SelectionEventState.WAITING;

	/**
	 * A list of listeners to selection changed events.
	 */
	transient Collection<SelectionChangedListener> listeners = new LinkedList<SelectionChangedListener>();

	
	/**
	 * Add a listener to the selection changed listener list. If <code>null</code>
	 * is passed, nothing happens.
	 * @param listener The listener to add to the list.
	 */
	public void addSelectionChangedListener(SelectionChangedListener listener) {
		if (listener != null)
			listeners.add(listener);
	}
	
	/**
	 * Remove a listener from the selection changed listener list. 
	 * If <code>null</code> is passed, nothing happens.
	 * @param listener The listener to remove from the list.
	 */
	public void removeSelectionChangedListener(SelectionChangedListener listener) {
		if (listener != null)
			listeners.remove(listener);
	}

	/**
	 * Remember to fire an selection changed event. A call to this will not fire
	 * the event immediately. For more, @see SelectionChangedListener
	 */
	public void fireSelectionChanged() {
		if (state == SelectionEventState.WAITING) {
			state = SelectionEventState.COLLECTING;
			SwingUtilities.invokeLater(new Collector());
		}
	}
	
	/**
	 * This function is needed by the Purger to get the actual selection.
	 * @return The selected primitives.
	 */
	abstract public Collection<OsmPrimitive> getSelected();
}
