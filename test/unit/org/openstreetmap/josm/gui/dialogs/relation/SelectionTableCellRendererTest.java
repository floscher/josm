// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.relation;

import static org.junit.Assert.assertEquals;

import javax.swing.JTable;

import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unit tests of {@link SelectionTableCellRenderer} class.
 */
public class SelectionTableCellRendererTest {

    /**
     * Setup test.
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules().preferences();

    /**
     * Unit test of {@link SelectionTableCellRenderer#SelectionTableCellRenderer}.
     */
    @Test
    public void testSelectionTableCellRenderer() {
        MemberTableModel model = new MemberTableModel(null, null, null);
        SelectionTableCellRenderer r = new SelectionTableCellRenderer(model);
        assertEquals(r, r.getTableCellRendererComponent(null, null, false, false, 0, 0));
        assertEquals(r, r.getTableCellRendererComponent(new JTable(model), new Node(), false, false, 0, 0));
    }
}
