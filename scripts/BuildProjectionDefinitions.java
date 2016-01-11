// License: GPL. For details, see LICENSE file.

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.openstreetmap.josm.data.projection.CustomProjection;
import org.openstreetmap.josm.data.projection.ProjectionConfigurationException;
import org.openstreetmap.josm.data.projection.Projections;
import org.openstreetmap.josm.data.projection.Projections.ProjectionDefinition;
import org.openstreetmap.josm.data.projection.proj.Proj;

/**
 * Generates the list of projections by combining two sources: The list from the
 * proj.4 project and a list maintained by the JOSM team.
 */
public class BuildProjectionDefinitions {

    private static final String JOSM_EPSG_FILE = "data_nodist/projection/josm-epsg";
    private static final String PROJ4_EPSG_FILE = "data_nodist/projection/epsg";
    private static final String OUTPUT_EPSG_FILE = "data/projection/custom-epsg";

    private static final Map<String, ProjectionDefinition> epsgProj4 = new LinkedHashMap<>();
    private static final Map<String, ProjectionDefinition> epsgJosm = new LinkedHashMap<>();

    private static final boolean printStats = false;

    // statistics:
    private static int noInJosm = 0;
    private static int noDeprecated = 0;
    private static int noGeocent = 0;
    private static int noBaseProjection = 0;
    private static final Map<String, Integer> baseProjectionMap = new HashMap<>();
    private static int noDatumgrid = 0;
    private static int noJosm = 0;
    private static int noProj4 = 0;

    /**
     * Program entry point
     * @param args command line arguments (not used)
     * @throws IOException if any I/O error occurs
     */
    public static void main(String[] args) throws IOException {
        buildList(args[0]);
    }

    static void buildList(String baseDir) throws IOException {
        List<ProjectionDefinition> pdJosm = Projections.loadProjectionDefinitions(baseDir + File.separator + JOSM_EPSG_FILE);
        for (ProjectionDefinition pd : pdJosm) {
            epsgJosm.put(pd.code, pd);
        }
        List<ProjectionDefinition> pdProj4 = Projections.loadProjectionDefinitions(baseDir + File.separator + PROJ4_EPSG_FILE);
        for (ProjectionDefinition pd : pdProj4) {
            epsgProj4.put(pd.code, pd);
        }

        try (BufferedWriter out = new BufferedWriter(new OutputStreamWriter(
                new FileOutputStream(baseDir + File.separator + OUTPUT_EPSG_FILE), StandardCharsets.UTF_8))) {
            out.write("## This file is autogenerated, do not edit!\n");
            out.write("## Run ant task \"epsg\" to rebuild.\n");
            out.write(String.format("## Source files are %s (can be changed) and %s (copied from the proj.4 project).%n", JOSM_EPSG_FILE, PROJ4_EPSG_FILE));
            out.write("##\n");
            out.write("## Entries checked and maintained by the JOSM team:\n");
            for (ProjectionDefinition pd : epsgJosm.values()) {
                write(out, pd);
                noJosm++;
            }
            out.write("## Other supported projections (source: proj.4):\n");
            for (ProjectionDefinition pd : epsgProj4.values()) {
                if (doInclude(pd, true)) {
                    write(out, pd);
                    noProj4++;
                }
            }
        }

        if (printStats) {
            System.out.println(String.format("loaded %d entries from %s", epsgJosm.size(), JOSM_EPSG_FILE));
            System.out.println(String.format("loaded %d entries from %s", epsgProj4.size(), PROJ4_EPSG_FILE));
            System.out.println();
            System.out.println("some entries from proj.4 have not been included:");
            System.out.println(String.format(" * already in the maintained JOSM list: %d entries", noInJosm));
            System.out.println(String.format(" * deprecated: %d entries", noDeprecated));
            System.out.println(String.format(" * using +proj=geocent, which is 3D (X,Y,Z) and not useful in JOSM: %d entries", noGeocent));
            System.out.println(String.format(" * unsupported base projection: %d entries", noBaseProjection));
            System.out.println("   in particular: " + baseProjectionMap);
            System.out.println(String.format(" * requires data file for datum conversion: %d entries", noDatumgrid));
            System.out.println();
            System.out.println(String.format("written %d entries from %s", noJosm, JOSM_EPSG_FILE));
            System.out.println(String.format("written %d entries from %s", noProj4, PROJ4_EPSG_FILE));
        }

    }

    static void write(BufferedWriter out, ProjectionDefinition pd) throws IOException {
        out.write("# " + pd.name + "\n");
        out.write("<"+pd.code.substring("EPSG:".length())+"> "+pd.definition+" <>\n");
    }

    static boolean doInclude(ProjectionDefinition pd, boolean noIncludeJosm) {

        boolean result = true;

        if (noIncludeJosm) {
            // we already have this projection
            if (epsgJosm.containsKey(pd.code)) {
                result = false;
                noInJosm++;
            }
        }

        // exclude deprecated projections
        // EPSG:4296 is also deprecated, but this is not mentioned in the name
        if (pd.name.contains("deprecated") || pd.code.equals("EPSG:4296")) {
            result = false;
            noDeprecated++;
        }

        Map<String, String> parameters;
        try {
            parameters = CustomProjection.parseParameterList(pd.definition, true);
        } catch (ProjectionConfigurationException ex) {
            throw new RuntimeException(pd.code+":"+ex);
        }
        String proj = parameters.get(CustomProjection.Param.proj.key);

        // +proj=geocent is 3D (X,Y,Z) "projection" - this is not useful in
        // JOSM as we only deal with 2D maps
        if ("geocent".equals(proj)) {
            result = false;
            noGeocent++;
        }

        // exclude entries where we don't support the base projection
        Proj bp = Projections.getBaseProjection(proj);
        if (!"utm".equals(proj) && bp == null) {
            result = false;
            noBaseProjection++;
            if (!"geocent".equals(proj)) {
                if (!baseProjectionMap.containsKey(proj)) {
                    baseProjectionMap.put(proj, 0);
                }
                baseProjectionMap.put(proj, baseProjectionMap.get(proj)+1);
            }
        }

        // no support for NAD27 datum, as it requires a conversion database
        String datum = parameters.get(CustomProjection.Param.datum.key);
        if ("NAD27".equals(datum)) {
            result = false;
            noDatumgrid++;
        }

        // requires datum conversion database
        if (parameters.containsKey("geoidgrids")) {
            result = false;
            noDatumgrid++;
        }

        return result;
    }
}