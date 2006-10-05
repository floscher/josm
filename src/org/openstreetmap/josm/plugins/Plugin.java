package org.openstreetmap.josm.plugins;

import java.net.URL;
import java.net.URLClassLoader;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.gui.MapFrame;

/**
 * All plugins *must* have an standard constructor taking no arguments.
 * This constructor is called at JOSM startup, after all Main-objects have been initialized.
 * For all purposes of loading dynamic resources, the Plugin's class loader should be used
 * (or else, the plugin jar will not be within the class path).
 *
 * All plugins should have at least one class subclassing this abstract base class. 
 * 
 * The actual implementation of this interface is optional, as all functions will be called
 * via reflection. This is to be able to change this interface without the need of recompiling
 * or even breaking the plugins. If your class does not provide a function here (or does
 * provide a function with a mismatching signature), it will not be called. That simple.
 *
 * Or in other words: See this base class as an documentation of what functions are provided.
 * Subclassing it and overriding some functions makes it easy for you to keep sync with the
 * correct actual plugin architecture of JOSM.
 *
 *
 * The pluginname provided to the constructor is also the name of the directory to 
 * store the plugin's own stuff (located under the josm preferences directory)
 *
 * @author Immanuel.Scholz
 */
public abstract class Plugin {

	private String name;

	public Plugin() {
		URL[] urls = ((URLClassLoader)getClass().getClassLoader()).getURLs();
		String s = urls[urls.length-1].toString();
		int lastSlash = s.lastIndexOf('/');
		name = s.substring(lastSlash+1, s.length()-4);
    }
	
	/**
	 * @return The name of this plugin. This is the name of the .jar file.
	 */
	public final String getName() {
		return name;
	}
	/**
	 * @return The directory for the plugin to store all kind of stuff.
	 */
	public final String getPluginDir() {
		return Main.pref.getPreferencesDir()+"plugins/"+name+"/";
	}

	

	/**
	 * Called after Main.mapFrame is initalized. (After the first data is loaded).
	 * You can use this callback to tweak the newFrame to your needs, as example install
	 * an alternative Painter.
	 */
	public void mapFrameInitialized(MapFrame oldFrame, MapFrame newFrame) {}

	/**
	 * Called to retrieve a one-liner description of what this plugin does for tooltips.
	 * @return <code>null</code>, which means: "no description available".
	 */
	public String getDescription() {return null;}

}
