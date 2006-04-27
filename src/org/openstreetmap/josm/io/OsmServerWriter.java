package org.openstreetmap.josm.io;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.LinkedList;

import org.jdom.JDOMException;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Segment;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.visitor.NameVisitor;
import org.openstreetmap.josm.data.osm.visitor.Visitor;

/**
 * Class that uploades all changes to the osm server.
 * 
 * This is done like this: - All objects with id = 0 are uploaded as new, except
 * those in deleted, which are ignored - All objects in deleted list are
 * deleted. - All remaining objects with modified flag set are updated.
 * 
 * This class implements visitor and will perform the correct upload action on
 * the visited element.
 * 
 * @author imi
 */
public class OsmServerWriter extends OsmConnection implements Visitor {

	/**
	 * This list contain all sucessfull processed objects. The caller of
	 * upload* has to check this after the call and update its dataset.
	 * 
	 * If a server connection error occours, this may contain fewer entries
	 * than where passed in the list to upload*.
	 */
	public Collection<OsmPrimitive> processed;

	/**
	 * Whether the operation should be aborted as soon as possible.
	 */
	private boolean cancel = false;

	/**
	 * Send the dataset to the server. Ask the user first and does nothing if he
	 * does not want to send the data.
	 */
	public void uploadOsm(Collection<OsmPrimitive> list) throws JDOMException {
		processed = new LinkedList<OsmPrimitive>();
		initAuthentication();

		progress.setMaximum(list.size());
		progress.setValue(0);

		NameVisitor v = new NameVisitor();
		try {
			for (OsmPrimitive osm : list) {
				if (cancel)
					return;
				osm.visit(v);
				currentAction.setText("Upload "+v.className+" "+v.name+" ("+osm.id+")...");
				osm.visit(this);
				progress.setValue(progress.getValue()+1);
			}
		} catch (RuntimeException e) {
			throw new JDOMException("An error occoured: ", e);
		}
	}

	/**
	 * Upload a single node.
	 */
	public void visit(Node n) {
		if (n.id == 0 && !n.deleted && n.get("created_by") == null) {
			n.put("created_by", "JOSM");
			sendRequest("PUT", "node", n, true);
		} else if (n.deleted) {
			sendRequest("DELETE", "node", n, false);
		} else {
			sendRequest("PUT", "node", n, true);
		}
		processed.add(n);
	}

	/**
	 * Upload a segment (without the nodes).
	 */
	public void visit(Segment ls) {
		if (ls.id == 0 && !ls.deleted && ls.get("created_by") == null) {
			ls.put("created_by", "JOSM");
			sendRequest("PUT", "segment", ls, true);
		} else if (ls.deleted) {
			sendRequest("DELETE", "segment", ls, false);
		} else {
			sendRequest("PUT", "segment", ls, true);
		}
		processed.add(ls);
	}

	/**
	 * Upload a whole way with the complete segment id list.
	 */
	public void visit(Way w) {
		if (w.id == 0 && !w.deleted && w.get("created_by") == null) {
			w.put("created_by", "JOSM");
			sendRequest("PUT", "way", w, true);
		} else if (w.deleted) {
			sendRequest("DELETE", "way", w, false);
		} else {
			sendRequest("PUT", "way", w, true);
		}
		processed.add(w);
	}

	/**
	 * Read a long from the input stream and return it.
	 */
	private long readId(InputStream inputStream) throws IOException {
		BufferedReader in = new BufferedReader(new InputStreamReader(
				inputStream));
		String s = in.readLine();
		if (s == null)
			return 0;
		try {
			return Long.parseLong(s);
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	/**
	 * Send the request. The objects id will be replaced if it was 0 before
	 * (on add requests).
	 * 
	 * @param requestMethod The http method used when talking with the server.
	 * @param urlSuffix The suffix to add at the server url.
	 * @param osm The primitive to encode to the server.
	 * @param addBody <code>true</code>, if the whole primitive body should be added. 
	 * 		<code>false</code>, if only the id is encoded.
	 */
	@SuppressWarnings("unchecked")
	private void sendRequest(String requestMethod, String urlSuffix,
			OsmPrimitive osm, boolean addBody) {
		try {
			URL url = new URL(Main.pref.get("osm-server.url") + "/0.3/" + urlSuffix + "/" + osm.id);
			System.out.println("upload to: "+url);
			activeConnection = (HttpURLConnection) url.openConnection();
			activeConnection.setConnectTimeout(15000);
			activeConnection.setRequestMethod(requestMethod);
			if (addBody)
				activeConnection.setDoOutput(true);
			activeConnection.connect();

			if (addBody) {
				Writer out = new OutputStreamWriter(activeConnection.getOutputStream());
				OsmWriter.outputSingle(out, osm, true);
				out.close();
			}

			int retCode = activeConnection.getResponseCode();
			if (retCode == 200 && osm.id == 0)
				osm.id = readId(activeConnection.getInputStream());
			System.out.println("got return: "+retCode+" with id "+osm.id);
			String retMsg = activeConnection.getResponseMessage();
			activeConnection.disconnect();
			if (retCode == 410 && requestMethod.equals("DELETE"))
				return; // everything fine.. was already deleted.
			if (retCode != 200) {
				StringWriter o = new StringWriter();
				OsmWriter.outputSingle(o, osm, true);
				System.out.println(o.getBuffer().toString());
				throw new RuntimeException(retCode+" "+retMsg);
			}
		} catch (UnknownHostException e) {
			throw new RuntimeException("Unknown host: "+e.getMessage(), e);
		} catch (Exception e) {
			if (cancel)
				return; // assume cancel
			if (e instanceof RuntimeException)
				throw (RuntimeException)e;
			throw new RuntimeException(e.getMessage(), e);
		}
	}
}
