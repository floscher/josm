package org.openstreetmap.josm.tools;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.MissingResourceException;

import org.openstreetmap.josm.Main;
import org.xnap.commons.i18n.I18nFactory;

/**
 * Internationalisation support.
 * 
 * @author Immanuel.Scholz
 */
public class I18n {
	private static org.xnap.commons.i18n.I18n i18n;
	
	static {
		try {
	        i18n = I18nFactory.getI18n(Main.class);
        } catch (MissingResourceException e) {
        	System.out.println("Locale '"+Locale.getDefault()+"' not found. Using default.");
        }
	}
	
	public static String tr(String text, Object... objects) {
		if (i18n == null)
			return MessageFormat.format(text, objects);
		return i18n.tr(text, objects);
	}

	public static String tr(String text) {
		if (i18n == null)
			return text;
		return i18n.tr(text);
	}

	public static String trn(String text, String pluralText, long n, Object... objects) {
		if (i18n == null)
			return n == 1 ? tr(text, objects) : tr(pluralText, objects);
		return i18n.trn(text, pluralText, n, objects);
	}

	public static String trn(String text, String pluralText, long n) {
		if (i18n == null)
			return n == 1 ? tr(text) : tr(pluralText);
		return i18n.trn(text, pluralText, n);
	}
}
