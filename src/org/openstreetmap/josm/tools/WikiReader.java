// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.tools;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;

import org.openstreetmap.josm.Main;

/**
 * Read a trac-wiki page.
 *
 * @author imi
 */
public class WikiReader {

    private final String baseurl;

    public WikiReader(String baseurl) {
        this.baseurl = baseurl;
    }

    public WikiReader() {
        this.baseurl = Main.pref.get("help.baseurl", "http://josm.openstreetmap.de");
    }

    /**
     * Read the page specified by the url and return the content.
     *
     * If the url is within the baseurl path, parse it as an trac wikipage and
     * replace relative pathes etc..
     *
     * @return Either the string of the content of the wiki page.
     * @throws IOException Throws, if the page could not be loaded.
     */
    public String read(String url) throws IOException {
        BufferedReader in = new BufferedReader(new InputStreamReader(new URL(url).openStream(), "utf-8"));
        if (url.startsWith(baseurl) && !url.endsWith("?format=txt"))
            return readFromTrac(in);
        return readNormal(in);
    }

    public String readLang(String text) {
        String languageCode = Main.getLanguageCodeU();
        String url = baseurl + "/wiki/"+languageCode+text;
        String res = "";
        try {
            res = readFromTrac(new BufferedReader(new InputStreamReader(new URL(url).openStream(), "utf-8")));
        } catch (IOException ioe) {}
        if(res.length() == 0 && languageCode.length() != 0)
        {
            url = baseurl + "/wiki/"+text;
            try {
                res = readFromTrac(new BufferedReader(new InputStreamReader(new URL(url).openStream(), "utf-8")));
            } catch (IOException ioe) {}
        }
        return res;
    }

    private String readNormal(BufferedReader in) throws IOException {
        String b = "";
        for (String line = in.readLine(); line != null; line = in.readLine()) {
            if(!line.contains("[[TranslatedPages]]"))
                b += line.replaceAll(" />", ">") + "\n";
        }
        return "<html>" + b + "</html>";
    }

    private String readFromTrac(BufferedReader in) throws IOException {
        boolean inside = false;
        boolean transl = false;
        String b = "";
        for (String line = in.readLine(); line != null; line = in.readLine()) {
            if (line.contains("<div id=\"searchable\">"))
                inside = true;
            else if (line.contains("<div class=\"wiki-toc trac-nav\""))
                transl = true;
            else if (line.contains("<div class=\"wikipage searchable\">"))
                inside = true;
            else if (line.contains("<div class=\"buttons\">"))
                inside = false;
            if (inside && !transl) {
                b += line.replaceAll("<img src=\"/", "<img src=\""+baseurl+"/")
                         .replaceAll("href=\"/", "href=\""+baseurl+"/")
                         .replaceAll(" />", ">") + "\n";
            }
            else if (transl && line.contains("</div>"))
                transl = false;
        }
        if(b.indexOf("      Describe ") >= 0)
            return "";
        return "<html>" + b + "</html>";
    }
}
