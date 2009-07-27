// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.corrector;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.OsmUtils;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.Way;

public class ReverseWayTagCorrector extends TagCorrector<Way> {

    private static class PrefixSuffixSwitcher {

        private final String a;
        private final String b;
        private final Pattern startPattern;
        private final Pattern endPattern;

        private final String SEPARATOR = "[:_]?";

        public PrefixSuffixSwitcher(String a, String b) {
            this.a = a;
            this.b = b;
            startPattern = Pattern.compile(
                    "^(" + a + "|" + b + ")(" + SEPARATOR + "|$)",
                    Pattern.CASE_INSENSITIVE);
            endPattern = Pattern.compile("^.*" +
                    SEPARATOR + "(" + a + "|" + b + ")$",
                    Pattern.CASE_INSENSITIVE);
        }

        public String apply(String text) {
            Matcher m = startPattern.matcher(text);
            if (!m.lookingAt()) {
                m = endPattern.matcher(text);
            }

            if (m.lookingAt()) {
                String leftRight = m.group(1).toLowerCase();

                StringBuilder result = new StringBuilder();
                result.append(text.substring(0, m.start(1)));
                result.append(leftRight.equals(a) ? b : a);
                result.append(text.substring(m.end(1)));

                return result.toString();
            }
            return text;
        }
    }

    private static PrefixSuffixSwitcher[] prefixSuffixSwitchers =
        new PrefixSuffixSwitcher[] {
        new PrefixSuffixSwitcher("left", "right"),
        new PrefixSuffixSwitcher("forward", "backward"),
        new PrefixSuffixSwitcher("forwards", "backwards")
    };

    @Override
    public Collection<Command> execute(Way oldway, Way way) throws UserCancelException {
        Map<OsmPrimitive, List<TagCorrection>> tagCorrectionsMap =
            new HashMap<OsmPrimitive, List<TagCorrection>>();

        ArrayList<OsmPrimitive> primitives = new ArrayList<OsmPrimitive>();
        primitives.add(way);
        primitives.addAll(way.getNodes());

        for (OsmPrimitive primitive : primitives) {
            tagCorrectionsMap.put(primitive, new ArrayList<TagCorrection>());

            for (String key : primitive.keySet()) {
                String newKey = key;
                String value = primitive.get(key);
                String newValue = value;

                if (key.equals("oneway")) {
                    if (value.equals("-1")) {
                        newValue = OsmUtils.trueval;
                    } else {
                        Boolean boolValue = OsmUtils.getOsmBoolean(value);
                        if (boolValue != null && boolValue.booleanValue()) {
                            newValue = "-1";
                        }
                    }
                } else {
                    for (PrefixSuffixSwitcher prefixSuffixSwitcher : prefixSuffixSwitchers) {
                        newKey = prefixSuffixSwitcher.apply(key);
                        if (!key.equals(newKey)) {
                            break;
                        }
                    }
                }

                if (!key.equals(newKey) || !value.equals(newValue)) {
                    tagCorrectionsMap.get(primitive).add(
                            new TagCorrection(key, value, newKey, newValue));
                }
            }
        }

        Map<OsmPrimitive, List<RoleCorrection>> roleCorrectionMap =
            new HashMap<OsmPrimitive, List<RoleCorrection>>();
        roleCorrectionMap.put(way, new ArrayList<RoleCorrection>());

        for (Relation relation : Main.main.getCurrentDataSet().relations) {
            int position = 0;
            for (RelationMember member : relation.members) {
                if (!member.member.hasEqualSemanticAttributes(oldway)
                        || member.role.length() == 0) {
                    position++;
                    continue;
                }

                boolean found = false;
                String newRole = null;
                for (PrefixSuffixSwitcher prefixSuffixSwitcher : prefixSuffixSwitchers) {
                    newRole = prefixSuffixSwitcher.apply(member.role);
                    if (!newRole.equals(member.role)) {
                        found = true;
                        break;
                    }
                }

                if (found) {
                    roleCorrectionMap.get(way).add(
                            new RoleCorrection(relation, position, member, newRole));
                }

                position++;
            }
        }

        return applyCorrections(tagCorrectionsMap, roleCorrectionMap,
                tr("When reversing this way, the following changes to properties "
                        + "of the way and its nodes are suggested in order "
                        + "to maintain data consistency."));
    }
}
