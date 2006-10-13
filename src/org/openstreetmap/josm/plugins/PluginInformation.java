package org.openstreetmap.josm.plugins;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Map;
import java.util.TreeMap;
import java.util.jar.Attributes;
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
	public final boolean early;
	public final String author;

	public final Map<String, String> attr = new TreeMap<String, String>();

	public PluginInformation(File file) {
		this.file = file;
		name = file.getName().substring(0, file.getName().length()-4);
		try {
			JarInputStream jar = new JarInputStream(new FileInputStream(file));
			Manifest manifest = jar.getManifest();
			Attributes attr = manifest.getMainAttributes();
			className = attr.getValue("Plugin-Class");
			description = attr.getValue("Plugin-Description");
			early = Boolean.parseBoolean(attr.getValue("Plugin-Early"));
			author = attr.getValue("Author");
			for (Object o : attr.keySet())
				this.attr.put(o.toString(), attr.getValue(o.toString()));
			jar.close();
		} catch (IOException e) {
			throw new PluginException(null, name, e);
		}
	}

	/**
	 * Load and instantiate the plugin
	 */
	public PluginProxy load(Class<?> klass) {
		try {
			return new PluginProxy(klass.newInstance(), this);
		} catch (Exception e) {
			throw new PluginException(null, name, e);
		}
	}

	/**
	 * Load the class of the plugin
	 */
	public Class<?> loadClass() {
		try {
			ClassLoader loader = URLClassLoader.newInstance(
					new URL[]{new URL(getURLString())},
					getClass().getClassLoader());
			Class<?> realClass = Class.forName(className, true, loader);
			return realClass;
		} catch (Exception e) {
			throw new PluginException(null, name, e);
		}
	}

	private String getURLString() {
		if (System.getProperty("os.name").startsWith("Windows"))
			return "file:/"+file.getAbsolutePath();
		return "file://"+file.getAbsolutePath();
	}
}