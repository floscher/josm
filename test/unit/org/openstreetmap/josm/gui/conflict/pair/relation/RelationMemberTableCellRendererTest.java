// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.conflict.pair.relation;

import static org.junit.Assert.assertEquals;

import javax.swing.JTable;

import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.gui.conflict.pair.ListRole;
import org.openstreetmap.josm.gui.conflict.pair.nodes.NodeListMergeModel;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unit tests of {@link RelationMemberTableCellRenderer} class.
 */
public class RelationMemberTableCellRendererTest {

    /**
     * Setup test.
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules().preferences();

    /**
     * Unit test of {@link RelationMemberTableCellRenderer#RelationMemberTableCellRenderer}.
     */
    @Test
    public void testRelationMemberTableCellRenderer() {
        JTable table = new JTable(new NodeListMergeModel().new EntriesTableModel(ListRole.MY_ENTRIES));
        RelationMember member = new RelationMember("foo", new Node());
        RelationMemberTableCellRenderer r = new RelationMemberTableCellRenderer();
        assertEquals(r, r.getTableCellRendererComponent(table, member, false, false, 0, 0));
        assertEquals(r, r.getTableCellRendererComponent(table, member, false, false, 0, 1));
        assertEquals(r, r.getTableCellRendererComponent(table, member, false, false, 0, 2));
        assertEquals(r, r.getTableCellRendererComponent(table, member, false, false, 0, 3));
    }
}
