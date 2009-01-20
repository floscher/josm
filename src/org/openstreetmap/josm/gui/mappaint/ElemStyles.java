package org.openstreetmap.josm.gui.mappaint;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Iterator;

import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.OsmUtils;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.Main;

public class ElemStyles
{
    public class StyleSet {
        private HashMap<String, IconElemStyle> icons;
        private HashMap<String, LineElemStyle> lines;
        private HashMap<String, AreaElemStyle> areas;
        private HashMap<String, LineElemStyle> modifiers;
        public StyleSet()
        {
            icons = new HashMap<String, IconElemStyle>();
            lines = new HashMap<String, LineElemStyle>();
            modifiers = new HashMap<String, LineElemStyle>();
            areas = new HashMap<String, AreaElemStyle>();
        }
        private IconElemStyle getNode(Map<String, String> keys)
        {
            IconElemStyle ret = null;
            Iterator<String> iterator = keys.keySet().iterator();
            while(iterator.hasNext())
            {
                String key = iterator.next();
                String val = keys.get(key);
                IconElemStyle style;
                if((style = icons.get("n" + key + "=" + val)) != null)
                {
                    if(ret == null || style.priority > ret.priority)
                        ret = style;
                }
                if((style = icons.get("b" + key + "=" + OsmUtils.getNamedOsmBoolean(val))) != null)
                {
                    if(ret == null || style.priority > ret.priority)
                        ret = style;
                }
                if((style = icons.get("x" + key)) != null)
                {
                    if(ret == null || style.priority > ret.priority)
                        ret = style;
                }
            }
            return ret;
        }
        private ElemStyle get(Map<String, String> keys)
        {
            AreaElemStyle retArea = null;
            LineElemStyle retLine = null;
            String linestring = null;
            HashMap<String, LineElemStyle> over = new HashMap<String, LineElemStyle>();
            Iterator<String> iterator = keys.keySet().iterator();
            while(iterator.hasNext())
            {
                String key = iterator.next();
                String val = keys.get(key);
                AreaElemStyle styleArea;
                LineElemStyle styleLine;
                String idx = "n" + key + "=" + val;
                if((styleArea = areas.get(idx)) != null && (retArea == null || styleArea.priority > retArea.priority))
                    retArea = styleArea;
                if((styleLine = lines.get(idx)) != null && (retLine == null || styleLine.priority > retLine.priority))
                {
                    retLine = styleLine;
                    linestring = idx;
                }
                if((styleLine = modifiers.get(idx)) != null)
                    over.put(idx, styleLine);
                idx = "b" + key + "=" + OsmUtils.getNamedOsmBoolean(val);
                if((styleArea = areas.get(idx)) != null && (retArea == null || styleArea.priority > retArea.priority))
                    retArea = styleArea;
                if((styleLine = lines.get(idx)) != null && (retLine == null || styleLine.priority > retLine.priority))
                {
                    retLine = styleLine;
                    linestring = idx;
                }
                if((styleLine = modifiers.get(idx)) != null)
                    over.put(idx, styleLine);
                idx = "x" + key;
                if((styleArea = areas.get(idx)) != null && (retArea == null || styleArea.priority > retArea.priority))
                    retArea = styleArea;
                if((styleLine = lines.get(idx)) != null && (retLine == null || styleLine.priority > retLine.priority))
                {
                    retLine = styleLine;
                    linestring = idx;
                }
                if((styleLine = modifiers.get(idx)) != null)
                    over.put(idx, styleLine);
            }
            over.remove(linestring);
            if(over.size() != 0 && retLine != null)
            {
                List<LineElemStyle> s = new LinkedList<LineElemStyle>(over.values());
                Collections.sort(s);
                retLine = new LineElemStyle(retLine, s);
            }
            if(retArea != null)
            {
                if(retLine != null)
                    return new AreaElemStyle(retArea, retLine);
                else
                    return retArea;
            }
            return retLine;
        }

        public ElemStyle get(OsmPrimitive osm)
        {
            return (osm.keys == null) ? null :
            ((osm instanceof Node) ? getNode(osm.keys) : get(osm.keys));
        }

        public IconElemStyle getIcon(OsmPrimitive osm)
        {
            return (osm.keys == null) ? null : getNode(osm.keys);
        }

        public boolean isArea(OsmPrimitive o)
        {
            if(o.keys != null && !(o instanceof Node))
            {
                Iterator<String> iterator = o.keys.keySet().iterator();
                while(iterator.hasNext())
                {
                    String key = iterator.next();
                    String val = o.keys.get(key);
                    if(areas.containsKey("n" + key + "=" + val)
                    || areas.containsKey("b" + key + "=" + OsmUtils.getNamedOsmBoolean(val))
                    || areas.containsKey("x" + key))
                        return true;
                }
            }
            return false;
        }

        public boolean hasAreas()
        {
            return areas.size() > 0;
        }
    }

    HashMap<String, StyleSet> styleSet;
    public ElemStyles()
    {
        styleSet = new HashMap<String, StyleSet>();
    }

    private String getKey(String k, String v, String b)
    {
        if(v != null)
            return "n" + k + "=" + v;
        else if(b != null)
            return "b" + k  + "=" + OsmUtils.getNamedOsmBoolean(b);
        else
            return "x" + k;
    }

    public void add(String name, String k, String v, String b, LineElemStyle style)
    {
        String key = getKey(k,v,b);
        style.code = key;
        getStyleSet(name, true).lines.put(key, style);
    }

    public void addModifier(String name, String k, String v, String b, LineElemStyle style)
    {
        String key = getKey(k,v,b);
        style.code = key;
        getStyleSet(name, true).modifiers.put(key, style);
    }

    public void add(String name, String k, String v, String b, AreaElemStyle style)
    {
        String key = getKey(k,v,b);
        style.code = key;
        getStyleSet(name, true).areas.put(key, style);
    }

    public void add(String name, String k, String v, String b, IconElemStyle style)
    {
        String key = getKey(k,v,b);
        style.code = key;
        getStyleSet(name, true).icons.put(key, style);
    }

    private StyleSet getStyleSet(String name, boolean create)
    {
        if(name == null)
            name = Main.pref.get("mappaint.style", "standard");

        StyleSet s = styleSet.get(name);
        if(create && s == null)
        {
            s = new StyleSet();
            styleSet.put(name, s);
        }
        return s;
    }

    /* called from class users, never return null */
    public StyleSet getStyleSet()
    {
        return getStyleSet(null, false);
    }
}
