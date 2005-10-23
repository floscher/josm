package org.openstreetmap.josm.io;

import java.io.IOException;
import java.util.Collection;
import java.util.LinkedList;

import javax.swing.JOptionPane;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.data.osm.OsmPrimitive;


/**
 * Send back the modified data to the osm server.
 * @author imi
 */
public class OsmWriter extends OsmConnection {

	/**
	 * The server base url to handle the osm requests.
	 */
	private final String server;
	/**
	 * The commands that should be uploaded on the server.
	 */
	private final Collection<Command> commands;

	public OsmWriter(String server, Collection<Command> commands) {
		this.server = server;
		this.commands = commands;
	}
	
	/**
	 * Upload the commands to the server.
	 * @throws IOException
	 */
	public void output() throws IOException {
		Collection<OsmPrimitive> added = new LinkedList<OsmPrimitive>();
		Collection<OsmPrimitive> modified = new LinkedList<OsmPrimitive>();
		Collection<OsmPrimitive> deleted = new LinkedList<OsmPrimitive>();
		for (Command c : commands)
			c.fillModifiedData(modified, deleted, added);
		int answer = JOptionPane.showConfirmDialog(Main.main, "Send "+added.size()+" new, "
				+modified.size()+" modified and "+deleted.size()+" deleted objects to server?");
		if (answer != JOptionPane.YES_OPTION)
			return;
	}
}
