package org.openstreetmap.josm.tools;

import java.awt.event.KeyEvent;

public class ShortCutLabel {
	public static String name(int shortCut, int modifiers) {
		String s = "";
		if ((modifiers & KeyEvent.CTRL_MASK) != 0)
			s += "Ctrl-";
		if ((modifiers & KeyEvent.ALT_MASK) != 0)
			s += "Alt-";
		if ((modifiers & KeyEvent.ALT_GRAPH_MASK) != 0)
			s += "AltGr-";
		if ((modifiers & KeyEvent.SHIFT_MASK) != 0)
			s += "Shift-";
		s += Character.toUpperCase((char)shortCut);
		return s;
	}
}
