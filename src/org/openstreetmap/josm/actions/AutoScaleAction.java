package org.openstreetmap.josm.actions;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.Collection;

import javax.swing.KeyStroke;
import javax.swing.MenuElement;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.SelectionChangedListener;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.visitor.BoundingXYVisitor;
import org.openstreetmap.josm.gui.MapFrame;
import org.openstreetmap.josm.gui.layer.Layer;

/**
 * Toggles the autoScale feature of the mapView
 * @author imi
 */
public class AutoScaleAction extends GroupAction {

	private enum AutoScaleMode {data, selection, layer, conflict}
	private AutoScaleMode mode = AutoScaleMode.data;
	private final MapFrame mapFrame;

	private class Action extends JosmAction {
		private final AutoScaleMode mode;
		public Action(AutoScaleMode mode) {
	        super("Auto Scale: "+mode, "dialogs/autoscale/"+mode, "Auto zoom the view to "+mode+". Disabled if the view is moved.", "Alt-A", KeyStroke.getKeyStroke(KeyEvent.VK_A, KeyEvent.ALT_MASK));
			this.mode = mode;
        }
		public void actionPerformed(ActionEvent e) {
			AutoScaleAction.this.mode = mode;
			if (e.getSource() instanceof MenuElement)
				setAutoScaleOnMapView();
			else
				mapFrame.mapView.setAutoScale(!mapFrame.mapView.isAutoScale());
        }
	}
	
	public AutoScaleAction(final MapFrame mapFrame) {
		for (AutoScaleMode mode : AutoScaleMode.values())
			actions.add(new Action(mode));
		setCurrent(0);
		this.mapFrame = mapFrame;
		Main.ds.addSelectionChangedListener(new SelectionChangedListener(){
			public void selectionChanged(Collection<OsmPrimitive> newSelection) {
				if (mode == AutoScaleMode.selection)
					mapFrame.mapView.recalculateCenterScale();
            }
		});
	}

	public BoundingXYVisitor getBoundingBox() {
		BoundingXYVisitor v = new BoundingXYVisitor();
		switch (mode) {
		case data:
			for (Layer l : mapFrame.mapView.getAllLayers())
				l.visitBoundingBox(v);
			break;
		case layer:
			mapFrame.mapView.getActiveLayer().visitBoundingBox(v);
			break;
		case selection:
		case conflict:
			Collection<OsmPrimitive> sel = mode == AutoScaleMode.selection ? Main.ds.getSelected() : mapFrame.conflictDialog.conflicts.keySet();
			for (OsmPrimitive osm : sel)
				osm.visit(v);
			// special case to zoom nicely to one single node
			if (v.min != null && v.max != null && v.min.north() == v.max.north() && v.min.east() == v.max.east()) {
				EastNorth en = Main.proj.latlon2eastNorth(new LatLon(0.02, 0.02));
				v.min = new EastNorth(v.min.east()-en.east(), v.min.north()-en.north());
				v.max = new EastNorth(v.max.east()+en.east(), v.max.north()+en.north());
			}
			break;
		}
		return v;
    }

	private void setAutoScaleOnMapView() {
    	if (mapFrame.mapView.isAutoScale())
    		mapFrame.mapView.recalculateCenterScale();
    	else
    		mapFrame.mapView.setAutoScale(true);
    }
}
