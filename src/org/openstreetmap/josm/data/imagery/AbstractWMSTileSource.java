// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.imagery;

import java.awt.Point;

import org.openstreetmap.gui.jmapviewer.Tile;
import org.openstreetmap.gui.jmapviewer.TileXY;
import org.openstreetmap.gui.jmapviewer.interfaces.ICoordinate;
import org.openstreetmap.gui.jmapviewer.tilesources.TMSTileSource;
import org.openstreetmap.gui.jmapviewer.tilesources.TileSourceInfo;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.ProjectionBounds;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.projection.Projection;

/**
 * Base class for different WMS tile sources those based on URL templates and those based on WMS endpoints
 * @author Wiktor Niesiobędzki
 * @since 10990
 *
 */
public class AbstractWMSTileSource extends TMSTileSource {

    private EastNorth anchorPosition;
    private int[] tileXMin;
    private int[] tileYMin;
    private int[] tileXMax;
    private int[] tileYMax;
    private double[] degreesPerTile;
    private static final float SCALE_DENOMINATOR_ZOOM_LEVEL_1 = 559082264.0287178f;

    public AbstractWMSTileSource(TileSourceInfo info) {
        super(info);
    }

    /**
     * Initializes class with current projection in JOSM. This call is needed every time projection changes.
     */
    public void initProjection() {
        initProjection(Main.getProjection());
    }

    private void initAnchorPosition(Projection proj) {
        Bounds worldBounds = proj.getWorldBoundsLatLon();
        EastNorth min = proj.latlon2eastNorth(worldBounds.getMin());
        EastNorth max = proj.latlon2eastNorth(worldBounds.getMax());
        this.anchorPosition = new EastNorth(min.east(), max.north());
    }

    /**
     * Initializes class with projection in JOSM. This call is needed every time projection changes.
     * @param proj new projection that shall be used for computations
     */
    public void initProjection(Projection proj) {
        initAnchorPosition(proj);
        ProjectionBounds worldBounds = proj.getWorldBoundsBoxEastNorth();

        EastNorth topLeft = new EastNorth(worldBounds.getMin().east(), worldBounds.getMax().north());
        EastNorth bottomRight = new EastNorth(worldBounds.getMax().east(), worldBounds.getMin().north());

        // use 256 as "tile size" to keep the scale in line with default tiles in Mercator projection
        double crsScale = 256 * 0.28e-03 / proj.getMetersPerUnit();
        tileXMin = new int[getMaxZoom() + 1];
        tileYMin = new int[getMaxZoom() + 1];
        tileXMax = new int[getMaxZoom() + 1];
        tileYMax = new int[getMaxZoom() + 1];
        degreesPerTile = new double[getMaxZoom() + 1];

        for (int zoom = 1; zoom <= getMaxZoom(); zoom++) {
            // use well known scale set "GoogleCompatibile" from OGC WMTS spec to calculate number of tiles per zoom level
            // this makes the zoom levels "glued" to standard TMS zoom levels
            degreesPerTile[zoom] = (SCALE_DENOMINATOR_ZOOM_LEVEL_1 / Math.pow(2d, zoom - 1d)) * crsScale;
            TileXY minTileIndex = eastNorthToTileXY(topLeft, zoom);
            tileXMin[zoom] = minTileIndex.getXIndex();
            tileYMin[zoom] = minTileIndex.getYIndex();
            TileXY maxTileIndex = eastNorthToTileXY(bottomRight, zoom);
            tileXMax[zoom] = maxTileIndex.getXIndex();
            tileYMax[zoom] = maxTileIndex.getYIndex();
        }
    }

    @Override
    public ICoordinate tileXYToLatLon(Tile tile) {
        return tileXYToLatLon(tile.getXtile(), tile.getYtile(), tile.getZoom());
    }

    @Override
    public ICoordinate tileXYToLatLon(TileXY xy, int zoom) {
        return tileXYToLatLon(xy.getXIndex(), xy.getYIndex(), zoom);
    }

    @Override
    public ICoordinate tileXYToLatLon(int x, int y, int zoom) {
        return Main.getProjection().eastNorth2latlon(getTileEastNorth(x, y, zoom)).toCoordinate();
    }

    @Override
    public TileXY latLonToTileXY(double lat, double lon, int zoom) {
        Projection proj = Main.getProjection();
        EastNorth enPoint = proj.latlon2eastNorth(new LatLon(lat, lon));
        return eastNorthToTileXY(enPoint, zoom);
    }

    private TileXY eastNorthToTileXY(EastNorth enPoint, int zoom) {
        double scale = getDegreesPerTile(zoom);
        return new TileXY(
                (enPoint.east() - anchorPosition.east()) / scale,
                (anchorPosition.north() - enPoint.north()) / scale
                );
    }

    @Override
    public TileXY latLonToTileXY(ICoordinate point, int zoom) {
        return latLonToTileXY(point.getLat(), point.getLon(), zoom);
    }

    @Override
    public int getTileXMax(int zoom) {
        return tileXMax[zoom];
    }

    @Override
    public int getTileXMin(int zoom) {
        return tileXMin[zoom];
    }

    @Override
    public int getTileYMax(int zoom) {
        return tileYMax[zoom];
    }

    @Override
    public int getTileYMin(int zoom) {
        return tileYMin[zoom];
    }

    @Override
    public Point latLonToXY(double lat, double lon, int zoom) {
        double scale = getDegreesPerTile(zoom) / getTileSize();
        EastNorth point = Main.getProjection().latlon2eastNorth(new LatLon(lat, lon));
        return new Point(
                (int) Math.round((point.east() - anchorPosition.east()) / scale),
                (int) Math.round((anchorPosition.north() - point.north()) / scale)
                );
    }

    @Override
    public Point latLonToXY(ICoordinate point, int zoom) {
        return latLonToXY(point.getLat(), point.getLon(), zoom);
    }

    @Override
    public ICoordinate xyToLatLon(Point point, int zoom) {
        return xyToLatLon(point.x, point.y, zoom);
    }

    @Override
    public ICoordinate xyToLatLon(int x, int y, int zoom) {
        double scale = getDegreesPerTile(zoom) / getTileSize();
        Projection proj = Main.getProjection();
        EastNorth ret = new EastNorth(
                anchorPosition.east() + x * scale,
                anchorPosition.north() - y * scale
                );
        return proj.eastNorth2latlon(ret).toCoordinate();
    }

    protected EastNorth getTileEastNorth(int x, int y, int z) {
        double scale = getDegreesPerTile(z);
        return new EastNorth(
                anchorPosition.east() + x * scale,
                anchorPosition.north() - y * scale
                );
    }

    private double getDegreesPerTile(int zoom) {
        return degreesPerTile[zoom];
    }

}
