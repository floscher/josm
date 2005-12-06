package org.openstreetmap.josm.gui.layer;

import java.awt.Graphics;
import java.util.LinkedList;
import java.util.Stack;

import javax.swing.Icon;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.LineSegment;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.Track;
import org.openstreetmap.josm.data.osm.visitor.BoundingVisitor;
import org.openstreetmap.josm.data.osm.visitor.SimplePaintVisitor;
import org.openstreetmap.josm.data.projection.Projection;
import org.openstreetmap.josm.gui.ImageProvider;
import org.openstreetmap.josm.gui.MapView;

/**
 * A layer holding data from a specific dataset.
 * The data can be fully edited.
 * 
 * @author imi
 */
public class OsmDataLayer extends Layer {

	private static Icon icon;

	/**
	 * The data behind this layer.
	 */
	public final DataSet data;

	/**
	 * All commands that were made on the dataset.
	 */
	private LinkedList<Command> commands = new LinkedList<Command>();
	/**
	 * The stack for redoing commands
	 */
	private Stack<Command> redoCommands = new Stack<Command>();

	/**
	 * Construct a OsmDataLayer.
	 */
	public OsmDataLayer(DataSet data, String name) {
		super(name);
		this.data = data;
	}

	/**
	 * TODO: @return Return a dynamic drawn icon of the map data. The icon is
	 * 		updated by a background thread to not disturb the running programm.
	 */
	@Override
	public Icon getIcon() {
		if (icon == null)
			icon = ImageProvider.get("layer", "osmdata");
		return icon;
	}

	/**
	 * Draw all primitives in this layer but do not draw modified ones (they
	 * are drawn by the edit layer).
	 * Draw nodes last to overlap the line segments they belong to.
	 */
	@Override
	public void paint(Graphics g, MapView mv) {
		SimplePaintVisitor visitor = new SimplePaintVisitor(g, mv, null);

		for (LineSegment ls : data.lineSegments)
			visitor.visit(ls);
		for (Track t : data.tracks)
			visitor.visit(t);
		for (Node n : data.nodes)
			visitor.visit(n);
	}

	@Override
	public String getToolTipText() {
		return data.nodes.size()+" nodes, "+data.tracks.size()+" streets.";
	}

	@Override
	public void mergeFrom(Layer from) {
		data.mergeFrom(((OsmDataLayer)from).data);
	}

	@Override
	public boolean isMergable(Layer other) {
		return other instanceof OsmDataLayer;
	}

	@Override
	public Bounds getBoundsLatLon() {
		BoundingVisitor b = new BoundingVisitor(BoundingVisitor.Type.LATLON);
		for (Node n : data.nodes)
			b.visit(n);
		return b.bounds;
	}

	@Override
	public Bounds getBoundsXY() {
		BoundingVisitor b = new BoundingVisitor(BoundingVisitor.Type.XY);
		for (Node n : data.nodes)
			b.visit(n);
		return b.bounds;
	}

	@Override
	public void init(Projection projection) {
		for (Node n : data.nodes)
			projection.latlon2xy(n.coor);
	}

	/**
	 * @return the last command added or <code>null</code> if no command in queue.
	 */
	public Command lastCommand() {
		return commands.isEmpty() ? null : commands.getLast();
	}
	
	/**
	 * Execute the command and add it to the intern command queue. Also mark all
	 * primitives in the command as modified.
	 */
	public void add(Command c) {
		c.executeCommand();
		commands.add(c);
		redoCommands.clear();
		// TODO: Replace with listener scheme
		Main.main.undoAction.setEnabled(true);
		Main.main.redoAction.setEnabled(false);
	}

	/**
	 * Undoes the last added command.
	 */
	public void undo() {
		if (commands.isEmpty())
			return;
		Command c = commands.removeLast();
		c.undoCommand();
		redoCommands.push(c);
		//TODO: Replace with listener scheme
		Main.main.undoAction.setEnabled(!commands.isEmpty());
		Main.main.redoAction.setEnabled(true);
	}
	/**
	 * Redoes the last undoed command.
	 */
	public void redo() {
		if (redoCommands.isEmpty())
			return;
		Command c = redoCommands.pop();
		c.executeCommand();
		commands.add(c);
		//TODO: Replace with listener scheme
		Main.main.undoAction.setEnabled(true);
		Main.main.redoAction.setEnabled(!redoCommands.isEmpty());
	}
}
