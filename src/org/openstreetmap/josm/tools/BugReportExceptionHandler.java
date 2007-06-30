package org.openstreetmap.josm.tools;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.GridBagLayout;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.plugins.PluginException;
import org.openstreetmap.josm.plugins.PluginProxy;

/**
 * An exception handler, that ask the user to send a bug report.
 *
 * @author imi
 */
public final class BugReportExceptionHandler implements Thread.UncaughtExceptionHandler {

	public void uncaughtException(Thread t, Throwable e) {
		e.printStackTrace();
		if (Main.parent != null) {
			if (e instanceof OutOfMemoryError) {
				JOptionPane.showMessageDialog(Main.parent, "You are out of memory. Strange things may happen.\nPlease restart JOSM and load smaller data sets.");
				return;
			}

			// Check for an explicit problem when calling a plugin function
			if (e instanceof PluginException) {
				PluginProxy plugin = ((PluginException)e).plugin;
				if (plugin != null && !plugin.misbehaving) {
					JOptionPane.showMessageDialog(Main.parent, tr("The plugin {0} throwed an exception: {1}\nIt may be outdated. Please contact the plugin's autor.\nThis message will not shown again until JOSM is restarted.", plugin.info.name, e.getMessage()));
					plugin.misbehaving = true;
					return;
				}
			}

			// Try a heuristic to guess whether the problem may be within a plugin
			String pluginName = guessPlugin(e);
			if (pluginName != null) {
				boolean loaded = false;
				for (PluginProxy p : Main.plugins)
					if (p.info.name.equals(pluginName))
						loaded = true;
				if (loaded) {
					String author = findPluginAuthor(pluginName);
					int answer = JOptionPane.showConfirmDialog(
							Main.parent, 
							tr("An unexpected exception occoured, that may come from in the ''{0}'' plugin.", pluginName)+"\n"+
								(author != null ? tr("According to the information within the plugin, the author is {0}.", author) : "")+"\n"+
								tr("Should the plugin be disabled?"),
							tr("Disable plugin"),
							JOptionPane.YES_NO_OPTION);
					if (answer == JOptionPane.OK_OPTION) {
						LinkedList<String> plugins = new LinkedList<String>(Arrays.asList(Main.pref.get("plugins").split(",")));
						if (plugins.contains(pluginName)) {
							while (plugins.remove(pluginName)) {}
							String p = "";
							for (String s : plugins)
								p += ","+s;
							if (p.length() > 0)
								p = p.substring(1);
							Main.pref.put("plugins", p);
							JOptionPane.showMessageDialog(Main.parent, tr("The plugin has been removed from the configuration. Please restart JOSM to unload the plugin."));
						} else {
							JOptionPane.showMessageDialog(Main.parent, tr("The plugin could not be removed. Please tell the people you got JOSM from about the problem."));
						}
						return;
					}
				}
			}

			Object[] options = new String[]{tr("Do nothing"), tr("Report Bug")};
			int answer = JOptionPane.showOptionDialog(Main.parent, tr("An unexpected exception occoured.\n\n" +
					"This is always a coding error. If you are running the latest\n" +
			"version of JOSM, please consider be kind and file a bug report."),
			tr("Unexpected Exception"), JOptionPane.YES_NO_OPTION, JOptionPane.ERROR_MESSAGE,
			null, options, options[0]);
			if (answer == 1) {
				try {
					StringWriter stack = new StringWriter();
					e.printStackTrace(new PrintWriter(stack));

					URL revUrl = Main.class.getResource("/REVISION");
					StringBuilder sb = new StringBuilder(tr("Please send this to josm@eigenheimstrasse.de\n\n"));
					if (revUrl == null) {
						sb.append("Development version. Unknown revision.");
						File f = new File("org/openstreetmap/josm/Main.class");
						if (!f.exists())
							f = new File("bin/org/openstreetmap/josm/Main.class");
						if (!f.exists())
							f = new File("build/org/openstreetmap/josm/Main.class");
						if (f.exists()) {
							DateFormat sdf = SimpleDateFormat.getDateTimeInstance();
							sb.append("\nMain.class build on "+sdf.format(new Date(f.lastModified())));
							sb.append("\n");
						}
					} else {
						BufferedReader in = new BufferedReader(new InputStreamReader(revUrl.openStream()));
						for (String line = in.readLine(); line != null; line = in.readLine()) {
							sb.append(line);
							sb.append('\n');
						}
					}
					sb.append("\n"+stack.getBuffer().toString());

					JPanel p = new JPanel(new GridBagLayout());
					p.add(new JLabel(tr("Please send an email with the following information to josm@eigenheimstrasse.de")), GBC.eop());

					JTextArea info = new JTextArea(sb.toString(), 20, 60);
					info.setCaretPosition(0);
					info.setEditable(false);
					p.add(new JScrollPane(info), GBC.eop());

					JOptionPane.showMessageDialog(Main.parent, p);
				} catch (Exception e1) {
					e1.printStackTrace();
				}
			}
		}
	}

	/**
	 * Try to find the author of the given plugin. Return <code>null</code>
	 * if no author specified or the plugin is not found.
	 */
	private String findPluginAuthor(String pluginName) {
		for (PluginProxy proxy : Main.plugins)
			if (pluginName.equals(proxy.info.name))
				return proxy.info.author;
		return null;
	}

	/**
	 * Analyze the stack of the argument and return a name of a plugin, if
	 * some known problem pattern has been found or <code>null</code>, if
	 * the stack does not contain plugin-code.
	 * 
	 * Note: This heuristic is not meant as discrimination against specific
	 * plugins, but only to stop the flood of similar bug reports about plugins.
	 * Of course, plugin writers are free to install their own version of 
	 * an exception handler with their email address listed to receive 
	 * bug reports ;-). 
	 */
	private String guessPlugin(Throwable e) {
		for (StackTraceElement element : e.getStackTrace()) {
			String c = element.getClassName();
			if (c.contains("wmsplugin.") || c.contains(".WMSLayer"))
				return "wmsplugin";
			if (c.contains("landsat.") || c.contains(".LandsatLayer"))
				return "landsat";
			if (c.contains("livegps."))
				return "livegps";
			if (c.contains("mappaint."))
				return "mappaint";
			if (c.contains("annotationtester."))
				return "annotation-tester";
		}
		return null;
	}
}