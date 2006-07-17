package org.openstreetmap.josm.tools;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.KeyEvent;


public class ShortCutLabel {
	public static String name(int shortCut, int modifiers) {
		String s = "";
		if ((modifiers & KeyEvent.CTRL_MASK) != 0)
			s += tr("Ctrl-");
		if ((modifiers & KeyEvent.ALT_MASK) != 0)
			s += tr("Alt-");
		if ((modifiers & KeyEvent.ALT_GRAPH_MASK) != 0)
			s += tr("AltGr-");
		if ((modifiers & KeyEvent.SHIFT_MASK) != 0)
			s += tr("Shift-");
		if (shortCut >= KeyEvent.VK_F1 && shortCut <= KeyEvent.VK_F12)
			s += "F"+(shortCut-KeyEvent.VK_F1+1);
		else
			s += Character.toUpperCase((char)shortCut);
		return s;
	}
}
