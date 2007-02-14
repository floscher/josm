package org.openstreetmap.josm.plugins;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.gui.MapFrame;
import org.openstreetmap.josm.gui.preferences.PreferenceSetting;

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
		name = urls[urls.length-1].toString();
		if (name.toLowerCase().endsWith(".jar")) {
			int lastSlash = name.lastIndexOf('/');
			name = name.substring(lastSlash+1, name.length()-4);
		}
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
	 * Called in the preferences dialog to create a preferences page for the plugin,
	 * if any available.
	 */
	public PreferenceSetting getPreferenceSetting() {return null;}
	
	
	/**
	 * Copies the ressource 'from' to the file in the plugin directory named 'to'.
	 */
	public void copy(String from, String to) throws FileNotFoundException, IOException {
		File pluginDir = new File(getPluginDir());
		if (!pluginDir.exists())
			pluginDir.mkdirs();
    	FileOutputStream out = new FileOutputStream(getPluginDir()+to);
    	InputStream in = getClass().getResourceAsStream(from);
    	byte[] buffer = new byte[8192];
    	for(int len = in.read(buffer); len > 0; len = in.read(buffer))
    		out.write(buffer, 0, len);
    	in.close();
    	out.close();
    }
}
