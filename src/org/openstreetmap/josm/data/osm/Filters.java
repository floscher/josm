package org.openstreetmap.josm.data.osm;

import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trc;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.swing.table.AbstractTableModel;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.search.SearchAction;
import org.openstreetmap.josm.actions.search.SearchAction.Function;

/**
 *
 * @author Petr_Dlouhý
 */
public class Filters extends AbstractTableModel{

    public int disabledCount, hiddenCount;

    public Filters(){
        loadPrefs();
    }

    private List<Filter> filters = new LinkedList<Filter>();
    public void filter(){
        Collection<OsmPrimitive> seld = new LinkedList<OsmPrimitive> ();
        Collection<OsmPrimitive> self = new LinkedList<OsmPrimitive> ();
        if(Main.main.getCurrentDataSet() == null)return;
        Main.main.getCurrentDataSet().setFiltered();
        Main.main.getCurrentDataSet().setDisabled();
        for (Filter flt : filters){
            if(flt.enable){
                SearchAction.getSelection(flt, seld, new Function(){
                    public Boolean isSomething(OsmPrimitive o){
                        return o.isDisabled();
                    }
                });
                if(flt.hide) {
                    SearchAction.getSelection(flt, self, new Function(){
                        public Boolean isSomething(OsmPrimitive o){
                            return o.isFiltered();
                        }
                    });
                }
            }
        }
        disabledCount = seld.size() - self.size();
        hiddenCount = self.size();
        Main.main.getCurrentDataSet().setFiltered(self);
        Main.main.getCurrentDataSet().setDisabled(seld);
        Main.map.mapView.repaint();
    }

    private void loadPrefs(){
        Map<String,String> prefs = Main.pref.getAllPrefix("filters.filter");
        for (String value : prefs.values()) {
            filters.add(new Filter(value));
        }
    }

    private void savePrefs(){
        Map<String,String> prefs = Main.pref.getAllPrefix("filters.filter");
        for (String key : prefs.keySet()) {
            String[] sts = key.split("\\.");
            if (sts.length != 3)throw new Error("Incompatible filter preferences");
            Main.pref.put("filters.filter." + sts[2], null);
        }

        int i = 0;
        for (Filter flt : filters){
            Main.pref.put("filters.filter." + i++, flt.getPrefString());
        }
    }

    private void savePref(int i){
        if(i >= filters.size()) {
            Main.pref.put("filters.filter." + i, null);
        } else {
            Main.pref.put("filters.filter." + i, filters.get(i).getPrefString());
        }
    }

    public void addFilter(Filter f){
        filters.add(f);
        savePref(filters.size()-1);
        filter();
        fireTableRowsInserted(filters.size()-1, filters.size()-1);
    }

    public void moveDownFilter(int i){
        if(i >= filters.size()-1) return;
        filters.add(i+1, filters.remove(i));
        savePref(i);
        savePref(i+1);
        filter();
        fireTableRowsUpdated(i, i+1);
    }

    public void moveUpFilter(int i){
        if(i == 0) return;
        filters.add(i-1, filters.remove(i));
        savePref(i);
        savePref(i-1);
        filter();
        fireTableRowsUpdated(i-1, i);
    }

    public void removeFilter(int i){
        filters.remove(i);
        savePrefs();
        filter();
        fireTableRowsDeleted(i, i);
    }

    public void setFilter(int i, Filter f){
        filters.set(i, f);
        savePref(i);
        filter();
        fireTableRowsUpdated(i, i);
    }

    public Filter getFilter(int i){
        return filters.get(i);
    }

    public int getRowCount(){
        return filters.size();
    }

    public int getColumnCount(){
        return 6;
    }

    @Override
    public String getColumnName(int column){
        String[] names = { /* translators notes must be in front */
                /* column header: enable filter */             trc("enable filter","E"),
                /* column header: hide filter */               trc("hide filter", "H"),
                /* column header: filter text */               trc("filter", "Text"),
                /* column header: apply filter for children */ trc("filter children", "C"),
                /* column header: inverted filter */           trc("invert filter", "I"),
                /* column header: filter mode */               trc("filter mode", "M")
        };
        return names[column];
    }

    @Override
    public Class<?> getColumnClass(int column){
        Class<?>[] classes = { Boolean.class, Boolean.class, String.class, Boolean.class, Boolean.class, String.class };
        return classes[column];
    }

    public boolean isCellEnabled(int row, int column){
        if(!filters.get(row).enable && column!=0) return false;
        return true;
    }

    @Override
    public boolean isCellEditable(int row, int column){
        if(!filters.get(row).enable && column!=0) return false;
        if(column < 5)return true;
        return false;
    }

    @Override
    public void setValueAt(Object aValue, int row, int column){
        Filter f = filters.get(row);
        switch(column){
        case 0: f.enable = (Boolean)aValue;
        savePref(row);
        filter();
        fireTableRowsUpdated(row, row);
        break;
        case 1: f.hide = (Boolean)aValue;
        savePref(row);
        filter();
        break;
        case 2: f.text = (String)aValue;
        savePref(row);
        break;
        case 3: f.applyForChildren = (Boolean)aValue;
        savePref(row);
        filter();
        break;
        case 4: f.inverted = (Boolean)aValue;
        savePref(row);
        filter();
        break;
        }
        if(column!=0) {
            fireTableCellUpdated(row, column);
        }
    }

    public Object getValueAt(int row, int column){
        Filter f = filters.get(row);
        switch(column){
        case 0: return f.enable;
        case 1: return f.hide;
        case 2: return f.text;
        case 3: return f.applyForChildren;
        case 4: return f.inverted;
        case 5:
            switch(f.mode){ /* translators notes must be in front */
            case replace:      /* filter mode: replace */      return trc("filter mode replace", "R");
            case add:          /* filter mode: add */          return trc("filter mode add", "A");
            case remove:       /* filter mode: remove */       return trc("filter mode remove", "D");
            case in_selection: /* filter mode: in selection */ return trc("filter mode in selection", "F");
            }
        }
        return null;
    }
}
