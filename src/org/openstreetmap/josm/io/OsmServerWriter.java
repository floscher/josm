package org.openstreetmap.josm.io;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collection;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.osm.Key;
import org.openstreetmap.josm.data.osm.LineSegment;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Track;
import org.openstreetmap.josm.data.osm.visitor.OsmXmlVisitor;
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
	 * Send the dataset to the server. Ask the user first and does nothing if he
	 * does not want to send the data.
	 */
	public void uploadOsm(Collection<OsmPrimitive> list) throws JDOMException {
		initAuthentication();

		try {
			for (OsmPrimitive osm : list)
				osm.visit(this);
		} catch (RuntimeException e) {
			throw new JDOMException("An error occoured: ", e);
		}
	}

	/**
	 * Upload a single node.
	 */
	@SuppressWarnings("unchecked")
	public void visit(Node n) {
		if (n.id == 0 && !n.isDeleted()) {
			sendRequest("PUT", "newnode", n, true);
		} else if (n.isDeleted()) {
			sendRequest("DELETE", "node/" + n.id, n, false);
		} else {
			sendRequest("PUT", "node/" + n.id, n, true);
		}
	}

	public void visit(LineSegment ls) {
		if (ls.id == 0 && !ls.isDeleted()) {
			sendRequest("PUT", "newsegment", ls, true);
		} else if (ls.isDeleted()) {
			sendRequest("DELETE", "segment/" + ls.id, ls, false);
		} else {
			sendRequest("PUT", "segment/" + ls.id, ls, true);
		}
	}

	public void visit(Track t) {
		// not implemented in server
	}

	public void visit(Key k) {
		// not implemented in server
	}

	/**
	 * Read an long from the input stream and return it.
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

	@SuppressWarnings("unchecked")
	private void sendRequest(String requestMethod, String urlSuffix,
			OsmPrimitive osm, boolean addBody) {
		try {
			URL url = new URL(Main.pref.osmDataServer + "/" + urlSuffix);
			HttpURLConnection con = (HttpURLConnection) url.openConnection();
			con.setConnectTimeout(20000);
			con.setRequestMethod(requestMethod);
			if (addBody)
				con.setDoOutput(true);
			con.connect();

			if (addBody) {
				OsmXmlVisitor visitor = new OsmXmlVisitor(false);
				osm.visit(visitor);
				Element root = new Element("osm");
				root.setAttribute("version", "0.2");
				root.getChildren().add(visitor.element);
				XMLOutputter xmlOut = new XMLOutputter(Format.getPrettyFormat());
				OutputStream out = con.getOutputStream();
				Document doc = new Document(root);
				xmlOut.output(doc, out);
				out.close();
			}

			int retCode = con.getResponseCode();
			if (retCode == 200 && osm.id == 0)
				osm.id = readId(con.getInputStream());
			con.disconnect();
		} catch (Exception e) {
			throw new RuntimeException(e.getMessage(), e);
		}
	}
}
