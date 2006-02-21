package org.openstreetmap.josm.gui.layer;

import java.awt.Graphics;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;
import java.util.Stack;

import javax.swing.Icon;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.LineSegment;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Track;
import org.openstreetmap.josm.data.osm.visitor.BoundingVisitor;
import org.openstreetmap.josm.data.osm.visitor.MergeVisitor;
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

	public interface ModifiedChangedListener {
		void modifiedChanged(boolean value, OsmDataLayer source);
	}
	
	private static Icon icon;

	/**
	 * The data behind this layer.
	 */
	public final DataSet data;

	/**
	 * Whether the data of this layer was modified during the session.
	 */
	private boolean modified = false;
	/**
	 * Whether the data was modified due an upload of the data to the server.
	 */
	public boolean uploadedModified = false;
	/**
	 * Whether the data (or pieces of the data) was loaded from disk rather than from
	 * the server directly. This affects the modified state.
	 */
	private boolean fromDisk = false;
	/**
	 * All commands that were made on the dataset.
	 */
	private LinkedList<Command> commands = new LinkedList<Command>();
	/**
	 * The stack for redoing commands
	 */
	private Stack<Command> redoCommands = new Stack<Command>();

	/**
	 * List of all listeners for changes of modified flag.
	 */
	LinkedList<ModifiedChangedListener> listener;

	
	/**
	 * Construct a OsmDataLayer.
	 */
	public OsmDataLayer(DataSet data, String name, boolean fromDisk) {
		super(name);
		this.data = data;
		this.fromDisk = fromDisk;
		Main.pref.addPropertyChangeListener(new PropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent evt) {
				if (evt.getPropertyName().equals("projection"))
					for (Node n : OsmDataLayer.this.data.nodes)
						((Projection)evt.getNewValue()).latlon2xy(n.coor);
			}
		});
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
		SimplePaintVisitor visitor = new SimplePaintVisitor(g, mv);
		for (OsmPrimitive osm : data.lineSegments)
			if (!osm.isDeleted())
				osm.visit(visitor);
		for (OsmPrimitive osm : data.tracks)
			if (!osm.isDeleted())
				osm.visit(visitor);
		for (OsmPrimitive osm : data.nodes)
			if (!osm.isDeleted())
				osm.visit(visitor);
		for (OsmPrimitive osm : data.getSelected())
			if (!osm.isDeleted())
				osm.visit(visitor);
	}

	@Override
	public String getToolTipText() {
		return undeletedSize(data.nodes)+" nodes, "+
			undeletedSize(data.lineSegments)+" segments, "+
			undeletedSize(data.tracks)+" streets.";
	}

	@Override
	public void mergeFrom(Layer from) {
		MergeVisitor visitor = new MergeVisitor(data);
		for (OsmPrimitive osm : ((OsmDataLayer)from).data.allPrimitives())
			osm.visit(visitor);
		visitor.fixReferences();
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
		return b.bounds != null ? b.bounds : new Bounds();
	}

	@Override
	public Bounds getBoundsXY() {
		BoundingVisitor b = new BoundingVisitor(BoundingVisitor.Type.XY);
		for (Node n : data.nodes)
			b.visit(n);
		return b.bounds != null ? b.bounds : new Bounds();
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
		setModified(true);
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
		if (commands.isEmpty())
			setModified(uploadedModified);
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
		setModified(true);
	}

	/**
	 * Clean out the data behind the layer. This means clearing the redo/undo lists,
	 * really deleting all deleted objects and reset the modified flags. This is done
	 * after a successfull upload.
	 * 
	 * @param uploaded <code>true</code>, if the data was uploaded, false if saved to disk
	 * @param processed A list of all objects, that were actually uploaded. 
	 * 		May be <code>null</code>, which means nothing has been uploaded but 
	 * 		saved to disk instead.
	 */
	public void cleanData(Collection<OsmPrimitive> processed, boolean dataAdded) {
		redoCommands.clear();
		commands.clear();

		// if uploaded, clean the modified flags as well
		if (processed != null) {
			Set<OsmPrimitive> processedSet = new HashSet<OsmPrimitive>(processed);
			for (Iterator<Node> it = data.nodes.iterator(); it.hasNext();)
				cleanIterator(it, processedSet);
			for (Iterator<LineSegment> it = data.lineSegments.iterator(); it.hasNext();)
				cleanIterator(it, processedSet);
			for (Iterator<Track> it = data.tracks.iterator(); it.hasNext();)
				cleanIterator(it, processedSet);
		}

		// update the modified flag
		
		if (fromDisk && processed != null && !dataAdded)
			return; // do nothing when uploading non-harmful changes.
		
		// modified if server changed the data (esp. the id).
		uploadedModified = fromDisk && processed != null && dataAdded;
		setModified(uploadedModified);
		//TODO: Replace with listener scheme
		Main.main.undoAction.setEnabled(false);
		Main.main.redoAction.setEnabled(false);
	}

	/**
	 * Clean the modified flag for the given iterator over a collection if it is in the
	 * list of processed entries.
	 * 
	 * @param it The iterator to change the modified and remove the items if deleted.
	 * @param processed A list of all objects that have been successfully progressed.
	 * 		If the object in the iterator is not in the list, nothing will be changed on it.
	 */
	private void cleanIterator(Iterator<? extends OsmPrimitive> it, Collection<OsmPrimitive> processed) {
		OsmPrimitive osm = it.next();
		if (!processed.remove(osm))
			return;
		osm.modified = false;
		osm.modifiedProperties = false;
		if (osm.isDeleted())
			it.remove();
	}

	public boolean isModified() {
		return modified;
	}

	public void setModified(boolean modified) {
		if (modified == this.modified)
			return;
		this.modified = modified;
		if (listener != null)
			for (ModifiedChangedListener l : listener)
				l.modifiedChanged(modified, this);
	}

	/**
	 * Add the parameter to the intern list of listener for modified state.
	 * @param l Listener to add to the list. Must not be null.
	 */
	public void addModifiedListener(ModifiedChangedListener l) {
		if (listener == null)
			listener = new LinkedList<ModifiedChangedListener>();
		listener.add(l);
	}

	/**
	 * @return The number of not-deleted primitives in the list.
	 */
	private int undeletedSize(Collection<? extends OsmPrimitive> list) {
		int size = 0;
		for (OsmPrimitive osm : list)
			if (!osm.isDeleted())
				size++;
		return size;
	}
}
