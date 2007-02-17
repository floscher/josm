package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.tools.I18n.tr;
import static org.xnap.commons.i18n.I18n.marktr;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.Collection;

import javax.swing.AbstractAction;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.SelectionChangedListener;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.visitor.BoundingXYVisitor;
import org.openstreetmap.josm.gui.MapFrame;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.tools.ImageProvider;

/**
 * Toggles the autoScale feature of the mapView
 * @author imi
 */
public class AutoScaleAction extends GroupAction {

	private static final String[] modes = {
		marktr("data"),
		marktr("selection"),
		marktr("layer"),
		marktr("conflict")
	};
	private String mode = "data";
	private final MapFrame mapFrame;

	private class Action extends AbstractAction {
		private final String mode;
		public Action(String mode) {
			super(tr("Auto Scale: {0}", tr(mode)), ImageProvider.get("dialogs/autoscale/"+mode));
			String modeHelp = Character.toUpperCase(mode.charAt(0))+mode.substring(1);
			putValue("help", "Action/AutoScale/"+modeHelp);
			putValue(SHORT_DESCRIPTION, tr("Auto zoom the view (to {0}. Disabled if the view is moved)", tr(mode)));
			this.mode = mode;
		}
		public void actionPerformed(ActionEvent e) {
			AutoScaleAction.this.mode = mode;
			if (mapFrame.mapView.isAutoScale())
            	mapFrame.mapView.recalculateCenterScale();
            else
            	mapFrame.mapView.setAutoScale(true);
			putValue("active", true);
		}
	}

	public AutoScaleAction(final MapFrame mapFrame) {
		super(KeyEvent.VK_A, 0);
		for (String mode : modes)
			actions.add(new Action(mode));
		setCurrent(0);
		this.mapFrame = mapFrame;
		Main.ds.listeners.add(new SelectionChangedListener(){
			public void selectionChanged(Collection<? extends OsmPrimitive> newSelection) {
				if (mode.equals("selection"))
					mapFrame.mapView.recalculateCenterScale();
			}
		});
	}

	public BoundingXYVisitor getBoundingBox() {
		BoundingXYVisitor v = new BoundingXYVisitor();
		if (mode.equals("data")) {
			for (Layer l : mapFrame.mapView.getAllLayers())
				l.visitBoundingBox(v);
		} else if (mode.equals("layer"))
			mapFrame.mapView.getActiveLayer().visitBoundingBox(v);
		else if (mode.equals("selection") || mode.equals("conflict")) {
			Collection<OsmPrimitive> sel = mode.equals("selection") ? Main.ds.getSelected() : mapFrame.conflictDialog.conflicts.keySet();
			for (OsmPrimitive osm : sel)
				osm.visit(v);
			// special case to zoom nicely to one single node
			if (v.min != null && v.max != null && v.min.north() == v.max.north() && v.min.east() == v.max.east()) {
				EastNorth en = Main.proj.latlon2eastNorth(new LatLon(0.02, 0.02));
				v.min = new EastNorth(v.min.east()-en.east(), v.min.north()-en.north());
				v.max = new EastNorth(v.max.east()+en.east(), v.max.north()+en.north());
			}
		}
		return v;
	}
}
