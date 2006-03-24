package org.openstreetmap.josm.actions;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.gui.GBC;
import org.openstreetmap.josm.gui.ImageProvider;
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
	
	public AboutAction() {
		super("About", "about", "Display the about screen.", KeyEvent.VK_A);
	}
	
	public void actionPerformed(ActionEvent e) {
		JTabbedPane about = new JTabbedPane();
		
		JTextArea readme = loadFile(Main.class.getResource("/README"));
		JTextArea revision = loadFile(Main.class.getResource("/REVISION"));
		
		Pattern versionPattern = Pattern.compile(".*?Revision: ([0-9]*).*", Pattern.CASE_INSENSITIVE|Pattern.DOTALL);
		Pattern timePattern = Pattern.compile(".*?Last Changed Date: ([^\n]*).*", Pattern.CASE_INSENSITIVE|Pattern.DOTALL);

		Matcher match = versionPattern.matcher(revision.getText());
		String version = match.matches() ? match.group(1) : "UNKNOWN";
		match = timePattern.matcher(revision.getText());
		String time = match.matches() ? match.group(1) : "UNKNOWN";
		
		JPanel info = new JPanel(new GridBagLayout());
		info.add(new JLabel("Java OpenStreetMap Editor Version "+version), GBC.eop());
		info.add(new JLabel("last change at "+time), GBC.eop());
		info.add(new JLabel("Homepage"), GBC.std().insets(0,0,10,0));
		info.add(new UrlLabel("http://wiki.eigenheimstrasse.de/wiki/JOSM"), GBC.eol());
		
		
		
		about.addTab("Info", info);
		about.addTab("Readme", new JScrollPane(readme));
		about.addTab("Revision", new JScrollPane(revision));
		
		about.setPreferredSize(new Dimension(500,300));
		
		JOptionPane.showMessageDialog(Main.main, about, "About JOSM...",
				JOptionPane.INFORMATION_MESSAGE, ImageProvider.get("logo"));
	}
	
	/**
	 * Load the specified ressource into an TextArea and return it.
	 * @param resource The resource url to load
	 * @return	An read-only text area with the content of "resource"
	 */
	private JTextArea loadFile(URL resource) {
		JTextArea area = new JTextArea("File could not be found.");
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
