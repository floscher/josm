package org.openstreetmap.josm.plugins.mappaint;

import java.io.File;
import java.io.FileReader;

import org.openstreetmap.josm.gui.MapFrame;
import org.openstreetmap.josm.gui.MapView.LayerChangeListener;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.plugins.Plugin;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

public class MapPaintPlugin extends Plugin implements LayerChangeListener {

	public static ElemStyles elemStyles = new ElemStyles();

	public MapPaintPlugin() {
		super("mappaint");
		
		String elemStylesFile = getPluginDir()+"elemstyles.xml";
		File f = new File(elemStylesFile);
		if (f.exists())
		{
			try
			{
				XMLReader xmlReader = XMLReaderFactory.createXMLReader();
				ElemStyleHandler handler = new ElemStyleHandler();
				xmlReader.setContentHandler(handler);
				xmlReader.setErrorHandler(handler);
				handler.setElemStyles(elemStyles);
				// temporary only!
				xmlReader.parse(new InputSource(new FileReader(f)));
			}
			catch (Exception e)
			{
				throw new RuntimeException(e);
			}
		}
	}

	@Override public void mapFrameInitialized(MapFrame oldFrame, MapFrame newFrame) {
		if (newFrame != null)
			newFrame.mapView.addLayerChangeListener(this);
		else
			oldFrame.mapView.removeLayerChangeListener(this);
	}

	public void activeLayerChange(Layer oldLayer, Layer newLayer) {}

	public void layerAdded(Layer newLayer) {
		if (newLayer instanceof OsmDataLayer)
			((OsmDataLayer)newLayer).setMapPainter(new MapPaintVisitor());
    }

	public void layerRemoved(Layer oldLayer) {}
}
