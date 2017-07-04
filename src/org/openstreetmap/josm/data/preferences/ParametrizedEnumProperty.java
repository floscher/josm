// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.preferences;

import org.openstreetmap.josm.Main;

/**
 * Abstract base class for properties with {@link Enum} value, where the preference
 * key is generated from a list of parameters.
 * @param <T> the {@code Enum} class
 */
public abstract class ParametrizedEnumProperty<T extends Enum<T>> {

    protected final T defaultValue;
    protected final Class<T> enumClass;

    public ParametrizedEnumProperty(Class<T> enumClass, T defaultValue) {
        this.defaultValue = defaultValue;
        this.enumClass = enumClass;
    }

    protected abstract String getKey(String... params);

    public T get(String... params) {
        return parse(Main.pref.get(getKey(params), defaultValue.name()));
    }

    public boolean put(T value, String... params) {
        return Main.pref.put(getKey(params), value.name());
    }

    protected T parse(String s) {
        try {
            return Enum.valueOf(enumClass, s);
        } catch (IllegalArgumentException e) {
            Main.trace(e);
            return defaultValue;
        }
    }
}
