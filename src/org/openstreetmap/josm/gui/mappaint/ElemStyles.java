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
    private class StyleSet {
        HashMap<String, IconElemStyle> icons;
        HashMap<String, LineElemStyle> lines;
        HashMap<String, AreaElemStyle> areas;
        HashMap<String, LineElemStyle> modifiers;
        public StyleSet()
        {
            icons = new HashMap<String, IconElemStyle>();
            lines = new HashMap<String, LineElemStyle>();
            modifiers = new HashMap<String, LineElemStyle>();
            areas = new HashMap<String, AreaElemStyle>();
        }
    }
    HashMap<String, StyleSet> styleSet;
    String styleName;

    public ElemStyles()
    {
        styleSet = new HashMap<String, StyleSet>();
        updateStyleName();
    }

    public void updateStyleName() {
        // Main.pref.get() is slow when done thousands of times, do it once here and cache it
        styleName = Main.pref.get("mappaint.style", "standard");
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
            name = styleName;
        StyleSet s = styleSet.get(name);
        if(create && s == null)
        {
            s = new StyleSet();
            styleSet.put(name, s);
        }
        return s;
    }

    private ElemStyle getNode(Map<String, String> keys, StyleSet ss)
    {
        IconElemStyle ret = null;
        Iterator<String> iterator = keys.keySet().iterator();
        while(iterator.hasNext())
        {
            String key = iterator.next();
            String val = keys.get(key);
            IconElemStyle style;
            if((style = ss.icons.get("n" + key + "=" + val)) != null)
            {
                if(ret == null || style.priority > ret.priority)
                    ret = style;
            }
            if((style = ss.icons.get("b" + key + "=" + OsmUtils.getNamedOsmBoolean(val))) != null)
            {
                if(ret == null || style.priority > ret.priority)
                    ret = style;
            }
            if((style = ss.icons.get("x" + key)) != null)
            {
                if(ret == null || style.priority > ret.priority)
                    ret = style;
            }
        }
        return ret;
    }

    private ElemStyle get(Map<String, String> keys, StyleSet ss)
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
            if((styleArea = ss.areas.get(idx)) != null && (retArea == null || styleArea.priority > retArea.priority))
                retArea = styleArea;
            if((styleLine = ss.lines.get(idx)) != null && (retLine == null || styleLine.priority > retLine.priority))
            {
                retLine = styleLine;
                linestring = idx;
            }
            if((styleLine = ss.modifiers.get(idx)) != null)
                over.put(idx, styleLine);
            idx = "b" + key + "=" + OsmUtils.getNamedOsmBoolean(val);
            if((styleArea = ss.areas.get(idx)) != null && (retArea == null || styleArea.priority > retArea.priority))
                retArea = styleArea;
            if((styleLine = ss.lines.get(idx)) != null && (retLine == null || styleLine.priority > retLine.priority))
            {
                retLine = styleLine;
                linestring = idx;
            }
            if((styleLine = ss.modifiers.get(idx)) != null)
                over.put(idx, styleLine);
            idx = "x" + key;
            if((styleArea = ss.areas.get(idx)) != null && (retArea == null || styleArea.priority > retArea.priority))
                retArea = styleArea;
            if((styleLine = ss.lines.get(idx)) != null && (retLine == null || styleLine.priority > retLine.priority))
            {
                retLine = styleLine;
                linestring = idx;
            }
            if((styleLine = ss.modifiers.get(idx)) != null)
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
        StyleSet ss = getStyleSet(null, false);
        return (ss == null || osm.keys == null) ? null :
        ((osm instanceof Node) ? getNode(osm.keys, ss) : get(osm.keys, ss));
    }

    private boolean isArea(Map<String, String> keys, StyleSet ss)
    {
        Iterator<String> iterator = keys.keySet().iterator();
        while(iterator.hasNext())
        {
            String key = iterator.next();
            String val = keys.get(key);
            if(ss.areas.containsKey("n" + key + "=" + val)
            || ss.areas.containsKey("b" + key + "=" + OsmUtils.getNamedOsmBoolean(val))
            || ss.areas.containsKey("x" + key))
                return true;
        }
        return false;
    }

    public boolean isArea(OsmPrimitive o)
    {
        StyleSet ss = getStyleSet(null, false);
        return (ss != null && o.keys != null && !(o instanceof Node))
        ? isArea(o.keys, ss) : false;
    }

    public boolean hasAreas()
    {
        StyleSet ss = getStyleSet(null, false);
        return ss != null && ss.areas.size() > 0;
    }
}
