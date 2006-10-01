package org.openstreetmap.josm.plugins;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.gui.MapFrame;

/**
 * All plugins must have at least one class implementing this interface. 
 * 
 * All plugins must have an default constructor (taking no arguments). This constructor
 * is called at JOSM startup, after all Main-objects have been initialized.
 *
 * The pluginname is also the name of the directory to store the plugin's
 * own stuff (located under the josm preferences directory)
 * @author Immanuel.Scholz
 */
public abstract class Plugin {

	private final String name;

	public Plugin(String pluginName) {
		this.name = pluginName;
	}

	public final String getPluginDir() {
		return Main.pref.getPreferencesDir()+"plugins/"+name+"/";
	}

	/**
	 * Called after Main.mapFrame is initalized. (After the first data is loaded).
	 */
	public void mapFrameInitialized(MapFrame mapFrame) {}
}
