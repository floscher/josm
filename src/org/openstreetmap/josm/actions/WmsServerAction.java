package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.JOptionPane;

import org.openstreetmap.josm.Main;

public class WmsServerAction extends JosmAction {

    public WmsServerAction() {
        super(tr("Show background"), "wmsserver", tr("Download and show landsat background images."), KeyEvent.VK_B);
    }

    public void actionPerformed(ActionEvent e) {
    	JOptionPane.showMessageDialog(Main.parent, tr("Not implemented yet."));
    }
}
