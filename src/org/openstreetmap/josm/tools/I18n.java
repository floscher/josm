package org.openstreetmap.josm.tools;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
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
		String localeFile = Main.pref.getPreferencesDir()+"lang/"+Locale.getDefault()+".jar";
		Class<?> klass = Main.class;
		if (new File(localeFile).exists()) {
			try {
				String url = localeFile.replace('\\','/');
				if (System.getProperty("os.name").startsWith("Windows"))
					url = "file:/"+url;
				else
					url = "file://"+url;
		        URLClassLoader loader = new URLClassLoader(new URL[]{new URL(url)});
		        klass = Class.forName("org.openstreetmap.josm.Translation_"+Locale.getDefault(), true, loader);
	        } catch (Exception e) {
	        	System.out.println("Couldn't load locale file "+localeFile);
	        	e.printStackTrace();
	        }
		}
		try {
			i18n = I18nFactory.getI18n(klass);
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
