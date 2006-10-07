package org.openstreetmap.josm.plugins;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;

/**
 * Encapsulate general information about a plugin. This information is available
 * without the need of loading any class from the plugin jar file.
 *
 * @author imi
 */
public class PluginInformation {
	public final File file;
	public final String name;
	public final String className;
	public final String description;

	public PluginInformation(File file) {
		this.file = file;
		name = file.getName().substring(0, file.getName().length()-4);
		try {
	        JarInputStream jar = new JarInputStream(new FileInputStream(file));
	        Manifest manifest = jar.getManifest();
	        className = manifest.getMainAttributes().getValue("Plugin-Class");
	        description = manifest.getMainAttributes().getValue("Plugin-Description");
	        jar.close();
        } catch (IOException e) {
        	throw new PluginException(null, name, e);
        }
    }

	/**
	 * Load and instantiate the plugin
	 */
	public PluginProxy load() {
		try {
			ClassLoader loader = URLClassLoader.newInstance(
					new URL[]{new URL("file://"+file.getAbsolutePath())},
					getClass().getClassLoader());
			Object plugin = Class.forName(className, true, loader).newInstance();
			return new PluginProxy(plugin, this);
		} catch (Exception e) {
			throw new PluginException(null, name, e);
		}
	}
}