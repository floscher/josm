// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.gui.preferences.projection;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Component;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collection;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.coor.CoordinateFormat;
import org.openstreetmap.josm.data.preferences.CollectionProperty;
import org.openstreetmap.josm.data.preferences.ParametrizedCollectionProperty;
import org.openstreetmap.josm.data.preferences.StringProperty;
import org.openstreetmap.josm.data.projection.Mercator;
import org.openstreetmap.josm.data.projection.Projection;
import org.openstreetmap.josm.data.projection.ProjectionSubPrefs;
import org.openstreetmap.josm.data.projection.Projections;
import org.openstreetmap.josm.gui.NavigatableComponent;
import org.openstreetmap.josm.gui.preferences.PreferenceSetting;
import org.openstreetmap.josm.gui.preferences.PreferenceSettingFactory;
import org.openstreetmap.josm.gui.preferences.PreferenceTabbedPane;
import org.openstreetmap.josm.gui.preferences.SubPreferenceSetting;
import org.openstreetmap.josm.gui.preferences.TabPreferenceSetting;
import org.openstreetmap.josm.plugins.PluginHandler;
import org.openstreetmap.josm.tools.GBC;

public class ProjectionPreference implements SubPreferenceSetting {

    public static class Factory implements PreferenceSettingFactory {
        public PreferenceSetting createPreferenceSetting() {
            return new ProjectionPreference();
        }
    }

    private static final StringProperty PROP_PROJECTION = new StringProperty("projection", Mercator.class.getName());
    private static final StringProperty PROP_COORDINATES = new StringProperty("coordinates", null);
    private static final CollectionProperty PROP_SUB_PROJECTION = new CollectionProperty("projection.sub", null);
    private static final ParametrizedCollectionProperty PROP_PROJECTION_SUBPROJECTION = new ParametrizedCollectionProperty(null) {
        @Override
        protected String getKey(String... params) {
            String name = params[0];
            String sname = name.substring(name.lastIndexOf(".")+1);
            return "projection.sub."+sname;
        }
    };
    public static final StringProperty PROP_SYSTEM_OF_MEASUREMENT = new StringProperty("system_of_measurement", "Metric");
    private static final String[] unitsValues = (new ArrayList<String>(NavigatableComponent.SYSTEMS_OF_MEASUREMENT.keySet())).toArray(new String[0]);
    private static final String[] unitsValuesTr = new String[unitsValues.length];
    static {
        for (int i=0; i<unitsValues.length; ++i) {
            unitsValuesTr[i] = tr(unitsValues[i]);
        }
    }

    /**
     * Combobox with all projections available
     */
    private JComboBox projectionCombo = new JComboBox(Projections.getProjections().toArray());

    /**
     * Combobox with all coordinate display possibilities
     */
    private JComboBox coordinatesCombo = new JComboBox(CoordinateFormat.values());

    private JComboBox unitsCombo = new JComboBox(unitsValuesTr);

    /**
     * This variable holds the JPanel with the projection's preferences. If the
     * selected projection does not implement this, it will be set to an empty
     * Panel.
     */
    private JPanel projSubPrefPanel;
    private JPanel projSubPrefPanelWrapper = new JPanel(new GridBagLayout());

    private JLabel projectionCodeLabel;
    private Component projectionCodeGlue;
    private JLabel projectionCode = new JLabel();
    private JLabel bounds = new JLabel();

    /**
     * This is the panel holding all projection preferences
     */
    private JPanel projPanel = new JPanel(new GridBagLayout());

    /**
     * The GridBagConstraints for the Panel containing the ProjectionSubPrefs.
     * This is required twice in the code, creating it here keeps both occurrences
     * in sync
     */
    static private GBC projSubPrefPanelGBC = GBC.std().fill(GBC.BOTH).weight(1.0, 1.0);

    public void addGui(PreferenceTabbedPane gui) {
        setupProjectionCombo();

        for (int i = 0; i < coordinatesCombo.getItemCount(); ++i) {
            if (((CoordinateFormat)coordinatesCombo.getItemAt(i)).name().equals(PROP_COORDINATES.get())) {
                coordinatesCombo.setSelectedIndex(i);
                break;
            }
        }

        for (int i = 0; i < unitsValues.length; ++i) {
            if (unitsValues[i].equals(PROP_SYSTEM_OF_MEASUREMENT.get())) {
                unitsCombo.setSelectedIndex(i);
                break;
            }
        }

        projPanel.setBorder(BorderFactory.createEmptyBorder( 0, 0, 0, 0 ));
        projPanel.setLayout(new GridBagLayout());
        projPanel.add(new JLabel(tr("Projection method")), GBC.std().insets(5,5,0,5));
        projPanel.add(GBC.glue(5,0), GBC.std().fill(GBC.HORIZONTAL));
        projPanel.add(projectionCombo, GBC.eop().fill(GBC.HORIZONTAL).insets(0,5,5,5));
        projPanel.add(projectionCodeLabel = new JLabel(tr("Projection code")), GBC.std().insets(25,5,0,5));
        projPanel.add(projectionCodeGlue = GBC.glue(5,0), GBC.std().fill(GBC.HORIZONTAL));
        projPanel.add(projectionCode, GBC.eop().fill(GBC.HORIZONTAL).insets(0,5,5,5));
        projPanel.add(new JLabel(tr("Bounds")), GBC.std().insets(25,5,0,5));
        projPanel.add(GBC.glue(5,0), GBC.std().fill(GBC.HORIZONTAL));
        projPanel.add(bounds, GBC.eop().fill(GBC.HORIZONTAL).insets(0,5,5,5));
        projSubPrefPanelWrapper.add(projSubPrefPanel, projSubPrefPanelGBC);
        projPanel.add(projSubPrefPanelWrapper, GBC.eol().fill(GBC.HORIZONTAL).insets(20,5,5,5));

        projPanel.add(new JSeparator(), GBC.eol().fill(GBC.HORIZONTAL).insets(0,5,0,10));
        projPanel.add(new JLabel(tr("Display coordinates as")), GBC.std().insets(5,5,0,5));
        projPanel.add(GBC.glue(5,0), GBC.std().fill(GBC.HORIZONTAL));
        projPanel.add(coordinatesCombo, GBC.eop().fill(GBC.HORIZONTAL).insets(0,5,5,5));
        projPanel.add(new JLabel(tr("System of measurement")), GBC.std().insets(5,5,0,5));
        projPanel.add(GBC.glue(5,0), GBC.std().fill(GBC.HORIZONTAL));
        projPanel.add(unitsCombo, GBC.eop().fill(GBC.HORIZONTAL).insets(0,5,5,5));
        projPanel.add(GBC.glue(1,1), GBC.std().fill(GBC.HORIZONTAL).weight(1.0, 1.0));

        JScrollPane scrollpane = new JScrollPane(projPanel);
        gui.getMapPreference().mapcontent.addTab(tr("Map Projection"), scrollpane);

        updateMeta(Main.getProjection());
    }

    private void updateMeta(Projection proj)
    {
        projectionCode.setText(proj.toCode());
        Bounds b = proj.getWorldBoundsLatLon();
        CoordinateFormat cf = CoordinateFormat.getDefaultFormat();
        bounds.setText(b.getMin().latToString(cf)+"; "+b.getMin().lonToString(cf)+" : "+b.getMax().latToString(cf)+"; "+b.getMax().lonToString(cf));
        boolean hideCode = proj instanceof SubPrefsOptions && !((SubPrefsOptions) proj).showProjectionCode();
        projectionCodeLabel.setVisible(!hideCode);
        projectionCodeGlue.setVisible(!hideCode);
        projectionCode.setVisible(!hideCode);
    }

    public boolean ok() {
        Projection proj = (Projection) projectionCombo.getSelectedItem();

        String projname = proj.getClass().getName();
        Collection<String> prefs = null;
        if(proj instanceof ProjectionSubPrefs) {
            prefs = ((ProjectionSubPrefs) proj).getPreferences(projSubPrefPanel);
        }

        PROP_PROJECTION.put(projname);
        setProjection(projname, prefs);

        if(PROP_COORDINATES.put(((CoordinateFormat)coordinatesCombo.getSelectedItem()).name())) {
            CoordinateFormat.setCoordinateFormat((CoordinateFormat)coordinatesCombo.getSelectedItem());
        }

        int i = unitsCombo.getSelectedIndex();
        PROP_SYSTEM_OF_MEASUREMENT.put(unitsValues[i]);

        return false;
    }

    static public void setProjection()
    {
        setProjection(PROP_PROJECTION.get(), PROP_SUB_PROJECTION.get());
    }

    static public void setProjection(String name, Collection<String> coll)
    {
        Bounds b = (Main.map != null && Main.map.mapView != null) ? Main.map.mapView.getRealBounds() : null;

        Projection proj = null;
        for (ClassLoader cl : PluginHandler.getResourceClassLoaders()) {
            try {
                proj = (Projection) Class.forName(name, true, cl).newInstance();
            } catch (final Exception e) {
            }
            if (proj != null) {
                break;
            }
        }
        if (proj == null) {
            JOptionPane.showMessageDialog(
                    Main.parent,
                    tr("The projection {0} could not be activated. Using Mercator", name),
                    tr("Error"),
                    JOptionPane.ERROR_MESSAGE
            );
            coll = null;
            proj = new Mercator();
            name = proj.getClass().getName();
            PROP_PROJECTION.put(name);
        }
        PROP_SUB_PROJECTION.put(coll);
        PROP_PROJECTION_SUBPROJECTION.put(coll, name);
        if (proj instanceof ProjectionSubPrefs) {
            ((ProjectionSubPrefs) proj).setPreferences(coll);
        }
        Projection oldProj = Main.getProjection();
        Main.setProjection(proj);
        if (b != null && (!proj.getClass().getName().equals(oldProj.getClass().getName()) || proj.hashCode() != oldProj.hashCode()))
        {
            Main.map.mapView.zoomTo(b);
            /* TODO - remove layers with fixed projection */
        }
    }

    private class SBPanel extends JPanel implements ActionListener
    {
        private ProjectionSubPrefs p;
        public SBPanel(ProjectionSubPrefs pr)
        {
            super();
            p = pr;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            p.setPreferences(p.getPreferences(this));
            updateMeta(p);
        }
    }

    /**
     * Handles all the work related to update the projection-specific
     * preferences
     * @param proj
     */
    private void selectedProjectionChanged(Projection proj) {
        if(!(proj instanceof ProjectionSubPrefs)) {
            projSubPrefPanel = new JPanel();
        } else {
            ProjectionSubPrefs projPref = (ProjectionSubPrefs) proj;
            projSubPrefPanel = new SBPanel(projPref);
            projPref.setupPreferencePanel(projSubPrefPanel, (SBPanel)projSubPrefPanel);
        }

        // Don't try to update if we're still starting up
        int size = projPanel.getComponentCount();
        if(size < 1)
            return;

        // Replace old panel with new one
        projSubPrefPanelWrapper.removeAll();
        projSubPrefPanelWrapper.add(projSubPrefPanel, projSubPrefPanelGBC);
        projPanel.revalidate();
        projSubPrefPanel.repaint();
        updateMeta(proj);
    }

    /**
     * Sets up projection combobox with default values and action listener
     */
    private void setupProjectionCombo() {
        boolean found = false;
        for (int i = 0; i < projectionCombo.getItemCount(); ++i) {
            Projection proj = (Projection)projectionCombo.getItemAt(i);
            String name = proj.getClass().getName();
            if(proj instanceof ProjectionSubPrefs) {
                ((ProjectionSubPrefs) proj).setPreferences(PROP_PROJECTION_SUBPROJECTION.get(name));
            }
            if (name.equals(PROP_PROJECTION.get())) {
                projectionCombo.setSelectedIndex(i);
                selectedProjectionChanged(proj);
                found = true;
                break;
            }
        }
        if (!found)
            throw new RuntimeException("Couldn't find the current projection in the list of available projections!");

        projectionCombo.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                JComboBox cb = (JComboBox)e.getSource();
                Projection proj = (Projection)cb.getSelectedItem();
                selectedProjectionChanged(proj);
            }
        });
    }

    @Override
    public boolean isExpert() {
        return false;
    }

    @Override
    public TabPreferenceSetting getTabPreferenceSetting(final PreferenceTabbedPane gui) {
        return gui.getMapPreference();
    }
}
