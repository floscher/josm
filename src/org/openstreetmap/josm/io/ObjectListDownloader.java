package org.openstreetmap.josm.io;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.visitor.MergeVisitor;
import org.openstreetmap.josm.data.osm.visitor.NameVisitor;
import org.xml.sax.SAXException;

public class ObjectListDownloader extends OsmServerReader {

	/**
	 * All the objects to download (if not downloading bounding boxes instead)
	 */
	private final Collection<OsmPrimitive> toDownload;
	private final DataSet ds = new DataSet();
	private final MergeVisitor merger = new MergeVisitor(ds);

	public ObjectListDownloader(Collection<OsmPrimitive> toDownload) {
		this.toDownload = toDownload;
	}

	public DataSet parse() throws SAXException, IOException {
		Main.pleaseWaitDlg.progress.setMaximum(toDownload.size());
		Main.pleaseWaitDlg.progress.setValue(0);
		try {
			final NameVisitor namer = new NameVisitor();
			for (OsmPrimitive osm : toDownload) {
				osm.visit(namer);
				download(tr(namer.className), osm.id);
				if (cancel)
					break;
			}
			if (!merger.conflicts.isEmpty())
				throw new RuntimeException(tr("Conflicts in disjunct objects"));
			return ds;
		} catch (IOException e) {
			if (cancel)
				return null;
			throw e;
		} catch (SAXException e) {
			throw e;
		} catch (Exception e) {
			if (cancel)
				return null;
			if (e instanceof RuntimeException)
				throw (RuntimeException)e;
			throw new RuntimeException(e);
		}
	}

	private void download(String className, long id) throws IOException, SAXException {
		Main.pleaseWaitDlg.currentAction.setText(tr("Downloading {0} {1}", className, id));
		InputStream in = getInputStream(className+"/"+id);
		if (in == null)
			return;
		DataSet data = OsmReader.parseDataSet(in, null, null);
		Main.pleaseWaitDlg.progress.setValue(Main.pleaseWaitDlg.progress.getValue()+1);
		if (data.allPrimitives().size() > 1)
			throw new SAXException(tr("Got more than one object when expecting only one."));
		for (OsmPrimitive osm : data.allPrimitives())
			osm.visit(merger);
	}
}
