// License: GPL. For details, see LICENSE file.
/**
 * Compare and analyse the differences of the editor imagery index and the JOSM imagery list.
 * The goal is to keep both lists in sync.
 *
 * The editor imagery index project (https://github.com/osmlab/editor-imagery-index)
 * provides also a version in the JOSM format, but the JSON is the original source
 * format, so we read that.
 *
 * How to run:
 * -----------
 *
 * Main JOSM binary needs to be in classpath, e.g.
 *
 * $ groovy -cp ../dist/josm-custom.jar sync_editor-imagery-index.groovy
 * 
 * Add option "-h" to show the available command line flags.
 */
import java.io.FileReader
import java.util.List

import javax.json.Json
import javax.json.JsonArray
import javax.json.JsonObject
import javax.json.JsonReader

import org.openstreetmap.josm.io.imagery.ImageryReader
import org.openstreetmap.josm.data.imagery.ImageryInfo
import org.openstreetmap.josm.tools.Utils

class sync_editor_imagery_index {

    List<ImageryInfo> josmEntries;
    JsonArray eiiEntries;

    def eiiUrls = new HashMap<String, JsonObject>()
    def josmUrls = new HashMap<String, ImageryInfo>()
    
    static String eiiInputFile = 'imagery.json'
    static String josmInputFile = 'maps.xml'
    static FileWriter outputFile = null
    static BufferedWriter outputStream = null
    static int skipCount = 0;
    static def skipEntries = [:]

    static def options
    
    /**
     * Main method.
     */
    static main(def args) {
        parse_command_line_arguments(args)
        def script = new sync_editor_imagery_index()
        script.loadSkip()
        script.loadJosmEntries()
        script.loadEIIEntries()
        script.checkInOneButNotTheOther()
        script.checkCommonEntries()
        if(outputStream != null) {
            outputStream.close();
        }
        if(outputFile != null) {
            outputFile.close();
        }
    }
    
    /**
     * Parse command line arguments.
     */
    static void parse_command_line_arguments(args) {
        def cli = new CliBuilder()
        cli.o(longOpt:'output', args:1, argName: "output", "Output file, - prints to stdout (default: -)")
        cli.e(longOpt:'eii_input', args:1, argName:"eii_input", "Input file for the editor imagery index (json). Default is $eiiInputFile (current directory).")
        cli.j(longOpt:'josm_input', args:1, argName:"josm_input", "Input file for the JOSM imagery list (xml). Default is $josmInputFile (current directory).")
        cli.s(longOpt:'shorten', "shorten the output, so it is easier to read in a console window")
        cli.n(longOpt:'noskip', argName:"noskip", "don't skip known entries")
        cli.m(longOpt:'nomissingeii', argName:"nomissingeii", "don't show missing editor imagery index entries")
        cli.h(longOpt:'help', "show this help")
        options = cli.parse(args)

        if (options.h) {
            cli.usage()
            System.exit(0)
        }
        if (options.eii_input) {
            eiiInputFile = options.eii_input
        }
        if (options.josm_input) {
            josmInputFile = options.josm_input
        }
        if (options.output && options.output != "-") {
            outputFile = new FileWriter(options.output)
            outputStream = new BufferedWriter(outputFile)
        }
    }

    void loadSkip() {
        if (options.noskip)
            return;
        skipEntries["+++ EII-URL is not unique: http://geolittoral.application.equipement.gouv.fr/wms/metropole?FORMAT=image/jpeg&VERSION=1.1.1&SERVICE=WMS&REQUEST=GetMap&Layers=ortholittorale&SRS={proj}&WIDTH={width}&HEIGHT={height}&BBOX={bbox}"] = 1
        skipEntries["  Streets NRW Geofabrik.de - http://tools.geofabrik.de/osmi/view/strassennrw/wxs?REQUEST=GetMap&SERVICE=wms&VERSION=1.1.1&FORMAT=image/png&SRS={proj}&STYLES=&LAYERS=unzugeordnete_strassen,kreisstrassen_ast,kreisstrassen,landesstrassen_ast,landesstrassen,bundesstrassen_ast,bundesstrassen,autobahnen_ast,autobahnen,endpunkte&WIDTH={width}&HEIGHT={height}&BBOX={bbox}"] = 1
        skipEntries["  Czech UHUL:ORTOFOTO - http://geoportal2.uhul.cz/cgi-bin/oprl.asp?SERVICE=WMS&VERSION=1.1.1&REQUEST=GetMap&SRS={proj}&LAYERS=Ortofoto_cb&STYLES=default&FORMAT=image/jpeg&TRANSPARENT=TRUE&SRS={proj}&WIDTH={width}&HEIGHT={height}&BBOX={bbox}"] = 1
        skipEntries["  Czech ÚHUL:ORTOFOTO tiles proxy - http://osm-{switch:a,b,c}.zby.cz/tiles_uhul.php/{zoom}/{x}/{y}.jpg"] = 1
        skipEntries["  [EE] Estonia Basemap (Maaamet) - http://kaart.maaamet.ee/wms/alus-geo?FORMAT=image/jpeg&VERSION=1.1.1&SERVICE=WMS&REQUEST=GetMap&Layers=pohi_vr2&SRS={proj}&WIDTH={width}&HEIGHT={height}&BBOX={bbox}"] = 1
        skipEntries["  [EE] Estonia Forestry (Maaamet) - http://kaart.maaamet.ee/wms/alus-geo?FORMAT=image/jpeg&VERSION=1.1.1&SERVICE=WMS&REQUEST=GetMap&Layers=cir_ngr&SRS={proj}&WIDTH={width}&HEIGHT={height}&BBOX={bbox}"] = 1
        skipEntries["  [EE] Estonia Hillshading (Maaamet) - http://kaart.maaamet.ee/wms/alus-geo?FORMAT=image/jpeg&VERSION=1.1.1&SERVICE=WMS&REQUEST=GetMap&Layers=reljeef&STYLES=&SRS={proj}&WIDTH={width}&HEIGHT={height}&BBOX={bbox}"] = 1
        skipEntries["  [EE] Estonia Ortho (Maaamet) - http://kaart.maaamet.ee/wms/alus-geo?VERSION=1.1.1&REQUEST=GetMap&LAYERS=of10000&SRS={proj}&FORMAT=image/jpeg&WIDTH={width}&HEIGHT={height}&BBOX={bbox}"] = 1
        skipEntries["  Hamburg (DK5) - http://gateway.hamburg.de/OGCFassade/HH_WMS_Geobasisdaten.aspx?FORMAT=image/jpeg&VERSION=1.1.1&SERVICE=WMS&REQUEST=GetMap&LAYERS=1&STYLES=&SRS={proj}&WIDTH={width}&HEIGHT={height}&BBOX={bbox}"] = 1
        skipEntries["  Hamburg (40 cm) - http://gateway.hamburg.de/OGCFassade/HH_WMS_DOP40.aspx?FORMAT=image/jpeg&VERSION=1.1.1&SERVICE=WMS&REQUEST=GetMap&LAYERS=0&STYLES=&SRS={proj}&WIDTH={width}&HEIGHT={height}&BBOX={bbox}"] = 1
        skipEntries["  name differs: http://wms.openstreetmap.fr/tms/1.0.0/tours_2013/{zoom}/{x}/{y}"] = 3
        skipEntries["  name differs: http://wms.openstreetmap.fr/tms/1.0.0/tours/{zoom}/{x}/{y}"] = 3
        skipEntries["  name differs: https://secure.erlangen.de/arcgiser/services/Luftbilder2011/MapServer/WmsServer?FORMAT=image/bmp&VERSION=1.1.1&SERVICE=WMS&REQUEST=GetMap&LAYERS=Erlangen_ratio10_5cm_gk4.jp2&STYLES=&SRS={proj}&WIDTH={width}&HEIGHT={height}&BBOX={bbox}"] = 3
        skipEntries["  name differs: http://wms.openstreetmap.fr/tms/1.0.0/iomhaiti/{zoom}/{x}/{y}"] = 3
        skipEntries["  name differs: http://{switch:a,b,c}.layers.openstreetmap.fr/bano/{zoom}/{x}/{y}.png"] = 3
        skipEntries["  name differs: http://ooc.openstreetmap.org/os1/{zoom}/{x}/{y}.jpg"] = 3
        skipEntries["  name differs: http://www.gisnet.lv/cgi-bin/osm_latvia?FORMAT=image/jpeg&VERSION=1.1.1&SERVICE=WMS&REQUEST=GetMap&Layers=piekraste&SRS={proj}&WIDTH={width}&height={height}&BBOX={bbox}"] = 3
        skipEntries["  name differs: http://tms.cadastre.openstreetmap.fr/*/tout/{z}/{x}/{y}.png"] = 3
        skipEntries["  name differs: http://{switch:a,b,c}.tiles.mapbox.com/v4/enf.e0b8291e/{zoom}/{x}/{y}.png?access_token=pk.eyJ1Ijoib3BlbnN0cmVldG1hcCIsImEiOiJhNVlHd29ZIn0.ti6wATGDWOmCnCYen-Ip7Q"] = 3
        skipEntries["  name differs: http://geo.nls.uk/mapdata2/os/25_inch/scotland_1/{zoom}/{x}/{y}.png"] = 3
        skipEntries["  name differs: http://geo.nls.uk/mapdata3/os/6_inch_gb_1900/{zoom}/{x}/{y}.png"] = 3
        skipEntries["  maxzoom differs: [DE] Bavaria (2 m) - http://geodaten.bayern.de/ogc/ogc_dop200_oa.cgi?FORMAT=image/jpeg&VERSION=1.1.1&SERVICE=WMS&REQUEST=GetMap&Layers=adv_dop200c&SRS={proj}&WIDTH={width}&HEIGHT={height}&BBOX={bbox}"] = 3
        skipEntries["  minzoom differs: [AU] LPI NSW Administrative Boundaries County - http://maps.six.nsw.gov.au/arcgis/services/public/NSW_Administrative_Boundaries/MapServer/WMSServer?SERVICE=WMS&VERSION=1.3.0&REQUEST=GetMap&CRS={proj}&BBOX={bbox}&WIDTH={width}&HEIGHT={height}&LAYERS=County&STYLES=&FORMAT=image/png32&DPI=96&MAP_RESOLUTION=96&FORMAT_OPTIONS=dpi:96&TRANSPARENT=TRUE"] = 3
        skipEntries["  minzoom differs: [AU] LPI NSW Administrative Boundaries NPWS Reserve - http://maps.six.nsw.gov.au/arcgis/services/public/NSW_Administrative_Boundaries/MapServer/WMSServer?SERVICE=WMS&VERSION=1.3.0&REQUEST=GetMap&CRS={proj}&BBOX={bbox}&WIDTH={width}&HEIGHT={height}&LAYERS=NPWSReserve&STYLES=&FORMAT=image/png32&DPI=96&MAP_RESOLUTION=96&FORMAT_OPTIONS=dpi:96&TRANSPARENT=TRUE"] = 3
        skipEntries["  minzoom differs: [AU] LPI NSW Administrative Boundaries Parish - http://maps.six.nsw.gov.au/arcgis/services/public/NSW_Administrative_Boundaries/MapServer/WMSServer?SERVICE=WMS&VERSION=1.3.0&REQUEST=GetMap&CRS={proj}&BBOX={bbox}&WIDTH={width}&HEIGHT={height}&LAYERS=Parish&STYLES=&FORMAT=image/png32&DPI=96&MAP_RESOLUTION=96&FORMAT_OPTIONS=dpi:96&TRANSPARENT=TRUE"] = 3
        skipEntries["  minzoom differs: [AU] LPI NSW Administrative Boundaries Suburb - http://maps.six.nsw.gov.au/arcgis/services/public/NSW_Administrative_Boundaries/MapServer/WMSServer?SERVICE=WMS&VERSION=1.3.0&REQUEST=GetMap&CRS={proj}&BBOX={bbox}&WIDTH={width}&HEIGHT={height}&LAYERS=Suburb&STYLES=&FORMAT=image/png32&DPI=96&MAP_RESOLUTION=96&FORMAT_OPTIONS=dpi:96&TRANSPARENT=TRUE"] = 3
        skipEntries["  minzoom differs: [AU] LPI NSW Imagery - http://maps.six.nsw.gov.au/arcgis/rest/services/public/NSW_Imagery/MapServer/tile/{zoom}/{y}/{x}"] = 3
        skipEntries["  minzoom differs: [AU] LPI NSW Topographic Map - http://maps.six.nsw.gov.au/arcgis/rest/services/public/NSW_Topo_Map/MapServer/tile/{zoom}/{y}/{x}"] = 3
        skipEntries["  minzoom differs: [AU] LPI NSW Administrative Boundaries State Forest - http://maps.six.nsw.gov.au/arcgis/services/public/NSW_Administrative_Boundaries/MapServer/WMSServer?SERVICE=WMS&VERSION=1.3.0&REQUEST=GetMap&CRS={proj}&BBOX={bbox}&WIDTH={width}&HEIGHT={height}&LAYERS=StateForest&STYLES=&FORMAT=image/png32&DPI=96&MAP_RESOLUTION=96&FORMAT_OPTIONS=dpi:96&TRANSPARENT=TRUE"] = 3
        skipEntries["  minzoom differs: [AU] LPI NSW Administrative Boundaries LGA - http://maps.six.nsw.gov.au/arcgis/services/public/NSW_Administrative_Boundaries/MapServer/WMSServer?SERVICE=WMS&VERSION=1.3.0&REQUEST=GetMap&CRS={proj}&BBOX={bbox}&WIDTH={width}&HEIGHT={height}&LAYERS=LocalGovernmentArea&STYLES=&FORMAT=image/png32&DPI=96&MAP_RESOLUTION=96&FORMAT_OPTIONS=dpi:96&TRANSPARENT=TRUE"] = 3
        skipEntries["  minzoom differs: [AU] LPI NSW Base Map - http://maps.six.nsw.gov.au/arcgis/rest/services/public/NSW_Base_Map/MapServer/tile/{zoom}/{y}/{x}"] = 3
    }
    
    void myprintln(String s) {
        if(skipEntries.containsKey(s)) {
            skipCount = skipEntries.get(s)
        }
        if(skipCount) {
            skipCount -= 1;
            return
        }
        if(outputStream != null) {
            outputStream.write(s);
            outputStream.newLine();
        } else {
            println s;
        }
    }
    
    void loadEIIEntries() {
        FileReader fr = new FileReader(eiiInputFile)
        JsonReader jr = Json.createReader(fr)
        eiiEntries = jr.readArray()
        jr.close()
        
        for (def e : eiiEntries) {
            def url = getUrl(e)
            if (eiiUrls.containsKey(url)) {
                myprintln "+++ EII-URL is not unique: "+url
            } else {
                eiiUrls.put(url, e)
            }
        }
        myprintln "*** Loaded ${eiiEntries.size()} entries (EII). ***"
    }

    void loadJosmEntries() {
        def reader = new ImageryReader(josmInputFile)
        josmEntries = reader.parse()
        
        for (def e : josmEntries) {
            def url = getUrl(e)
            if (josmUrls.containsKey(url)) {
                myprintln "+++ JOSM-URL is not unique: "+url
            } else {
              josmUrls.put(url, e)
            }
        }
        myprintln "*** Loaded ${josmEntries.size()} entries (JOSM). ***"
    }

    List inOneButNotTheOther(Map m1, Map m2) {
        def l = []
        for (def url : m1.keySet()) {
            if (!m2.containsKey(url)) {
                def name = getName(m1.get(url))
                l += "  "+getDescription(m1.get(url))
            }
        }
        l.sort()
    }
    
    void checkInOneButNotTheOther() {
        def l1 = inOneButNotTheOther(eiiUrls, josmUrls)
        myprintln "*** URLs found in EII but not in JOSM (${l1.size()}): ***"
        if (l1.isEmpty()) {
            myprintln "  -"
        } else {
            for (def l : l1)
                myprintln l
        }

        if (options.nomissingeii)
            return
        def l2 = inOneButNotTheOther(josmUrls, eiiUrls)
        myprintln "*** URLs found in JOSM but not in EII (${l2.size()}): ***"
        if (l2.isEmpty()) {
            myprintln "  -"
        } else {
            for (def l : l2)
                myprintln l
        }
    }
    
    void checkCommonEntries() {
        myprintln "*** Same URL, but different name: ***"
        for (def url : eiiUrls.keySet()) {
            def e = eiiUrls.get(url)
            if (!josmUrls.containsKey(url)) continue
            def j = josmUrls.get(url)
            if (!getName(e).equals(getName(j))) {
                myprintln "  name differs: $url"
                myprintln "     (IEE):     ${getName(e)}"
                myprintln "     (JOSM):    ${getName(j)}"
            }
        }
        
        myprintln "*** Same URL, but different type: ***"
        for (def url : eiiUrls.keySet()) {
            def e = eiiUrls.get(url)
            if (!josmUrls.containsKey(url)) continue
            def j = josmUrls.get(url)
            if (!getType(e).equals(getType(j))) {
                myprintln "  type differs: ${getName(j)} - $url"
                myprintln "     (IEE):     ${getType(e)}"
                myprintln "     (JOSM):    ${getType(j)}"
            }
        }
        
        myprintln "*** Same URL, but different zoom bounds: ***"
        for (def url : eiiUrls.keySet()) {
            def e = eiiUrls.get(url)
            if (!josmUrls.containsKey(url)) continue
            def j = josmUrls.get(url)

            Integer eMinZoom = getMinZoom(e)
            Integer jMinZoom = getMinZoom(j)
            if (eMinZoom != jMinZoom  && !(eMinZoom == 0 && jMinZoom == null)) {
                myprintln "  minzoom differs: ${getDescription(j)}"
                myprintln "     (IEE):     ${eMinZoom}"
                myprintln "     (JOSM):    ${jMinZoom}"
            }
            Integer eMaxZoom = getMaxZoom(e)
            Integer jMaxZoom = getMaxZoom(j)
            if (eMaxZoom != jMaxZoom) {
                myprintln "  maxzoom differs: ${getDescription(j)}"
                myprintln "     (IEE):     ${eMaxZoom}"
                myprintln "     (JOSM):    ${jMaxZoom}"
            }
        }
        
        myprintln "*** Same URL, but different country code: ***"
        for (def url : eiiUrls.keySet()) {
            def e = eiiUrls.get(url)
            if (!josmUrls.containsKey(url)) continue
            def j = josmUrls.get(url)
            if (!getCountryCode(e).equals(getCountryCode(j))) {
                myprintln "  country code differs: ${getDescription(j)}"
                myprintln "     (IEE):     ${getCountryCode(e)}"
                myprintln "     (JOSM):    ${getCountryCode(j)}"
            }
        }
        myprintln "*** Same URL, but different quality: ***"
        for (def url : eiiUrls.keySet()) {
            def e = eiiUrls.get(url)
            if (!josmUrls.containsKey(url)) {
              def q = getQuality(e)
              if("best".equals(q)) {
                myprintln "  quality best entry not in JOSM for ${getDescription(e)}"
              }
              continue
            }
            def j = josmUrls.get(url)
            if (!getQuality(e).equals(getQuality(j))) {
                myprintln "  quality differs: ${getDescription(j)}"
                myprintln "     (IEE):     ${getQuality(e)}"
                myprintln "     (JOSM):    ${getQuality(j)}"
            }
        }
    }
    
    /**
     * Utility functions that allow uniform access for both ImageryInfo and JsonObject.
     */
    static String getUrl(Object e) {
        if (e instanceof ImageryInfo) return e.url
        return e.getString("url")
    }
    static String getName(Object e) {
        if (e instanceof ImageryInfo) return e.name
        return e.getString("name")
    }
    static String getType(Object e) {
        if (e instanceof ImageryInfo) return e.getImageryType().getTypeString()
        return e.getString("type")
    }
    static Integer getMinZoom(Object e) {
        if (e instanceof ImageryInfo) {
            int mz = e.getMinZoom()
            return mz == 0 ? null : mz
        } else {
            def ext = e.getJsonObject("extent")
            if (ext == null) return null
            def num = ext.getJsonNumber("min_zoom")
            if (num == null) return null
            return num.intValue()
        }
    }
    static Integer getMaxZoom(Object e) {
        if (e instanceof ImageryInfo) {
            int mz = e.getMaxZoom()
            return mz == 0 ? null : mz
        } else {
            def ext = e.getJsonObject("extent")
            if (ext == null) return null
            def num = ext.getJsonNumber("max_zoom")
            if (num == null) return null
            return num.intValue()
        }
    }
    static String getCountryCode(Object e) {
        if (e instanceof ImageryInfo) return "".equals(e.getCountryCode()) ? null : e.getCountryCode()
        return e.getString("country_code", null)
    }
    static String getQuality(Object e) {
        //if (e instanceof ImageryInfo) return "".equals(e.getQuality()) ? null : e.getQuality()
        if (e instanceof ImageryInfo) return null
        return e.get("best") ? "best" : null
    }
    String getDescription(Object o) {
        def url = getUrl(o)
        def cc = getCountryCode(o)
        if (cc == null) {
            def j = josmUrls.get(url)
            if (j != null) cc = getCountryCode(j)
            if (cc == null) {
                def e = eiiUrls.get(url)
                if (e != null) cc = getCountryCode(e)
            }
        }
        if (cc == null) {
            cc = ''
        } else {
            cc = "[$cc] "
        }
        def d = cc + getName(o) + " - " + getUrl(o)
        if (options.shorten) {
            def MAXLEN = 140
            if (d.length() > MAXLEN) d = d.substring(0, MAXLEN-1) + "..."
        }
        return d
    }

}
