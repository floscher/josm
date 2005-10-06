package org.openstreetmap.josm.gui;

import java.awt.AWTEvent;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.AWTEventListener;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.util.Map.Entry;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.Popup;
import javax.swing.PopupFactory;

import org.openstreetmap.josm.data.GeoPoint;
import org.openstreetmap.josm.data.osm.Key;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.visitor.SelectionComponentVisitor;

/**
 * A component that manages some status information display about the map.
 * It keeps a status line below the map up to date and displays some tooltip
 * information if the user hold the mouse long enough at some point.
 * 
 * All this is done in background to not disturb other processes.
 * 
 * The background thread does not alter any data of the map (read only thread).
 * Also it is rather fail safe. In case of some error in the data, it just do
 * nothing instead of whining and complaining.
 * 
 * @author imi
 */
public class MapStatus extends JPanel {

	/**
	 * The Layer this status belongs. 
	 */
	final Layer mv;
	/**
	 * The position of the mouse cursor.
	 */
	private JTextField positionText = new JTextField("-000.00000000000000 -000.00000000000000".length());
	/**
	 * The field holding the name of the object under the mouse.
	 */
	private JTextField nameText = new JTextField(30);
	/**
	 * The background thread thats collecting the data.
	 */
	private Runnable collector;

	/**
	 * The collector class that waits for notification and then update
	 * the display objects.
	 * 
	 * @author imi
	 */
	private final class Collector implements Runnable {
		/**
		 * The last object displayed in status line.
		 */
		OsmPrimitive osmStatus;
		/**
		 * A visitor to retrieve name information about the osm primitive
		 */
		private SelectionComponentVisitor visitor = new SelectionComponentVisitor();
		/**
		 * The old modifiers, that was pressed the last time this collector ran.
		 */
		private int oldModifiers;
		/**
		 * The popup displayed to show additional information
		 */
		private Popup popup;

		/**
		 * Execution function for the Collector.
		 */
		public void run() {
			for (;;) {
				MouseState ms = new MouseState();
				synchronized (this) {
					try {wait();} catch (InterruptedException e) {}
					ms.modifiers = mouseState.modifiers;
					ms.mousePos = mouseState.mousePos;
				}
				if ((ms.modifiers & MouseEvent.CTRL_DOWN_MASK) != 0 || ms.mousePos == null)
					continue; // freeze display when holding down ctrl
				OsmPrimitive osm = mv.getNearest(ms.mousePos, (ms.modifiers & MouseEvent.ALT_DOWN_MASK) != 0);
				if (osm == osmStatus && ms.modifiers == oldModifiers)
					continue;
				osmStatus = osm;
				oldModifiers = ms.modifiers;
				if (osm != null) {
					osm.visit(visitor);
					nameText.setText(visitor.name);
				} else
					nameText.setText("");
				
				// Popup Information
				if ((ms.modifiers & MouseEvent.BUTTON2_DOWN_MASK) != 0 && osm != null) {
					if (popup != null)
						popup.hide();
					
					StringBuilder text = new StringBuilder("<html>");
					text.append(visitor.name);
					if (osm.keys != null) {
						for (Entry<Key, String> e : osm.keys.entrySet()) {
							text.append("<br>");
							text.append(e.getKey().name);
							text.append("=");
							text.append(e.getValue());
						}
					}
					JLabel l = new JLabel(text.toString(), visitor.icon, JLabel.HORIZONTAL);
					
					Point p = mv.getLocationOnScreen();
					popup = PopupFactory.getSharedInstance().getPopup(mv, l, p.x+ms.mousePos.x+16, p.y+ms.mousePos.y+16);
					popup.show();
				} else if (popup != null) {
					popup.hide();
					popup = null;
				}
			}
		}
	}

	/**
	 * Everything, the collector is interested of. Access must be synchronized.
	 * @author imi
	 */
	class MouseState {
		Point mousePos;
		int modifiers;
	}
	/**
	 * The last sent mouse movement event.
	 */
	private MouseState mouseState = new MouseState();
	
	/**
	 * Construct a new MapStatus and attach it to the Layer.
	 * @param mv The Layer the status line is part of.
	 */
	public MapStatus(final Layer mv) {
		this.mv = mv;
		
		// Listen for mouse movements and set the position text field
		mv.addMouseMotionListener(new MouseMotionListener(){
			public void mouseDragged(MouseEvent e) {
				mouseMoved(e);
			}
			public void mouseMoved(MouseEvent e) {
				// Do not update the view, if ctrl is pressed.
				if ((e.getModifiersEx() & MouseEvent.CTRL_DOWN_MASK) == 0) {
					GeoPoint p = mv.getPoint(e.getX(),e.getY(),true);
					positionText.setText(p.lat+" "+p.lon);
				}
			}
		});
		
		// Listen to keyboard/mouse events for pressing/releasing alt key and
		// inform the collector.
		Toolkit.getDefaultToolkit().addAWTEventListener(new AWTEventListener(){
			public void eventDispatched(AWTEvent event) {
				synchronized (collector) {
					mouseState.modifiers = ((InputEvent)event).getModifiersEx();
					if (event instanceof MouseEvent)
						mouseState.mousePos = ((MouseEvent)event).getPoint();
					collector.notify();
				}
			}
		}, AWTEvent.KEY_EVENT_MASK | AWTEvent.MOUSE_EVENT_MASK | AWTEvent.MOUSE_MOTION_EVENT_MASK);

		positionText.setEditable(false);
		nameText.setEditable(false);
		setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
		setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
		add(new JLabel("Lat/Lon "));
		add(positionText);
		add(new JLabel(" Object "));
		add(nameText);

		// The background thread
		collector = new Collector();
		new Thread(collector).start();
	}
}
