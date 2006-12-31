package org.openstreetmap.josm.tools;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Container;
import java.awt.GridBagLayout;

import javax.swing.JCheckBox;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import org.openstreetmap.josm.Main;

public class DontShowAgainInfo {

	public static boolean show(String prefKey, Container msg) {
		if (!Main.pref.getBoolean("message."+prefKey)) {
			JCheckBox dontshowagain = new JCheckBox(tr("Do not show again"));
			dontshowagain.setSelected(Main.pref.getBoolean("message."+prefKey, true));
			JPanel all = new JPanel(new GridBagLayout());
			all.add(msg, GBC.eop());
			all.add(dontshowagain, GBC.eol());
			int answer = JOptionPane.showConfirmDialog(Main.parent, all, tr("Information"), JOptionPane.OK_CANCEL_OPTION);
			if (answer != JOptionPane.OK_OPTION)
				return false;
			Main.pref.put("message."+prefKey, dontshowagain.isSelected());
		}
		return true;
	}
}
