// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.validation.tests;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.validation.Severity;
import org.openstreetmap.josm.data.validation.Test;
import org.openstreetmap.josm.data.validation.TestError;
import org.openstreetmap.josm.tools.LanguageInfo;
import org.openstreetmap.josm.tools.SubclassFilteredCollection;

/**
 * Checks for <a href="http://wiki.openstreetmap.org/wiki/Conditional_restrictions">conditional restrictions</a>
 * @since 6605
 */
public class ConditionalKeys extends Test.TagTest {

    private final OpeningHourTest openingHourTest = new OpeningHourTest();
    private static final Set<String> RESTRICTION_TYPES = new HashSet<>(Arrays.asList("oneway", "toll", "noexit", "maxspeed", "minspeed",
            "maxstay", "maxweight", "maxaxleload", "maxheight", "maxwidth", "maxlength", "overtaking", "maxgcweight", "maxgcweightrating",
            "fee"));
    private static final Set<String> RESTRICTION_VALUES = new HashSet<>(Arrays.asList("yes", "official", "designated", "destination",
            "delivery", "permissive", "private", "agricultural", "forestry", "no"));
    private static final Set<String> TRANSPORT_MODES = new HashSet<>(Arrays.asList("access", "foot", "ski", "inline_skates", "ice_skates",
            "horse", "vehicle", "bicycle", "carriage", "trailer", "caravan", "motor_vehicle", "motorcycle", "moped", "mofa",
            "motorcar", "motorhome", "psv", "bus", "taxi", "tourist_bus", "goods", "hgv", "agricultural", "atv", "snowmobile"
            /*,"hov","emergency","hazmat","disabled"*/));

    /**
     * Constructs a new {@code ConditionalKeys}.
     */
    public ConditionalKeys() {
        super(tr("Conditional Keys"), tr("Tests for the correct usage of ''*:conditional'' tags."));
    }

    @Override
    public void initialize() throws Exception {
        super.initialize();
        openingHourTest.initialize();
    }

    public static boolean isRestrictionType(String part) {
        return RESTRICTION_TYPES.contains(part);
    }

    public static boolean isRestrictionValue(String part) {
        return RESTRICTION_VALUES.contains(part);
    }

    public static boolean isTransportationMode(String part) {
        // http://wiki.openstreetmap.org/wiki/Key:access#Transport_mode_restrictions
        return TRANSPORT_MODES.contains(part);
    }

    public static boolean isDirection(String part) {
        return "forward".equals(part) || "backward".equals(part);
    }

    public boolean isKeyValid(String key) {
        // <restriction-type>[:<transportation mode>][:<direction>]:conditional
        // -- or --            <transportation mode> [:<direction>]:conditional
        if (!key.endsWith(":conditional")) {
            return false;
        }
        final String[] parts = key.replaceAll(":conditional", "").split(":");
        return isKeyValid3Parts(parts) || isKeyValid1Part(parts) || isKeyValid2Parts(parts);
    }

    private static boolean isKeyValid3Parts(String... parts) {
        return parts.length == 3 && isRestrictionType(parts[0]) && isTransportationMode(parts[1]) && isDirection(parts[2]);
    }

    private static boolean isKeyValid2Parts(String... parts) {
        return parts.length == 2 && ((isRestrictionType(parts[0]) && (isTransportationMode(parts[1]) || isDirection(parts[1])))
                                  || (isTransportationMode(parts[0]) && isDirection(parts[1])));
    }

    private static boolean isKeyValid1Part(String... parts) {
        return parts.length == 1 && (isRestrictionType(parts[0]) || isTransportationMode(parts[0]));
    }

    public boolean isValueValid(String key, String value) {
        return validateValue(key, value) == null;
    }

    static class ConditionalParsingException extends RuntimeException {
        ConditionalParsingException(String message) {
            super(message);
        }
    }

    public static class ConditionalValue {
        public final String restrictionValue;
        public final Collection<String> conditions;

        public ConditionalValue(String restrictionValue, Collection<String> conditions) {
            this.restrictionValue = restrictionValue;
            this.conditions = conditions;
        }

        /**
         * Parses the condition values as string.
         * @param value value, must match {@code <restriction-value> @ <condition>[;<restriction-value> @ <condition>]} pattern
         * @return list of {@code ConditionalValue}s
         * @throws ConditionalParsingException if {@code value} does not match expected pattern
         */
        public static List<ConditionalValue> parse(String value) {
            // <restriction-value> @ <condition>[;<restriction-value> @ <condition>]
            final List<ConditionalValue> r = new ArrayList<>();
            final String part = Pattern.compile("([^@\\p{Space}][^@]*?)"
                    + "\\s*@\\s*" + "(\\([^)\\p{Space}][^)]+?\\)|[^();\\p{Space}][^();]*?)\\s*").toString();
            final Matcher m = Pattern.compile('(' + part + ")(;\\s*" + part + ")*").matcher(value);
            if (!m.matches()) {
                throw new ConditionalParsingException(tr("Does not match pattern ''restriction value @ condition''"));
            } else {
                int i = 2;
                while (i + 1 <= m.groupCount() && m.group(i + 1) != null) {
                    final String restrictionValue = m.group(i);
                    final String[] conditions = m.group(i + 1).replace("(", "").replace(")", "").split("\\s+(AND|and)\\s+");
                    r.add(new ConditionalValue(restrictionValue, Arrays.asList(conditions)));
                    i += 3;
                }
            }
            return r;
        }
    }

    public String validateValue(String key, String value) {
        try {
            for (final ConditionalValue conditional : ConditionalValue.parse(value)) {
                // validate restriction value
                if (isTransportationMode(key.split(":")[0]) && !isRestrictionValue(conditional.restrictionValue)) {
                    return tr("{0} is not a valid restriction value", conditional.restrictionValue);
                }
                // validate opening hour if the value contains an hour (heuristic)
                for (final String condition : conditional.conditions) {
                    if (condition.matches(".*[0-9]:[0-9]{2}.*")) {
                        final List<OpeningHourTest.OpeningHoursTestError> errors = openingHourTest.checkOpeningHourSyntax(
                                "", condition, OpeningHourTest.CheckMode.TIME_RANGE, true, LanguageInfo.getJOSMLocaleCode());
                        if (!errors.isEmpty()) {
                            return errors.get(0).getMessage();
                        }
                    }
                }
            }
        } catch (ConditionalParsingException ex) {
            Main.debug(ex);
            return ex.getMessage();
        }
        return null;
    }

    public List<TestError> validatePrimitive(OsmPrimitive p) {
        final List<TestError> errors = new ArrayList<>();
        for (final String key : SubclassFilteredCollection.filter(p.keySet(),
                Pattern.compile(":conditional(:.*)?$").asPredicate())) {
            if (!isKeyValid(key)) {
                errors.add(TestError.builder(this, Severity.WARNING, 3201)
                        .message(tr("Wrong syntax in {0} key", key))
                        .primitives(p)
                        .build());
                continue;
            }
            final String value = p.get(key);
            final String error = validateValue(key, value);
            if (error != null) {
                errors.add(TestError.builder(this, Severity.WARNING, 3202)
                        .message(tr("Error in {0} value: {1}", key, error))
                        .primitives(p)
                        .build());
            }
        }
        return errors;
    }

    @Override
    public void check(OsmPrimitive p) {
        errors.addAll(validatePrimitive(p));
    }
}
