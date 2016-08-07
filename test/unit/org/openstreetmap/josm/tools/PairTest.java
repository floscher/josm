// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools;

import org.junit.Test;

import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;

/**
 * Unit tests of {@link Pair} class.
 */
public class PairTest {

    /**
     * Unit test of methods {@link Pair#equals} and {@link Pair#hashCode}.
     */
    @Test
    public void testEqualsContract() {
        EqualsVerifier.forClass(Pair.class).suppress(Warning.NONFINAL_FIELDS).verify();
    }
}
