// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.io;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.io.File;

import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.gui.layer.AbstractModifiableLayer;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unit tests of {@link SaveLayerInfo} class.
 */
public class SaveLayerInfoTest {
    /**
     * Setup test.
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules();

    /**
     * Test of {@link SaveLayerInfo} class - null case.
     */
    @Test(expected = IllegalArgumentException.class)
    @SuppressFBWarnings(value = "NP_NULL_PARAM_DEREF_NONVIRTUAL")
    public void testSaveLayerInfoNull() {
        new SaveLayerInfo(null);
    }

    /**
     * Test of {@link SaveLayerInfo} class - nominal case.
     */
    @Test
    public void testSaveLayerInfoNominal() {
        File file = new File("test");
        String name = "layername";
        AbstractModifiableLayer layer = new OsmDataLayer(new DataSet(), name, file);
        SaveLayerInfo sli = new SaveLayerInfo(layer);
        assertEquals(file, sli.getFile());
        assertEquals(layer, sli.getLayer());
        assertEquals(name, sli.getName());
        assertNull(sli.getSaveState());
        assertNull(sli.getUploadState());
    }
}
