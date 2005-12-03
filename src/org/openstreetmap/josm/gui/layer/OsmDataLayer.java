package org.openstreetmap.josm.gui.layer;

import java.awt.Graphics;
import java.io.FileWriter;
import java.io.StringWriter;
import java.util.LinkedList;

import javax.swing.Icon;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.LineSegment;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.Track;
import org.openstreetmap.josm.data.osm.visitor.BoundingVisitor;
import org.openstreetmap.josm.data.osm.visitor.CsvVisitor;
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
	private LinkedList<String> debugDsBefore = new LinkedList<String>();

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

		for (Track t : data.tracks)
			visitor.visit(t);
		for (LineSegment ls : data.lineSegments)
			visitor.visit(ls);
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
	 * Execute the command and add it to the intern command queue. Also mark all
	 * primitives in the command as modified.
	 */
	public void add(Command c) {
		StringWriter sw = new StringWriter();
		CsvVisitor v = new CsvVisitor(sw);
		for (Node n : Main.main.ds.nodes) {
			v.visit(n);
			sw.append('\n');
		}
		for (LineSegment ls : Main.main.ds.lineSegments) {
			v.visit(ls);
			sw.append('\n');
		}
		for (Track t : Main.main.ds.tracks) {
			v.visit(t);
			sw.append('\n');
		}
		debugDsBefore.add(sw.getBuffer().toString());
		
		c.executeCommand();
		commands.add(c);
	}

	/**
	 * Undoes the last added command.
	 */
	public void undo() {
		if (commands.isEmpty())
			return;
		Command c = commands.removeLast();
		c.undoCommand();
		
		//DEBUG
		StringWriter sw = new StringWriter();
		CsvVisitor v = new CsvVisitor(sw);
		for (Node n : Main.main.ds.nodes) {
			v.visit(n);
			sw.append('\n');
		}
		for (LineSegment ls : Main.main.ds.lineSegments) {
			v.visit(ls);
			sw.append('\n');
		}
		for (Track t : Main.main.ds.tracks) {
			v.visit(t);
			sw.append('\n');
		}
		String s = Main.main.getMapFrame().mapView.editLayer().debugDsBefore.removeLast();
		if (!s.equals(sw.getBuffer().toString())) {
			try {
				FileWriter fw = new FileWriter("/home/imi/richtig");
				fw.append(sw.getBuffer().toString());
				fw.close();
				fw = new FileWriter("/home/imi/falsch");
				fw.append(s);
				fw.close();
			} catch (Exception x) {
				x.printStackTrace();
			}
		}
	}
}
