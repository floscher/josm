// License: GPL. See LICENSE file for details.

package org.openstreetmap.josm.gui;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import java.awt.BorderLayout;
import java.awt.EventQueue;

import javax.swing.JScrollPane;
import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.border.EmptyBorder;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.io.CacheCustomContent;
import org.openstreetmap.josm.tools.OpenBrowser;
import org.openstreetmap.josm.tools.WikiReader;
import org.openstreetmap.josm.actions.AboutAction;

public class GettingStarted extends JPanel {
    private String content = "";
    static private String styles = "<style type=\"text/css\">\n"+
            "body { font-family: sans-serif; font-weight: bold; }\n"+
            "h1 {text-align: center;}\n"+
            "</style>\n";

    public class LinkGeneral extends JEditorPane implements HyperlinkListener {
        public LinkGeneral(String text) {
            setContentType("text/html");
            setText(text);
            setEditable(false);
            setOpaque(false);
            addHyperlinkListener(this);
        }
        public void hyperlinkUpdate(HyperlinkEvent e) {
            if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                OpenBrowser.displayUrl(e.getDescription());
            }
        }
    }

    /**
     * Grabs current MOTD from cache or webpage and parses it.
     */
    private class assignContent extends CacheCustomContent {
        public assignContent() {
            super("motd.html", CacheCustomContent.INTERVAL_DAILY);
        }

        final private int myVersion = AboutAction.getVersionNumber();

        /**
         * This function gets executed whenever the cached files need updating
         * @see org.openstreetmap.josm.io.CacheCustomContent#updateData()
         */
        protected byte[] updateData() {
            String motd = "";
            String baseurl = Main.pref.get("help.baseurl", "http://josm.openstreetmap.de");
            WikiReader wr = new WikiReader(baseurl);
            String vers = "";
            String languageCode = Main.getLanguageCodeU();
            try {
                motd = wr.read(baseurl + "/wiki/"+languageCode+"StartupPage");
            } catch (IOException ioe) {
                try {
                    motd = wr.read(baseurl + "/wiki/StartupPage");
                } catch (IOException ioe2) {
                    motd = "<html>" + styles + "<body><h1>" +
                        "JOSM - " + tr("Java OpenStreetMap Editor") +
                        "</h1>\n<h2 align=\"center\">(" +
                        tr("Message of the day not available") +
                        ")</h2></html>";
                }
            }
            try {
                vers = wr.read(baseurl + "/version?format=txt");
                motd = motd.replace("</html>", getVersionNumber(vers)+"</html>");
            } catch (IOException ioe) {}
            // Save this to prefs in case JOSM is updated so MOTD can be refreshed
            Main.pref.putInteger("cache.motd.html.version", myVersion);

            return motd.getBytes();
        }

        /**
         * Additionally check if JOSM has been updated and refresh MOTD
         */
        @Override
        protected boolean isCacheValid() {
            // We assume a default of myVersion because it only kicks in in two cases:
            // 1. Not yet written - but so isn't the interval variable, so it gets updated anyway
            // 2. Cannot be written (e.g. while developing). Obviously we don't want to update
            //    everytime because of something we can't read.
            return Main.pref.getInteger("cache.motd.html.version", myVersion) == myVersion;
        }

        /**
         * Tries to read the version number from a given Future<String>
         * @param Future<String> that contains the version page
         * @return String with HTML Code
         */
        private String getVersionNumber(String str) {
            try {
                Matcher m = Pattern.compile(".*josm-tested\\.jar: *(\\d+).*", Pattern.DOTALL).matcher(str);
                m.matches();
                int curVersion = Integer.parseInt(m.group(1));
                m = Pattern.compile(".*josm-latest\\.jar: *(\\d+).*", Pattern.DOTALL).matcher(str);
                m.matches();
                int latest = Integer.parseInt(m.group(1));
                return "<div style=\"text-align:right;font-size:small;font-weight:normal;\">"
                + "<b>"
                + (curVersion > myVersion ? tr("Update available") + " &#151; ": "")
                + tr("Version Details:") + "</b> "
                + tr("Yours: {2}; Current: {0}; <font style=\"font-size:x-small\">"
                + "(latest untested: {1} &#150; not recommended)</font>",
                curVersion, latest, myVersion)
                + "</div>";
            } catch(Exception e) {
              // e.printStackTrace();
            }

            return "";
        }
    }

    /**
     * Initializes getting the MOTD as well as enabling the FileDrop Listener.
     * Displays a message while the MOTD is downloading.
     */
    public GettingStarted() {
        super(new BorderLayout());
        final LinkGeneral lg = new LinkGeneral(
            "<html>" +
            styles +
            "<h1>" +
            "JOSM - " +
            tr("Java OpenStreetMap Editor") +
            "</h1><h2 align=\"center\">" +
            tr("Downloading \"Message of the day\"") +
            "</h2>");
        JScrollPane scroller = new JScrollPane(lg);
        scroller.setViewportBorder(new EmptyBorder(10,100,10,100));
        add(scroller, BorderLayout.CENTER);

        // Asynchronously get MOTD to speed-up JOSM startup
        Thread t = new Thread(new Runnable() {
            public void run() {
                if (content.length() == 0 && Main.pref.getBoolean("help.displaymotd", true))
                    content = new assignContent().updateIfRequiredString();

                EventQueue.invokeLater(new Runnable() {
                    public void run() {
                       lg.setText(content);
                       //lg.moveCaretPosition(0);
                    }
                });
            }
        }, "MOTD-Loader");
        t.setDaemon(true);
        t.start();

        new FileDrop(scroller);
    }
}
