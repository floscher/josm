package org.openstreetmap.josm.tools;

import org.openstreetmap.josm.Main;
import org.xnap.commons.i18n.I18nFactory;

/**
 * Internationalisation support.
 * 
 * @author Immanuel.Scholz
 */
public class I18n {
	private static org.xnap.commons.i18n.I18n i18n = I18nFactory.getI18n(Main.class);
	
	public static String tr(String text, Object... objects) {
		return i18n.tr(text, objects);
	}

	public static String tr(String text) {
		return i18n.tr(text);
	}

	public static String trn(String text, String pluralText, long n, Object... objects) {
		return i18n.trn(text, pluralText, n, objects);
	}

	public static String trn(String text, String pluralText, long n) {
		return i18n.trn(text, pluralText, n);
	}
}
