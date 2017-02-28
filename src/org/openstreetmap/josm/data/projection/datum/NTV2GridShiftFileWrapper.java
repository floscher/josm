// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.projection.datum;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.io.CachedFile;

/**
 * Wrapper for {@link NTV2GridShiftFile}.
 *
 * Loads the shift file from disk, when it is first accessed.
 * @since 5226
 */
public class NTV2GridShiftFileWrapper {

    private NTV2GridShiftFile instance;
    private final String gridFileName;

    /**
     * Constructs a new {@code NTV2GridShiftFileWrapper}.
     * @param filename Path to the grid file (GSB format)
     */
    public NTV2GridShiftFileWrapper(String filename) {
        this.gridFileName = filename;
    }

    /**
     * Returns the actual {@link NTV2GridShiftFile} behind this wrapper.
     * The grid file is only loaded once, when first accessed.
     * @return The NTv2 grid file
     * @throws IOException if the grid file cannot be found/loaded
     */
    public synchronized NTV2GridShiftFile getShiftFile() throws IOException {
        if (instance == null) {
            File grid = null;
            // Check is the grid is installed in default PROJ.4 directories
            for (File dir : Main.platform.getDefaultProj4NadshiftDirectories()) {
                File file = new File(dir, gridFileName);
                if (file.exists() && file.isFile()) {
                    grid = file;
                    break;
                }
            }
            // If not, search into PROJ_LIB directory
            if (grid == null) {
                String projLib = System.getProperty("PROJ_LIB");
                if (projLib != null && !projLib.isEmpty()) {
                    File dir = new File(projLib);
                    if (dir.exists() && dir.isDirectory()) {
                        File file = new File(dir, gridFileName);
                        if (file.exists() && file.isFile()) {
                            grid = file;
                        }
                    }
                }
            }
            // If not, retrieve it from JOSM website
            String location = grid != null ? grid.getAbsolutePath() : (Main.getJOSMWebsite() + "/proj/" + gridFileName);
            // Try to load grid file
            try (CachedFile cf = new CachedFile(location); InputStream is = cf.getInputStream()) {
                NTV2GridShiftFile ntv2 = new NTV2GridShiftFile();
                ntv2.loadGridShiftFile(is, false);
                instance = ntv2;
            }
        }
        return instance;
    }
}
