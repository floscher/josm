package org.openstreetmap.josm.tools;

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

/**
 * An exception handler, that ask the user to send a bug report.
 * 
 * @author imi
 */
public final class BugReportExceptionHandler implements Thread.UncaughtExceptionHandler {
	public void uncaughtException(Thread t, Throwable e) {
		e.printStackTrace();
		if (Main.parent != null) {
			Object[] options = new String[]{"Do nothing", "Report Bug"};
			int answer = JOptionPane.showOptionDialog(Main.parent, "An unexpected exception occoured.\n\n" +
					"This is always a coding error. If you are running the latest\n" +
					"version of JOSM, please consider be kind and file a bug report.",
					"Unexpected Exception", JOptionPane.YES_NO_OPTION, JOptionPane.ERROR_MESSAGE,
					null, options, options[0]);
			if (answer == 1) {
				try {
					StringWriter stack = new StringWriter();
					e.printStackTrace(new PrintWriter(stack));

					URL revUrl = Main.class.getResource("/REVISION");
					StringBuilder sb = new StringBuilder("Please send this to josm@eigenheimstrasse.de\n\n");
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
					p.add(new JLabel("Please send an email with the following information to josm@eigenheimstrasse.de"), GBC.eop());

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