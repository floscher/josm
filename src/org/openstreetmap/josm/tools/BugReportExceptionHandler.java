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
import java.util.Date;

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

			if (e instanceof PluginException) {
				PluginProxy plugin = ((PluginException)e).getPlugin();
				if (plugin != null && !plugin.misbehaving) {
					JOptionPane.showMessageDialog(Main.parent, tr("The plugin {0} throwed an exception: {1}\nIt may be outdated. Please contact the plugin's autor.\nThis message will not shown again until JOSM is restarted.", plugin.info.name, e.getMessage()));
					plugin.misbehaving = true;
					return;
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
}