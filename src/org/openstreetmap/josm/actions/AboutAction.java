//License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.Version;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.plugins.PluginHandler;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.Shortcut;
import org.openstreetmap.josm.tools.UrlLabel;

/**
 * Nice about screen. I guess every application need one these days.. *sigh*
 *
 * The REVISION resource is read and if present, it shows the revision
 * information of the jar-file.
 *
 * @author imi
 */
public class AboutAction extends JosmAction {

    public AboutAction() {
        super(tr("About"), "about", tr("Display the about screen."),
            Shortcut.registerShortcut("system:about", tr("About"),
            KeyEvent.VK_F1, Shortcut.SHIFT), true);
    }

    public void actionPerformed(ActionEvent e) {
        final JTabbedPane about = new JTabbedPane();

        Version version = Version.getInstance();

        JTextArea readme = new JTextArea();
        readme.setEditable(false);
        readme.setText(Version.loadResourceFile(Main.class.getResource("/README")));
        readme.setCaretPosition(0);

        JTextArea revision = new JTextArea();
        revision.setEditable(false);
        revision.setText(version.getReleaseAttributes());
        revision.setCaretPosition(0);

        JTextArea contribution = new JTextArea();
        contribution.setEditable(false);
        contribution.setText(Version.loadResourceFile(Main.class.getResource("/CONTRIBUTION")));
        contribution.setCaretPosition(0);

        JTextArea license = new JTextArea();
        license.setEditable(false);
        license.setText(Version.loadResourceFile(Main.class.getResource("/LICENSE")));
        license.setCaretPosition(0);

        JPanel info = new JPanel(new GridBagLayout());
        JLabel caption = new JLabel("JOSM – " + tr("Java OpenStreetMap Editor"));
        caption.setFont(GuiHelper.getTitleFont());
        info.add(caption, GBC.eol().fill(GBC.HORIZONTAL).insets(10,0,0,0));
        info.add(GBC.glue(0,10), GBC.eol());
        info.add(new JLabel(tr("Version {0}", version.getVersionString())), GBC.eol().fill(GBC.HORIZONTAL).insets(10,0,0,0));
        info.add(GBC.glue(0,5), GBC.eol());
        info.add(new JLabel(tr("Last change at {0}",version.getTime())), GBC.eol().fill(GBC.HORIZONTAL).insets(10,0,0,0));
        info.add(GBC.glue(0,5), GBC.eol());
        info.add(new JLabel(tr("Java Version {0}",System.getProperty("java.version"))), GBC.eol().fill(GBC.HORIZONTAL).insets(10,0,0,0));
        info.add(GBC.glue(0,10), GBC.eol());
        info.add(new JLabel(tr("Homepage")), GBC.std().insets(10,0,10,0));
        info.add(new UrlLabel("http://josm.openstreetmap.de",2), GBC.eol().fill(GBC.HORIZONTAL));
        info.add(GBC.glue(0,5), GBC.eol());
        info.add(new JLabel(tr("Bug Reports")), GBC.std().insets(10,0,10,0));
        info.add(new UrlLabel("http://josm.openstreetmap.de/newticket",2), GBC.eol().fill(GBC.HORIZONTAL));

        about.addTab(tr("Info"), info);
        about.addTab(tr("Readme"), createScrollPane(readme));
        about.addTab(tr("Revision"), createScrollPane(revision));
        about.addTab(tr("Contribution"), createScrollPane(contribution));
        about.addTab(tr("License"), createScrollPane(license));
        about.addTab(tr("Plugins"), new JScrollPane(PluginHandler.getInfoPanel()));

        // Intermediate panel to allow proper optionPane resizing
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setPreferredSize(new Dimension(600, 300));
        panel.add(about, GBC.std().fill());
        
        GuiHelper.prepareResizeableOptionPane(panel, panel.getPreferredSize());
        JOptionPane.showMessageDialog(Main.parent, panel, tr("About JOSM..."),
                JOptionPane.INFORMATION_MESSAGE, ImageProvider.get("logo"));
    }

    private JScrollPane createScrollPane(JTextArea area) {
        area.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
        area.setOpaque(false);
        JScrollPane sp = new JScrollPane(area);
        sp.setBorder(null);
        sp.setOpaque(false);
        return sp;
    }
}
