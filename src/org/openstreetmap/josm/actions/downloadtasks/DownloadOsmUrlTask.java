// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions.downloadtasks;

import java.util.concurrent.Future;

import static org.openstreetmap.josm.tools.I18n.tr;

import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.tools.OsmUrlToBounds;

public class DownloadOsmUrlTask extends DownloadOsmTask {

    @Override
    public Future<?> loadUrl(boolean newLayer, String url, ProgressMonitor progressMonitor) {
        return download(newLayer, OsmUrlToBounds.parse(url), null);
    }
    
    @Override
    public String[] getPatterns() {
        return new String[]{"http://www\\.openstreetmap\\.org/\\?lat=.*&lon=.*"};
    }

    @Override
    public String getTitle() {
        return tr("Download OSM URL");
    }
}
