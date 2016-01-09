// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.mappaint.styleelement;

import java.awt.Font;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.visitor.paint.MapPaintSettings;
import org.openstreetmap.josm.data.osm.visitor.paint.StyledMapRenderer;
import org.openstreetmap.josm.gui.mappaint.Cascade;
import org.openstreetmap.josm.gui.mappaint.Keyword;
import org.openstreetmap.josm.gui.mappaint.StyleKeys;
import org.openstreetmap.josm.gui.mappaint.mapcss.Instruction.RelativeFloat;

public abstract class StyleElement implements StyleKeys {

    protected static final int ICON_IMAGE_IDX = 0;
    protected static final int ICON_WIDTH_IDX = 1;
    protected static final int ICON_HEIGHT_IDX = 2;
    protected static final int ICON_OPACITY_IDX = 3;
    protected static final int ICON_OFFSET_X_IDX = 4;
    protected static final int ICON_OFFSET_Y_IDX = 5;
    protected static final String[] ICON_KEYS = {ICON_IMAGE, ICON_WIDTH, ICON_HEIGHT, ICON_OPACITY, ICON_OFFSET_X, ICON_OFFSET_Y};
    protected static final String[] REPEAT_IMAGE_KEYS = {REPEAT_IMAGE, REPEAT_IMAGE_WIDTH, REPEAT_IMAGE_HEIGHT, REPEAT_IMAGE_OPACITY,
            null, null};

    public float majorZIndex;
    public float zIndex;
    public float objectZIndex;
    public boolean isModifier;  // false, if style can serve as main style for the
    // primitive; true, if it is a highlight or modifier
    public boolean defaultSelectedHandling;

    public StyleElement(float major_z_index, float z_index, float object_z_index, boolean isModifier, boolean defaultSelectedHandling) {
        this.majorZIndex = major_z_index;
        this.zIndex = z_index;
        this.objectZIndex = object_z_index;
        this.isModifier = isModifier;
        this.defaultSelectedHandling = defaultSelectedHandling;
    }

    protected StyleElement(Cascade c, float default_major_z_index) {
        majorZIndex = c.get(MAJOR_Z_INDEX, default_major_z_index, Float.class);
        zIndex = c.get(Z_INDEX, 0f, Float.class);
        objectZIndex = c.get(OBJECT_Z_INDEX, 0f, Float.class);
        isModifier = c.get(MODIFIER, Boolean.FALSE, Boolean.class);
        defaultSelectedHandling = c.isDefaultSelectedHandling();
    }

    /**
     * draws a primitive
     * @param primitive primitive to draw
     * @param paintSettings paint settings
     * @param painter painter
     * @param selected true, if primitive is selected
     * @param outermember true, if primitive is not selected and outer member of a selected multipolygon relation
     * @param member true, if primitive is not selected and member of a selected relation
     */
    public abstract void paintPrimitive(OsmPrimitive primitive, MapPaintSettings paintSettings, StyledMapRenderer painter,
            boolean selected, boolean outermember, boolean member);

    public boolean isProperLineStyle() {
        return false;
    }

    /**
     * Get a property value of type Width
     * @param c the cascade
     * @param key property key for the width value
     * @param relativeTo reference width. Only needed, when relative width syntax is used, e.g. "+4".
     * @return width
     */
    protected static Float getWidth(Cascade c, String key, Float relativeTo) {
        Float width = c.get(key, null, Float.class, true);
        if (width != null) {
            if (width > 0)
                return width;
        } else {
            Keyword widthKW = c.get(key, null, Keyword.class, true);
            if (Keyword.THINNEST.equals(widthKW))
                return 0f;
            if (Keyword.DEFAULT.equals(widthKW))
                return (float) MapPaintSettings.INSTANCE.getDefaultSegmentWidth();
            if (relativeTo != null) {
                RelativeFloat width_rel = c.get(key, null, RelativeFloat.class, true);
                if (width_rel != null)
                    return relativeTo + width_rel.val;
            }
        }
        return null;
    }

    /* ------------------------------------------------------------------------------- */
    /* cached values                                                                   */
    /* ------------------------------------------------------------------------------- */
    /*
     * Two preference values and the set of created fonts are cached in order to avoid
     * expensive lookups and to avoid too many font objects
     *
     * FIXME: cached preference values are not updated if the user changes them during
     * a JOSM session. Should have a listener listening to preference changes.
     */
    private static volatile String DEFAULT_FONT_NAME;
    private static volatile Float DEFAULT_FONT_SIZE;
    private static final Object lock = new Object();

    // thread save access (double-checked locking)
    private static Float getDefaultFontSize() {
        Float s = DEFAULT_FONT_SIZE;
        if (s == null) {
            synchronized (lock) {
                s = DEFAULT_FONT_SIZE;
                if (s == null) {
                    DEFAULT_FONT_SIZE = s = (float) Main.pref.getInteger("mappaint.fontsize", 8);
                }
            }
        }
        return s;
    }

    private static String getDefaultFontName() {
        String n = DEFAULT_FONT_NAME;
        if (n == null) {
            synchronized (lock) {
                n = DEFAULT_FONT_NAME;
                if (n == null) {
                    DEFAULT_FONT_NAME = n = Main.pref.get("mappaint.font", "Droid Sans");
                }
            }
        }
        return n;
    }

    private static class FontDescriptor {
        public String name;
        public int style;
        public int size;

        FontDescriptor(String name, int style, int size) {
            this.name = name;
            this.style = style;
            this.size = size;
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, style, size);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            FontDescriptor that = (FontDescriptor) obj;
            return style == that.style &&
                    size == that.size &&
                    Objects.equals(name, that.name);
        }
    }

    private static final Map<FontDescriptor, Font> FONT_MAP = new HashMap<>();

    private static Font getCachedFont(FontDescriptor fd) {
        Font f = FONT_MAP.get(fd);
        if (f != null) return f;
        f = new Font(fd.name, fd.style, fd.size);
        FONT_MAP.put(fd, f);
        return f;
    }

    private static Font getCachedFont(String name, int style, int size) {
        return getCachedFont(new FontDescriptor(name, style, size));
    }

    protected static Font getFont(Cascade c, String s) {
        String name = c.get(FONT_FAMILY, getDefaultFontName(), String.class);
        float size = c.get(FONT_SIZE, getDefaultFontSize(), Float.class);
        int weight = Font.PLAIN;
        if ("bold".equalsIgnoreCase(c.get(FONT_WEIGHT, null, String.class))) {
            weight = Font.BOLD;
        }
        int style = Font.PLAIN;
        if ("italic".equalsIgnoreCase(c.get(FONT_STYLE, null, String.class))) {
            style = Font.ITALIC;
        }
        Font f = getCachedFont(name, style | weight, Math.round(size));
        if (f.canDisplayUpTo(s) == -1)
            return f;
        else {
            // fallback if the string contains characters that cannot be
            // rendered by the selected font
            return getCachedFont("SansSerif", style | weight, Math.round(size));
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StyleElement that = (StyleElement) o;
        return Float.compare(that.majorZIndex, majorZIndex) == 0 &&
                Float.compare(that.zIndex, zIndex) == 0 &&
                Float.compare(that.objectZIndex, objectZIndex) == 0 &&
                isModifier == that.isModifier;
    }

    @Override
    public int hashCode() {
        return Objects.hash(majorZIndex, zIndex, objectZIndex, isModifier);
    }

    @Override
    public String toString() {
        return String.format("z_idx=[%s/%s/%s] ", majorZIndex, zIndex, objectZIndex) + (isModifier ? "modifier " : "");
    }
}
