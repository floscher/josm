package org.openstreetmap.josm.gui;

import java.awt.AWTEvent;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.GridBagLayout;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.AWTEventListener;
import java.awt.event.InputEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Arrays;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.Map.Entry;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.Popup;
import javax.swing.PopupFactory;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.visitor.SelectionComponentVisitor;
import org.openstreetmap.josm.tools.GBC;

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
	 * The MapView this status belongs. 
	 */
	final MapView mv;
	/**
	 * The position of the mouse cursor.
	 */
	JTextField positionText = new JTextField("-000.00000000000000 -000.00000000000000".length());
	/**
	 * The field holding the name of the object under the mouse.
	 */
	JTextField nameText = new JTextField(30);
	
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
		Collection<OsmPrimitive> osmStatus;
		/**
		 * The old modifiers, that was pressed the last time this collector ran.
		 */
		private int oldModifiers;
		/**
		 * The popup displayed to show additional information
		 */
		private Popup popup;
		/**
		 * Signals the collector to shut down on next event.
		 */
		boolean exitCollector = false;
		
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
				if (exitCollector)
					return;
				if ((ms.modifiers & MouseEvent.CTRL_DOWN_MASK) != 0 || ms.mousePos == null)
					continue; // freeze display when holding down ctrl

				// This try/catch is a hack to stop the flooding bug reports about this.
				// The exception needed to handle with in the first place, means that this
				// access to the data need to be restarted, if the main thread modifies 
				// the data.
				try {
					Collection<OsmPrimitive> osms = mv.getAllNearest(ms.mousePos);
					
					if (osms == null && osmStatus == null && ms.modifiers == oldModifiers)
						continue;
					if (osms != null && osms.equals(osmStatus) && ms.modifiers == oldModifiers)
						continue;
					
					osmStatus = osms;
					oldModifiers = ms.modifiers;
					
					OsmPrimitive osmNearest = null;
					// Set the text label in the bottom status bar
					osmNearest = mv.getNearest(ms.mousePos, (ms.modifiers & MouseEvent.ALT_DOWN_MASK) != 0);
					if (osmNearest != null) {
						SelectionComponentVisitor visitor = new SelectionComponentVisitor();
						osmNearest.visit(visitor);
						nameText.setText(visitor.name);
					} else
						nameText.setText("");
					
					// Popup Information
					if ((ms.modifiers & MouseEvent.BUTTON2_DOWN_MASK) != 0 && osms != null) {
						if (popup != null)
							popup.hide();
						
						JPanel c = new JPanel(new GridBagLayout());
						for (final OsmPrimitive osm : osms) {
							SelectionComponentVisitor visitor = new SelectionComponentVisitor();
							osm.visit(visitor);
							final StringBuilder text = new StringBuilder();
							if (osm.id == 0 || osm.modified)
								visitor.name = "<i><b>"+visitor.name+"*</b></i>";
							text.append(visitor.name);
							if (osm.id != 0)
								text.append("<br>id="+osm.id);
							for (Entry<String, String> e : osm.entrySet())
								text.append("<br>"+e.getKey()+"="+e.getValue());
							final JLabel l = new JLabel("<html>"+text.toString()+"</html>", visitor.icon, JLabel.HORIZONTAL);
							l.setFont(l.getFont().deriveFont(Font.PLAIN));
							l.setVerticalTextPosition(JLabel.TOP);
							l.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
							l.addMouseListener(new MouseAdapter(){
								@Override public void mouseEntered(MouseEvent e) {
									l.setText("<html><u color='blue'>"+text.toString()+"</u></html>");
								}
								@Override public void mouseExited(MouseEvent e) {
									l.setText("<html>"+text.toString()+"</html>");
								}
								@Override public void mouseClicked(MouseEvent e) {
									Main.ds.setSelected(Arrays.asList(new OsmPrimitive[]{osm}));
									mv.repaint();
								}
							});
							c.add(l, GBC.eol());
						}
						
						Point p = mv.getLocationOnScreen();
						popup = PopupFactory.getSharedInstance().getPopup(mv, c, p.x+ms.mousePos.x+16, p.y+ms.mousePos.y+16);
						popup.show();
					} else if (popup != null) {
						popup.hide();
						popup = null;
					}
				} catch (ConcurrentModificationException x) {
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
	MouseState mouseState = new MouseState();
	
	/**
	 * Construct a new MapStatus and attach it to the map view.
	 * @param mv The MapView the status line is part of.
	 */
	public MapStatus(final MapFrame mapFrame) {
		this.mv = mapFrame.mapView;
		
		// Listen for mouse movements and set the position text field
		mv.addMouseMotionListener(new MouseMotionListener(){
			public void mouseDragged(MouseEvent e) {
				mouseMoved(e);
			}
			public void mouseMoved(MouseEvent e) {
				// Do not update the view, if ctrl is pressed.
				if ((e.getModifiersEx() & MouseEvent.CTRL_DOWN_MASK) == 0) {
					LatLon p = mv.getLatLon(e.getX(),e.getY());
					positionText.setText(p.lat()+" "+p.lon());
				}
			}
		});
		
		positionText.setEditable(false);
		nameText.setEditable(false);
		setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
		setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
		add(new JLabel("Lat/Lon "));
		add(positionText);
		add(new JLabel(" Object "));
		add(nameText);
		
		// The background thread
		final Collector collector = new Collector();
		new Thread(collector).start();
		
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
		
		// listen for shutdowns to cancel the background thread
		mapFrame.addPropertyChangeListener("visible", new PropertyChangeListener(){
			public void propertyChange(PropertyChangeEvent evt) {
				if (evt.getNewValue() == Boolean.FALSE) {
					collector.exitCollector = true;
					synchronized (collector) {
						collector.notify();
					}
				}
			}
		});
	}
}
