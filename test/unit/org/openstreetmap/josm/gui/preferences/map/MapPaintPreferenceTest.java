// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.preferences.map;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import org.junit.BeforeClass;
import org.junit.Test;
import org.openstreetmap.josm.JOSMFixture;
import org.openstreetmap.josm.gui.mappaint.MapPaintStyles;
import org.openstreetmap.josm.gui.mappaint.mapcss.parsergen.ParseException;
import org.openstreetmap.josm.gui.preferences.SourceEditor.ExtendedSourceEntry;

/**
 * Unit tests of {@link MapPaintPreference} class.
 */
public class MapPaintPreferenceTest {

    /**
     * Setup test.
     */
    @BeforeClass
    public static void setUpBeforeClass() {
        JOSMFixture.createUnitTestFixture().init();
    }

    /**
     * Test that available map paint styles are valid.
     * @throws IOException if any I/O error occurs
     * @throws ParseException if the config file does not match MapCSS syntax
     */
    @Test
    public void testValidityOfAvailableStyles() throws ParseException, IOException {
        Collection<ExtendedSourceEntry> sources = new MapPaintPreference.MapPaintSourceEditor()
                .loadAndGetAvailableSources();
        assertFalse(sources.isEmpty());
        Collection<Throwable> allErrors = new ArrayList<>();
        for (ExtendedSourceEntry source : sources) {
            System.out.print(source.url);
            Collection<Throwable> errors = MapPaintStyles.addStyle(source);
            System.out.println(errors.isEmpty() ? " => OK" : " => KO");
            allErrors.addAll(errors);
        }
        assertTrue(allErrors.isEmpty());
    }
}
