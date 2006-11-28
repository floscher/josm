package org.openstreetmap.josm.plugins;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
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
	public final List<URL> libraries = new ArrayList<URL>();

	public final Map<String, String> attr = new TreeMap<String, String>();

	/**
	 * @param file the plugin jar file.
	 */
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
			libraries.add(new URL(getURLString(file.getAbsolutePath())));
			String classPath = attr.getValue("Class-Path");
			if (classPath != null) {
				String[] cp = classPath.split(" ");
				StringBuilder entry = new StringBuilder();
				for (String s : cp) {
					entry.append(s);
					if (s.endsWith("\\")) {
						entry.setLength(entry.length()-1);
						entry.append("%20"); // append the split character " " as html-encode
						continue;
					}
					s = entry.toString();
					entry = new StringBuilder();
					if (!s.startsWith("/") && !s.startsWith("\\") && !s.matches("^.\\:"))
						s = file.getParent() + File.separator + s;
					libraries.add(new URL(getURLString(s)));
				}
			}

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
			URL[] urls = new URL[libraries.size()];
			urls = libraries.toArray(urls);
			ClassLoader loader = URLClassLoader.newInstance(urls, getClass().getClassLoader());
			Class<?> realClass = Class.forName(className, true, loader);
			return realClass;
		} catch (Exception e) {
			throw new PluginException(null, name, e);
		}
	}

	private String getURLString(String fileName) {
		if (System.getProperty("os.name").startsWith("Windows"))
			return "file:/"+fileName;
		return "file://"+fileName;
	}
}