package org.openstreetmap.josm.gui.preferences;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.plugins.PluginInformation;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.UrlLabel;

public class PluginPreference implements PreferenceSetting {

	private Map<PluginInformation, Boolean> pluginMap;

	public void addGui(final PreferenceDialog gui) {
		pluginMap = new HashMap<PluginInformation, Boolean>();
		Box pluginPanel = Box.createVerticalBox();
		Collection<PluginInformation> availablePlugins = new LinkedList<PluginInformation>();
		File[] pluginFiles = new File(Main.pref.getPreferencesDir()+"plugins").listFiles();
		if (pluginFiles != null) {
			Arrays.sort(pluginFiles);
			for (File f : pluginFiles)
				if (f.isFile() && f.getName().endsWith(".jar"))
					availablePlugins.add(new PluginInformation(f));
		}

		Collection<String> enabledPlugins = Arrays.asList(Main.pref.get("plugins").split(","));
		for (final PluginInformation plugin : availablePlugins) {
			boolean enabled = enabledPlugins.contains(plugin.name);
			final JCheckBox pluginCheck = new JCheckBox(plugin.name, enabled);
			pluginPanel.add(pluginCheck);

			pluginCheck.setToolTipText(plugin.file.getAbsolutePath());
			JLabel label = new JLabel("<html><i>"+(plugin.description==null?"no description available":plugin.description)+"</i></html>");
			label.setBorder(BorderFactory.createEmptyBorder(0,20,0,0));
			pluginPanel.add(label);
			pluginPanel.add(Box.createVerticalStrut(5));

			pluginMap.put(plugin, enabled);
			pluginCheck.addActionListener(new ActionListener(){
				public void actionPerformed(ActionEvent e) {
					pluginMap.put(plugin, pluginCheck.isSelected());
					gui.requiresRestart = true;
				}
			});
		}
		JScrollPane pluginPane = new JScrollPane(pluginPanel, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		pluginPane.setBorder(null);

		JPanel plugin = gui.createPreferenceTab("plugin", tr("Plugins"), tr("Configure available Plugins."));
		plugin.add(pluginPane, GBC.eol().fill(GBC.BOTH));
		plugin.add(GBC.glue(0,10), GBC.eol());
		plugin.add(new UrlLabel("http://josm.eigenheimstrasse.de/wiki/Plugins", tr("Get more plugins")), GBC.std().fill(GBC.HORIZONTAL));
	}

	public void ok() {
		String plugins = "";
		for (Entry<PluginInformation, Boolean> entry : pluginMap.entrySet())
			if (entry.getValue())
				plugins += entry.getKey().name + ",";
		if (plugins.endsWith(","))
			plugins = plugins.substring(0, plugins.length()-1);
		Main.pref.put("plugins", plugins);
	}
}
