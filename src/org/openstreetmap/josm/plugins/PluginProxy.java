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
	public final String name;
	public boolean misbehaving = false;

	public PluginProxy(Object plugin, String name) {
		this.plugin = plugin;
		this.name = name;
    }


	
	@Override public void mapFrameInitialized(MapFrame oldFrame, MapFrame newFrame) {
		try {
	        plugin.getClass().getMethod("mapFrameInitialized", MapFrame.class, MapFrame.class).invoke(plugin, oldFrame, newFrame);
        } catch (Exception e) {
        	throw new PluginException(this, name, e);
        }
    }

	@Override public String getDescription() {
	    try {
	        return (String)plugin.getClass().getMethod("getDescription").invoke(plugin);
        } catch (NoSuchMethodException e) {
        	return super.getDescription();
        } catch (Exception e) {
        	throw new PluginException(this, name, e);
        }
    }
}
