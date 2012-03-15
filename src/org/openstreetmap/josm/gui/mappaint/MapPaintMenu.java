package org.openstreetmap.josm.gui.mappaint;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.util.HashMap;
import java.util.Map;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.JosmAction;
import org.openstreetmap.josm.gui.dialogs.MapPaintDialog;
import org.openstreetmap.josm.gui.dialogs.MapPaintDialog.LaunchMapPaintPreferencesAction;
import org.openstreetmap.josm.gui.mappaint.MapPaintStyles.MapPaintSylesUpdateListener;
import org.openstreetmap.josm.gui.util.StayOpenCheckBoxMenuItem;
import org.openstreetmap.josm.tools.ImageProvider;

public class MapPaintMenu extends JMenu implements MapPaintSylesUpdateListener {

    private static class MapPaintAction extends JosmAction {

        private StyleSource style;
        private JCheckBoxMenuItem button;

        public MapPaintAction(StyleSource style) {
            super(style.getDisplayString(), style.icon,
                    tr("Select the map painting styles"), null, style.icon != null);
            if (style.icon == null) {
                putValue("toolbar", "mappaint/" + style.getDisplayString());
                Main.toolbar.register(this);
            }
            this.button = new StayOpenCheckBoxMenuItem(this);
            this.style = style;
            updateButton();
        }

        private void updateButton() {
            button.getModel().setSelected(style.active);
        }

        private void toggleStyle() {
            MapPaintStyles.toggleStyleActive(MapPaintStyles.getStyles().getStyleSources().indexOf(style));
            updateButton();
        }

        @Override
        public void actionPerformed(ActionEvent ae) {
            toggleStyle();
        }

        public JCheckBoxMenuItem getButton() {
            return button;
        }

        @Override
        public void updateEnabledState() {
            setEnabled(Main.map != null && Main.main.getEditLayer() != null);
        }
    }
    private final Map<String, MapPaintAction> actions = new HashMap<String, MapPaintAction>();
    private final LaunchMapPaintPreferencesAction mapPaintPreferencesAction = new MapPaintDialog.LaunchMapPaintPreferencesAction() {

        {
            putValue("toolbar", "mappaintpreference");
        }
    };

    public MapPaintMenu() {
        super(tr("Map Paint Styles"));
        setIcon(ImageProvider.get("dialogs", "mapstyle"));
        MapPaintStyles.addMapPaintSylesUpdateListener(this);
    }

    @Override
    public void mapPaintStylesUpdated() {
        removeAll();
        for (StyleSource style : MapPaintStyles.getStyles().getStyleSources()) {
            final String k = style.getDisplayString();
            MapPaintAction a = actions.get(k);
            if (a == null) {
                actions.put(k, a = new MapPaintAction(style));
                add(a.getButton());
            } else {
                add(a.getButton());
                a.updateButton();
            }
        }
        addSeparator();
        add(mapPaintPreferencesAction);
    }

    @Override
    public void mapPaintStyleEntryUpdated(int idx) {
        mapPaintStylesUpdated();
    }
}
