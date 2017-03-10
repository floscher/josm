// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions.mapmode;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Collections;

import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.notes.Note;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.NoteData;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unit tests for class {@link AddNoteAction}.
 */
public class AddNoteActionTest {

    /**
     * Setup test.
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules().commands();

    /**
     * Unit test of {@link AddNoteAction#enterMode} and {@link AddNoteAction#exitMode}.
     */
    @Test
    public void testMode() {
        OsmDataLayer layer = new OsmDataLayer(new DataSet(), "", null);
        try {
            Main.getLayerManager().addLayer(layer);
            AddNoteAction mapMode = new AddNoteAction(new NoteData(Collections.<Note>emptyList()));
            MapMode oldMapMode = Main.map.mapMode;
            assertTrue(Main.map.selectMapMode(mapMode));
            assertEquals(mapMode, Main.map.mapMode);
            assertTrue(Main.map.selectMapMode(oldMapMode));
        } finally {
            Main.getLayerManager().removeLayer(layer);
        }
    }
}
