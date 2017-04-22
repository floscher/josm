// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm;

import java.util.Objects;

import org.openstreetmap.josm.actions.search.SearchAction.SearchMode;
import org.openstreetmap.josm.actions.search.SearchAction.SearchSetting;
import org.openstreetmap.josm.data.Preferences.pref;
import org.openstreetmap.josm.data.Preferences.writeExplicitly;

/**
 * Data class representing one entry in the filter dialog.
 *
 * @author Petr_Dlouhý
 * @since 2125
 */
public class Filter extends SearchSetting {
    private static final String version = "1";

    /**
     * Enabled status.
     * @see FilterPreferenceEntry#enable
     */
    public boolean enable = true;

    /**
     * If this option is activated, the chosen objects are completely hidden.
     * Otherwise they are disabled and shown in a shade of gray.
     * @see FilterPreferenceEntry#hiding
     */
    public boolean hiding;

    /**
     * Normally, the specified objects are hidden and the rest is shown.
     * If this option is activated, only the specified objects are shown and the rest is hidden.
     * @see FilterPreferenceEntry#inverted
     */
    public boolean inverted;

    /**
     * Constructs a new {@code Filter}.
     */
    public Filter() {
        super();
        mode = SearchMode.add;
    }

    /**
     * Constructs a new {@code Filter} from a preference entry.
     * @param e preference entry
     */
    public Filter(FilterPreferenceEntry e) {
        this();
        text = e.text;
        if ("replace".equals(e.mode)) {
            mode = SearchMode.replace;
        } else if ("add".equals(e.mode)) {
            mode = SearchMode.add;
        } else if ("remove".equals(e.mode)) {
            mode = SearchMode.remove;
        } else if ("in_selection".equals(e.mode)) {
            mode = SearchMode.in_selection;
        }
        caseSensitive = e.case_sensitive;
        regexSearch = e.regex_search;
        mapCSSSearch = e.mapCSS_search;
        enable = e.enable;
        hiding = e.hiding;
        inverted = e.inverted;
    }

    public static class FilterPreferenceEntry {
        @writeExplicitly
        @pref public String version = "1";

        @pref public String text;

        /**
         * Mode selector which defines how a filter is combined with the previous one:<ul>
         * <li>replace: replace selection</li>
         * <li>add: add to selection</li>
         * <li>remove: remove from selection</li>
         * <li>in_selection: find in selection</li>
         * </ul>
         * @see SearchMode
         */
        @writeExplicitly
        @pref public String mode = "add";

        @pref public boolean case_sensitive;

        @pref public boolean regex_search;

        @pref public boolean mapCSS_search;

        /**
         * Enabled status.
         * @see Filter#enable
         */
        @writeExplicitly
        @pref public boolean enable = true;

        /**
         * If this option is activated, the chosen objects are completely hidden.
         * Otherwise they are disabled and shown in a shade of gray.
         * @see Filter#hiding
         */
        @writeExplicitly
        @pref public boolean hiding;

        /**
         * Normally, the specified objects are hidden and the rest is shown.
         * If this option is activated, only the specified objects are shown and the rest is hidden.
         * @see Filter#inverted
         */
        @writeExplicitly
        @pref public boolean inverted;

        @Override
        public int hashCode() {
            return Objects.hash(case_sensitive, enable, hiding, inverted, mapCSS_search, mode, regex_search, text, version);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null || getClass() != obj.getClass())
                return false;
            FilterPreferenceEntry other = (FilterPreferenceEntry) obj;
            return case_sensitive == other.case_sensitive
                    && enable == other.enable
                    && hiding == other.hiding
                    && inverted == other.inverted
                    && mapCSS_search == other.mapCSS_search
                    && regex_search == other.regex_search
                    && Objects.equals(mode, other.mode)
                    && Objects.equals(text, other.text)
                    && Objects.equals(version, other.version);
        }
    }

    /**
     * Returns a new preference entry for this filter.
     * @return preference entry
     */
    public FilterPreferenceEntry getPreferenceEntry() {
        FilterPreferenceEntry e = new FilterPreferenceEntry();
        e.version = version;
        e.text = text;
        e.mode = mode.toString();
        e.case_sensitive = caseSensitive;
        e.regex_search = regexSearch;
        e.mapCSS_search = mapCSSSearch;
        e.enable = enable;
        e.hiding = hiding;
        e.inverted = inverted;
        return e;
    }
}
