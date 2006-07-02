package org.openstreetmap.josm.tools;

import java.text.MessageFormat;

/**
 * Internationalisation support.
 * 
 * @author Immanuel.Scholz
 */
public class I18n {
	public static String tr(String text, Object... objects) {
		MessageFormat mf = new MessageFormat(text);
		return mf.format(objects);
	}

	public static String tr(String text) {
		return text;
	}

	public static String trn(String text, String pluralText, long n, Object... objects) {
		return n==1 ? tr(text,objects) : tr(pluralText,objects);
	}

	public static String trn(String text, String pluralText, long n) {
		return n==1 ? text : pluralText;
	}
}
