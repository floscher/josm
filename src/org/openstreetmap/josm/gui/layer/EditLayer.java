package org.openstreetmap.josm.gui.layer;

import java.awt.Color;
import java.awt.Graphics;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;

import javax.swing.Icon;
import javax.swing.SwingUtilities;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.visitor.AllNodesVisitor;
import org.openstreetmap.josm.data.osm.visitor.BoundingVisitor;
import org.openstreetmap.josm.data.osm.visitor.SimplePaintVisitor;
import org.openstreetmap.josm.data.projection.Projection;
import org.openstreetmap.josm.gui.ImageProvider;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.gui.MapView.LayerChangeListener;

/**
 * The layer that contain all editing commands in the current session. All commands
 * are saved here. The command actions "move" data from the data layer to this 
 * layer (by just removing them from the data layer) for all edited data, so 
 * invisible this layer will actually invisible all changes made to the data set.
 * 
 * There may only be one EditLayer per MapView.
 * 
 * Only EditLayers can be uploaded to the server.
 */
public class EditLayer extends Layer implements LayerChangeListener, PropertyChangeListener {

	private static Icon icon;

	/**
	 * All commands that were made on the dataset.
	 */
	public Collection<Command> commands = new LinkedList<Command>();

	/**
	 * The map view this layer belongs to.
	 */
	private final MapView mv;

	/**
	 * Create an initial empty edit layer.
	 * @param mv The mapview that emits layer change events.
	 */
	public EditLayer(final MapView mv) {
		super("not uploaded changes");
		this.mv = mv;
		mv.addLayerChangeListener(this);
		Main.pref.addPropertyChangeListener(this);
	}


	/**
	 * Paint all changes in the changes list.
	 * @param g
	 * @param mv
	 */
	@Override
	public void paint(Graphics g, MapView mv) {
		LinkedList<OsmPrimitive> changes = new LinkedList<OsmPrimitive>();
		LinkedList<OsmPrimitive> deleted = new LinkedList<OsmPrimitive>();
		for (Command c : commands)
			c.fillModifiedData(changes, deleted, changes);

		SimplePaintVisitor visitor = new SimplePaintVisitor(g, mv, null);
		SimplePaintVisitor deletedvisitor = new SimplePaintVisitor(g, mv, Color.DARK_GRAY);

		changes.removeAll(deleted);

		// first draw deleted objects in dark gray
		if (Main.pref.isDrawDeleted())
			for (OsmPrimitive osm : deleted)
				osm.visit(deletedvisitor);
		// next draw the tracks (and line segments)
		for (OsmPrimitive osm : changes)
			if (!(osm instanceof Node))
				osm.visit(visitor);
		// now draw the remaining objects
		for (OsmPrimitive osm : changes)
			if (osm instanceof Node)
				osm.visit(visitor);
		
	}

	@Override
	public Icon getIcon() {
		if (icon == null)
			icon = ImageProvider.get("layer", "edit");
		return icon;
	}

	@Override
	public String getToolTipText() {
		return commands.size()+" changes";
	}

	@Override
	public void mergeFrom(Layer from) {
		EditLayer edit = (EditLayer)from;
		commands.addAll(edit.commands);
	}

	@Override
	public boolean isMergable(Layer other) {
		return other instanceof EditLayer;
	}

	@Override
	public Bounds getBoundsLatLon() {
		LinkedList<OsmPrimitive> data = new LinkedList<OsmPrimitive>();
		for (Command c : commands)
			c.fillModifiedData(data, null, data); // all in one list
		BoundingVisitor b = new BoundingVisitor(BoundingVisitor.Type.LATLON);
		for (OsmPrimitive osm : data)
			osm.visit(b);
		return b.bounds;
	}

	@Override
	public Bounds getBoundsXY() {
		LinkedList<OsmPrimitive> data = new LinkedList<OsmPrimitive>();
		for (Command c : commands)
			c.fillModifiedData(data, null, data); // all in one list
		BoundingVisitor b = new BoundingVisitor(BoundingVisitor.Type.XY);
		for (OsmPrimitive osm : data)
			osm.visit(b);
		return b.bounds;
	}

	@Override
	public void init(Projection projection) {
		LinkedList<OsmPrimitive> data = new LinkedList<OsmPrimitive>();
		for (Command c : commands)
			c.fillModifiedData(data, data, data); // all in one list
		for (OsmPrimitive osm : data)
			for (Node n : AllNodesVisitor.getAllNodes(osm))
				projection.latlon2xy(n.coor);
	}
	
	/**
	 * Execute the command and add it to the intern command queue. Also mark all
	 * primitives in the command as modified.
	 */
	public void add(Command c) {
		c.executeCommand();
		Collection<OsmPrimitive> data = new HashSet<OsmPrimitive>();
		c.fillModifiedData(data, data, data);
		for (OsmPrimitive osm : data)
			osm.modified = true;
		commands.add(c);
	}

	/**
	 * Clean up the data and remove itself from the map view after
	 * the notifications ended.
	 */
	public void layerRemoved(Layer oldLayer) {
		if (oldLayer == EditLayer.this) {
			Collection<OsmPrimitive> data = new HashSet<OsmPrimitive>();
			for (Command c : commands)
				c.fillModifiedData(data, data, data);
			for (OsmPrimitive osm : data)
				osm.modified = false;
			commands.clear();
			SwingUtilities.invokeLater(new Runnable(){
				public void run() {
					mv.removeLayerChangeListener(EditLayer.this);
				}
			});
		}
	}
	
	/**
	 * Does nothing. Only to satisfy LayerChangeListener
	 */
	public void activeLayerChange(Layer oldLayer, Layer newLayer) {}
	/**
	 * Does nothing. Only to satisfy LayerChangeListener
	 */
	public void layerAdded(Layer newLayer) {}


	public void propertyChange(PropertyChangeEvent evt) {
		if (evt.getPropertyName().equals("drawDeleted"))
			mv.repaint();
	}
}
