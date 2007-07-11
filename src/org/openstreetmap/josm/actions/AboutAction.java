package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.plugins.PluginProxy;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.UrlLabel;

/**
 * Nice about screen. I guess every application need one these days.. *sigh*
 * 
 * The REVISION resource is read and if present, it shows the revision 
 * information of the jar-file.
 * 
 * @author imi
 */
public class AboutAction extends JosmAction {

	public static final String version;

	private final static JTextArea revision;
	private static String time;

	static {
		revision = loadFile(Main.class.getResource("/REVISION"));

		Pattern versionPattern = Pattern.compile(".*?Revision: ([0-9]*).*", Pattern.CASE_INSENSITIVE|Pattern.DOTALL);
		Matcher match = versionPattern.matcher(revision.getText());
		version = match.matches() ? match.group(1) : "UNKNOWN";

		Pattern timePattern = Pattern.compile(".*?Last Changed Date: ([^\n]*).*", Pattern.CASE_INSENSITIVE|Pattern.DOTALL);
		match = timePattern.matcher(revision.getText());
		time = match.matches() ? match.group(1) : "UNKNOWN";
	}

	public AboutAction() {
		super(tr("About"), "about",tr("Display the about screen."), KeyEvent.VK_F1, KeyEvent.SHIFT_DOWN_MASK, true);
	}

	public void actionPerformed(ActionEvent e) {
		JTabbedPane about = new JTabbedPane();

		JTextArea readme = loadFile(Main.class.getResource("/README"));
		JTextArea contribution = loadFile(Main.class.getResource("/CONTRIBUTION"));
		JTextArea plugins = loadFile(null);

		JPanel info = new JPanel(new GridBagLayout());
		info.add(new JLabel(tr("Java OpenStreetMap Editor Version {0}",version)), GBC.eol());
		info.add(new JLabel(tr("last change at {0}",time)), GBC.eol());
		info.add(new JLabel(tr("Java Version {0}",System.getProperty("java.version"))), GBC.eol());
		info.add(new JLabel(tr("Latest Version on JOSM homepage is")), GBC.std().insets(0,0,5,0));
		final JLabel checkVersionLabel = new JLabel("<html><em>"+tr("checking...")+"</em></html>");
		info.add(checkVersionLabel, GBC.eol());
		new Thread(){
			@Override public void run() {
				final String version = checkLatestVersion();
				try {
					if (version == null)
						throw new NullPointerException();
	                SwingUtilities.invokeAndWait(new Runnable(){
	                	public void run() {
	                		checkVersionLabel.setText(version);
	                    }
	                });
                } catch (Exception e) {
	                checkVersionLabel.setText("failed.");
                }
            }
		}.start();
		
		info.add(GBC.glue(0,10), GBC.eol());
		
		info.add(new JLabel(tr("Homepage")), GBC.std().insets(0,0,10,0));
		info.add(new UrlLabel("http://josm.openstreetmap.de"), GBC.eol());
		info.add(new JLabel(tr("Bug Reports")), GBC.std().insets(0,0,10,0));
		info.add(new UrlLabel("http://josm.openstreetmap.de/newticket"), GBC.eol());
		info.add(new JLabel(tr("News about JOSM")), GBC.std().insets(0,0,10,0));
		info.add(new UrlLabel("http://www.opengeodata.org/?cat=17"), GBC.eol());

		StringBuilder pluginsStr = new StringBuilder();
		for (PluginProxy p : Main.plugins)
			pluginsStr.append(p.info.name + "\n");
		plugins.setText(pluginsStr.toString());
		plugins.setCaretPosition(0);

		about.addTab(tr("Info"), info);
		about.addTab(tr("Readme"), createScrollPane(readme));
		about.addTab(tr("Revision"), createScrollPane(revision));
		about.addTab(tr("Contribution"), createScrollPane(contribution));
		about.addTab(tr("Plugins"), createScrollPane(plugins));

		about.setPreferredSize(new Dimension(500,300));

		JOptionPane.showMessageDialog(Main.parent, about, tr("About JOSM..."),
				JOptionPane.INFORMATION_MESSAGE, ImageProvider.get("logo"));
	}

	private JScrollPane createScrollPane(JTextArea area) {
		area.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
		area.setOpaque(false);
	    JScrollPane sp = new JScrollPane(area);
		sp.setBorder(null);
		sp.setOpaque(false);
	    return sp;
    }

	/**
	 * Retrieve the latest JOSM version from the JOSM homepage.
	 * @return An string with the latest version or "UNKNOWN" in case
	 * 		of problems (e.g. no internet connection).
	 */
	public static String checkLatestVersion() {
        String latest;
        try {
        	InputStream s = new URL("http://josm.openstreetmap.de/current").openStream();
        	latest = new BufferedReader(new InputStreamReader(s)).readLine();
        	s.close();
        } catch (IOException x) {
        	x.printStackTrace();
        	return "UNKNOWN";
        }
        return latest;
    }

	/**
	 * Load the specified ressource into an TextArea and return it.
	 * @param resource The resource url to load
	 * @return	An read-only text area with the content of "resource"
	 */
	private static JTextArea loadFile(URL resource) {
		JTextArea area = new JTextArea(tr("File could not be found."));
		area.setEditable(false);
		Font font = Font.getFont("monospaced");
		if (font != null)
			area.setFont(font);
		if (resource == null)
			return area;
		BufferedReader in;
		try {
			in = new BufferedReader(new InputStreamReader(resource.openStream()));
			StringBuilder sb = new StringBuilder();
			for (String line = in.readLine(); line != null; line = in.readLine()) {
				sb.append(line);
				sb.append('\n');
			}
			area.setText(sb.toString());
			area.setCaretPosition(0);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return area;
	}
}
