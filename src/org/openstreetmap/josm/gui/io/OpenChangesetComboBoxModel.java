// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.io;

import java.util.ArrayList;
import java.util.List;

import javax.swing.DefaultComboBoxModel;

import org.openstreetmap.josm.data.osm.Changeset;
import org.openstreetmap.josm.data.osm.ChangesetCache;
import org.openstreetmap.josm.data.osm.ChangesetCacheEvent;
import org.openstreetmap.josm.data.osm.ChangesetCacheListener;

/**
 * A combobox model for the list of open changesets
 *
 */
public class OpenChangesetComboBoxModel extends DefaultComboBoxModel implements ChangesetCacheListener {
    private List<Changeset> changesets;
    private long uid;
    private Changeset selectedChangeset = null;

    protected Changeset getChangesetById(long id) {
        for (Changeset cs : changesets) {
            if (cs.getId() == id) return cs;
        }
        return null;
    }

    public OpenChangesetComboBoxModel() {
        this.changesets = new ArrayList<Changeset>();
    }

    public void refresh() {
        changesets.clear();
        changesets.addAll(ChangesetCache.getInstance().getOpenChangesets());
        fireContentsChanged(this, 0, getSize());
        int idx = changesets.indexOf(selectedChangeset);
        if (idx < 0) {
            setSelectedItem(null);
        } else {
            setSelectedItem(changesets.get(idx));
        }
    }

    public void setUserId(long uid) {
        this.uid = uid;
    }

    public long getUserId() {
        return uid;
    }

    public void selectFirstChangeset() {
        if (changesets == null || changesets.isEmpty()) {
            setSelectedItem(null);
        } else {
            setSelectedItem(changesets.get(0));
        }
    }

    /* ------------------------------------------------------------------------------------ */
    /* ChangesetCacheListener                                                               */
    /* ------------------------------------------------------------------------------------ */
    public void changesetCacheUpdated(ChangesetCacheEvent event) {
        refresh();
    }

    /* ------------------------------------------------------------------------------------ */
    /* ComboBoxModel                                                                        */
    /* ------------------------------------------------------------------------------------ */
    @Override
    public Object getElementAt(int index) {
        return changesets.get(index);
    }

    @Override
    public int getIndexOf(Object anObject) {
        return changesets.indexOf(anObject);
    }

    @Override
    public int getSize() {
        return changesets.size();
    }

    @Override
    public Object getSelectedItem() {
        return selectedChangeset;
    }

    @Override
    public void setSelectedItem(Object anObject) {
        if (anObject == null) {
            this.selectedChangeset = null;
            super.setSelectedItem(null);
            return;
        }
        if (! (anObject instanceof Changeset)) return;
        Changeset cs = (Changeset)anObject;
        if (cs.getId() == 0 || ! cs.isOpen()) return;
        Changeset candidate = getChangesetById(cs.getId());
        if (candidate == null) return;
        this.selectedChangeset = candidate;
        super.setSelectedItem(selectedChangeset);
    }
}
