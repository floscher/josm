package org.openstreetmap.josm.plugins;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;

/**
 * Helper class to detect and load plugins.
 * 
 * @author Immanuel.Scholz
 */
public class PluginLoader {

	/**
	 * @return The class name from the manifest of the jar given as parameter.
	 */
	public String loadClassName(File pluginFile) throws PluginException {
	    try {
	        JarInputStream jar = new JarInputStream(new FileInputStream(pluginFile));
	        Manifest manifest = jar.getManifest();
	        String pluginClass = manifest.getMainAttributes().getValue("Plugin-Class");
	        jar.close();
	        return pluginClass;
        } catch (IOException e) {
        	String name = pluginFile.getName();
        	if (name.endsWith(".jar"))
        		name = name.substring(0, name.length()-4);
        	throw new PluginException(null, name, e);
        }
    }

	/**
	 * Load and instantiate the plugin
	 */
	public PluginProxy loadPlugin(String pluginClass, File pluginFile) throws PluginException {
		String name = pluginFile.getName();
		if (name.endsWith(".jar"))
			name = name.substring(0, name.length()-4);
		try {
			ClassLoader loader = URLClassLoader.newInstance(
					new URL[]{new URL("file:/"+pluginFile.getAbsolutePath())},
					getClass().getClassLoader());
			Object plugin = Class.forName(pluginClass, true, loader).newInstance();
			return new PluginProxy(plugin, name);
        } catch (Exception e) {
        	throw new PluginException(null, name, e);
        }
	}
}
