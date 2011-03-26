// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.mappaint;

import static org.openstreetmap.josm.tools.Utils.equal;

import java.awt.Font;
import java.util.HashMap;
import java.util.Map;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.visitor.paint.MapPaintSettings;
import org.openstreetmap.josm.data.osm.visitor.paint.MapPainter;
import org.openstreetmap.josm.gui.mappaint.mapcss.Instruction.RelativeFloat;

abstract public class ElemStyle {

    public float z_index;
    public float object_z_index;
    public boolean isModifier;  // false, if style can serve as main style for the
    // primitive; true, if it is a highlight or modifier

    public ElemStyle(float z_index, float object_z_index, boolean isModifier) {
        this.z_index = z_index;
        this.object_z_index = object_z_index;
        this.isModifier = isModifier;
    }

    protected ElemStyle(Cascade c, float default_z_index) {
        z_index = c.get("z-index", default_z_index, Float.class);
        object_z_index = c.get("object-z-index", 0f, Float.class);
        isModifier = c.get("modifier", false, Boolean.class);
    }

    public abstract void paintPrimitive(OsmPrimitive primitive, MapPaintSettings paintSettings, MapPainter painter, boolean selected, boolean member);

    public boolean isProperLineStyle() {
        return false;
    }

    /**
     * Get a property value of type Width
     * @param c the cascade
     * @param key property key for the width value
     * @param relativeTo reference width. Only needed, when relative width syntax
     *              is used, e.g. "+4".
     */
    protected static Float getWidth(Cascade c, String key, Float relativeTo) {
        Float width = c.get(key, null, Float.class, true);
        if (width != null) {
            if (width > 0)
                return width;
        } else {
            Keyword widthKW = c.get(key, null, Keyword.class, true);
            if (equal(widthKW, Keyword.THINNEST))
                return 0f;
            if (equal(widthKW, Keyword.DEFAULT))
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
     * (in analogy to flyweight pattern).
     * 
     * FIXME: cached preference values are not updated if the user changes them during
     * a JOSM session. Should have a listener listening to preference changes.
     */
    static private String DEFAULT_FONT_NAME = null;
    static private Float DEFAULT_FONT_SIZE = null;
    static private void initDefaultFontParameters() {
        if (DEFAULT_FONT_NAME != null) return; // already initialized - skip initialization
        DEFAULT_FONT_NAME = Main.pref.get("mappaint.font", "Helvetica");
        DEFAULT_FONT_SIZE = (float) Main.pref.getInteger("mappaint.fontsize", 8);
    }

    static private class FontDescriptor {
        public String name;
        public int style;
        public int size;

        public FontDescriptor(String name, int style, int size){
            this.name = name;
            this.style = style;
            this.size = size;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((name == null) ? 0 : name.hashCode());
            result = prime * result + size;
            result = prime * result + style;
            return result;
        }
        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            FontDescriptor other = (FontDescriptor) obj;
            if (name == null) {
                if (other.name != null)
                    return false;
            } else if (!name.equals(other.name))
                return false;
            if (size != other.size)
                return false;
            if (style != other.style)
                return false;
            return true;
        }
    }

    static private final Map<FontDescriptor, Font> FONT_MAP = new HashMap<FontDescriptor, Font>();
    static private Font getCachedFont(FontDescriptor fd) {
        Font f = FONT_MAP.get(fd);
        if (f != null) return f;
        f = new Font(fd.name, fd.style, fd.size);
        FONT_MAP.put(fd, f);
        return f;
    }

    static private Font getCachedFont(String name, int style, int size){
        return getCachedFont(new FontDescriptor(name, style, size));
    }

    protected static Font getFont(Cascade c) {
        initDefaultFontParameters(); // populated cached preferences, if necesary
        String name = c.get("font-family", DEFAULT_FONT_NAME, String.class);
        float size = c.get("font-size", DEFAULT_FONT_SIZE, Float.class);
        int weight = Font.PLAIN;
        Keyword weightKW = c.get("font-weight", null, Keyword.class);
        if (weightKW != null && equal(weightKW, "bold")) {
            weight = Font.BOLD;
        }
        int style = Font.PLAIN;
        Keyword styleKW = c.get("font-style", null, Keyword.class);
        if (styleKW != null && equal(styleKW.val, "italic")) {
            style = Font.ITALIC;
        }
        return getCachedFont(name, style | weight, Math.round(size));
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ElemStyle))
            return false;
        ElemStyle s = (ElemStyle) o;
        return z_index == s.z_index && object_z_index == s.object_z_index && isModifier == s.isModifier;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 41 * hash + Float.floatToIntBits(this.z_index);
        hash = 41 * hash + Float.floatToIntBits(this.object_z_index);
        hash = 41 * hash + (isModifier ? 1 : 0);
        return hash;
    }

    @Override
    public String toString() {
        if (z_index != 0f || object_z_index != 0f)
            return String.format("z_idx=%s/%s ", z_index, object_z_index) + (isModifier ? "modifier " : "");
        return "";
    }
}
