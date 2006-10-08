package org.openstreetmap.josm.plugins;

import org.openstreetmap.josm.gui.MapFrame;


/**
 * Helper class for the JOSM system to communicate with the plugin.
 *
 * This class should be of no interest for sole plugin writer.
 *
 * @author Immanuel.Scholz
 */
public class PluginProxy extends Plugin {

	public final Object plugin;
	public final PluginInformation info;
	public boolean misbehaving = false;

	public PluginProxy(Object plugin, PluginInformation info) {
		this.plugin = plugin;
		this.info = info;
    }

	@Override public void mapFrameInitialized(MapFrame oldFrame, MapFrame newFrame) {
		try {
	        plugin.getClass().getMethod("mapFrameInitialized", MapFrame.class, MapFrame.class).invoke(plugin, oldFrame, newFrame);
        } catch (NoSuchMethodException e) {
        } catch (Exception e) {
        	throw new PluginException(this, info.name, e);
        }
    }
}
