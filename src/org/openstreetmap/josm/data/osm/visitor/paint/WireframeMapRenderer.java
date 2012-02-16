/* License: GPL. Copyright 2007 by Immanuel Scholz and others */
package org.openstreetmap.josm.data.osm.visitor.paint;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.util.Collection;
import java.util.Iterator;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.osm.BBox;
import org.openstreetmap.josm.data.osm.Changeset;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.WaySegment;
import org.openstreetmap.josm.data.osm.visitor.Visitor;
import org.openstreetmap.josm.gui.NavigatableComponent;

/**
 * A map renderer that paints a simple scheme of every primitive it visits to a
 * previous set graphic environment.
 *
 * @author imi
 */
public class WireframeMapRenderer extends AbstractMapRenderer implements Visitor {

    /**
     * Preferences
     */
    protected Color inactiveColor;
    protected Color selectedColor;
    protected Color nodeColor;
    protected Color dfltWayColor;
    protected Color relationColor;
    protected Color untaggedWayColor;
    protected Color incompleteColor;
    protected Color backgroundColor;
    protected Color highlightColor;
    protected Color taggedColor;
    protected Color connectionColor;
    protected Color taggedConnectionColor;
    protected boolean showDirectionArrow;
    protected boolean showOnewayArrow;
    protected boolean showHeadArrowOnly;
    protected boolean showOrderNumber;
    protected boolean fillSelectedNode;
    protected boolean fillUnselectedNode;
    protected boolean fillTaggedNode;
    protected boolean fillConnectionNode;
    protected int selectedNodeSize;
    protected int unselectedNodeSize;
    protected int connectionNodeSize;
    protected int taggedNodeSize;
    protected int defaultSegmentWidth;
    protected int virtualNodeSize;
    protected int virtualNodeSpace;
    protected int segmentNumberSpace;

    /**
     * Draw subsequent segments of same color as one Path
     */
    protected Color currentColor = null;
    protected GeneralPath currentPath = new GeneralPath();

    /**
     * {@inheritDoc}
     */
    public WireframeMapRenderer(Graphics2D g, NavigatableComponent nc, boolean isInactiveMode) {
        super(g, nc, isInactiveMode);
    }

    public void getColors()
    {
        inactiveColor = PaintColors.INACTIVE.get();
        selectedColor = PaintColors.SELECTED.get();
        nodeColor = PaintColors.NODE.get();
        dfltWayColor = PaintColors.DEFAULT_WAY.get();
        relationColor = PaintColors.RELATION.get();
        untaggedWayColor = PaintColors.UNTAGGED_WAY.get();
        incompleteColor = PaintColors.INCOMPLETE_WAY.get();
        backgroundColor = PaintColors.BACKGROUND.get();
        highlightColor = PaintColors.HIGHLIGHT_WIREFRAME.get();
        taggedColor = PaintColors.TAGGED.get();
        connectionColor = PaintColors.CONNECTION.get();

        if (taggedColor != nodeColor) {
            taggedConnectionColor = taggedColor;
        } else {
            taggedConnectionColor = connectionColor;
        }
    }

    protected void getSettings(boolean virtual) {
        MapPaintSettings settings = MapPaintSettings.INSTANCE;
        showDirectionArrow = settings.isShowDirectionArrow();
        showOnewayArrow = settings.isShowOnewayArrow();
        showHeadArrowOnly = settings.isShowHeadArrowOnly();
        showOrderNumber = settings.isShowOrderNumber();
        selectedNodeSize = settings.getSelectedNodeSize();
        unselectedNodeSize = settings.getUnselectedNodeSize();
        connectionNodeSize = settings.getConnectionNodeSize();
        taggedNodeSize = settings.getTaggedNodeSize();
        defaultSegmentWidth = settings.getDefaultSegmentWidth();
        fillSelectedNode = settings.isFillSelectedNode();
        fillUnselectedNode = settings.isFillUnselectedNode();
        fillConnectionNode = settings.isFillConnectionNode();
        fillTaggedNode = settings.isFillTaggedNode();
        virtualNodeSize = virtual ? Main.pref.getInteger("mappaint.node.virtual-size", 8) / 2 : 0;
        virtualNodeSpace = Main.pref.getInteger("mappaint.node.virtual-space", 70);
        segmentNumberSpace = Main.pref.getInteger("mappaint.segmentnumber.space", 40);
        getColors();

        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                Main.pref.getBoolean("mappaint.wireframe.use-antialiasing", false) ?
                        RenderingHints.VALUE_ANTIALIAS_ON : RenderingHints.VALUE_ANTIALIAS_OFF);
    }

    DataSet ds;
    public void render(DataSet data, boolean virtual, Bounds bounds) {
        BBox bbox = new BBox(bounds);
        this.ds = data;
        getSettings(virtual);

        /* draw tagged ways first, then untagged ways. takes
           time to iterate through list twice, OTOH does not
           require changing the colour while painting... */
        for (final OsmPrimitive osm: data.searchRelations(bbox)) {
            if (!osm.isDeleted() && !ds.isSelected(osm) && !osm.isDisabledAndHidden()) {
                osm.visit(this);
            }
        }

        for (final OsmPrimitive osm:data.searchWays(bbox)){
            if (!osm.isDeleted() && !ds.isSelected(osm) && !osm.isDisabledAndHidden() && osm.isTagged()) {
                osm.visit(this);
            }
        }
        displaySegments();

        for (final OsmPrimitive osm:data.searchWays(bbox)){
            if (!osm.isDeleted() && !ds.isSelected(osm) && !osm.isDisabledAndHidden() && !osm.isTagged()) {
                osm.visit(this);
            }
        }
        displaySegments();
        for (final OsmPrimitive osm : data.getSelected()) {
            if (!osm.isDeleted()) {
                osm.visit(this);
            }
        }
        displaySegments();

        for (final OsmPrimitive osm: data.searchNodes(bbox)) {
            if (!osm.isDeleted() && !ds.isSelected(osm) && !osm.isDisabledAndHidden())
            {
                osm.visit(this);
            }
        }
        drawVirtualNodes(data.searchWays(bbox), data.getHighlightedVirtualNodes());

        // draw highlighted way segments over the already drawn ways. Otherwise each
        // way would have to be checked if it contains a way segment to highlight when
        // in most of the cases there won't be more than one segment. Since the wireframe
        // renderer does not feature any transparency there should be no visual difference.
        for(final WaySegment wseg : data.getHighlightedWaySegments()) {
            drawSegment(nc.getPoint(wseg.getFirstNode()), nc.getPoint(wseg.getSecondNode()), highlightColor, false);
        }
        displaySegments();
    }

    private static final int max(int a, int b, int c, int d) {
        return Math.max(Math.max(a, b), Math.max(c, d));
    }

    /**
     * Draw a small rectangle.
     * White if selected (as always) or red otherwise.
     *
     * @param n The node to draw.
     */
    public void visit(Node n) {
        if (n.isIncomplete()) return;

        if (n.isHighlighted()) {
            drawNode(n, highlightColor, selectedNodeSize, fillSelectedNode);
        } else {
            Color color;

            if (isInactiveMode || n.isDisabled()) {
                color = inactiveColor;
            } else if (ds.isSelected(n)) {
                color = selectedColor;
            } else if (n.isConnectionNode()) {
                if (n.isTagged()) {
                    color = taggedConnectionColor;
                } else {
                    color = connectionColor;
                }
            } else {
                if (n.isTagged()) {
                    color = taggedColor;
                } else {
                    color = nodeColor;
                }
            }

            final int size = max((ds.isSelected(n) ? selectedNodeSize : 0),
                    (n.isTagged() ? taggedNodeSize : 0),
                    (n.isConnectionNode() ? connectionNodeSize : 0),
                    unselectedNodeSize);

            final boolean fill = (ds.isSelected(n) && fillSelectedNode) ||
            (n.isTagged() && fillTaggedNode) ||
            (n.isConnectionNode() && fillConnectionNode) ||
            fillUnselectedNode;

            drawNode(n, color, size, fill);
        }
    }

    public static boolean isLargeSegment(Point2D p1, Point2D p2, int space)
    {
        double xd = Math.abs(p1.getX()-p2.getX());
        double yd = Math.abs(p1.getY()-p2.getY());
        return (xd+yd > space);
    }

    public void drawVirtualNodes(Collection<Way> ways, Collection<WaySegment> highlightVirtualNodes) {
        if (virtualNodeSize == 0)
            return;
        // print normal virtual nodes
        GeneralPath path = new GeneralPath();
        for (Way osm : ways) {
            if (osm.isUsable() && !osm.isDisabledAndHidden() && !osm.isDisabled()) {
                visitVirtual(path, osm);
            }
        }
        g.setColor(nodeColor);
        g.draw(path);
        // print highlighted virtual nodes. Since only the color changes, simply
        // drawing them over the existing ones works fine (at least in their current
        // simple style)
        path = new GeneralPath();
        for (WaySegment wseg: highlightVirtualNodes){
            if (wseg.way.isUsable() && !wseg.way.isDisabled()) {
                visitVirtual(path, wseg.toWay());
            }
        }
        g.setColor(highlightColor);
        g.draw(path);
    }

    public void visitVirtual(GeneralPath path, Way w) {
        Iterator<Node> it = w.getNodes().iterator();
        if (it.hasNext()) {
            Point lastP = nc.getPoint(it.next());
            while(it.hasNext())
            {
                Point p = nc.getPoint(it.next());
                if(isSegmentVisible(lastP, p) && isLargeSegment(lastP, p, virtualNodeSpace))
                {
                    int x = (p.x+lastP.x)/2;
                    int y = (p.y+lastP.y)/2;
                    path.moveTo(x-virtualNodeSize, y);
                    path.lineTo(x+virtualNodeSize, y);
                    path.moveTo(x, y-virtualNodeSize);
                    path.lineTo(x, y+virtualNodeSize);
                }
                lastP = p;
            }
        }
    }

    /**
     * Draw a darkblue line for all segments.
     * @param w The way to draw.
     */
    public void visit(Way w) {
        if (w.isIncomplete() || w.getNodesCount() < 2)
            return;

        /* show direction arrows, if draw.segment.relevant_directions_only is not set, the way is tagged with a direction key
           (even if the tag is negated as in oneway=false) or the way is selected */

        boolean showThisDirectionArrow = ds.isSelected(w) || showDirectionArrow;
        /* head only takes over control if the option is true,
           the direction should be shown at all and not only because it's selected */
        boolean showOnlyHeadArrowOnly = showThisDirectionArrow && !ds.isSelected(w) && showHeadArrowOnly;
        Color wayColor;

        if (isInactiveMode || w.isDisabled()) {
            wayColor = inactiveColor;
        } else if(w.isHighlighted()) {
            wayColor = highlightColor;
        } else if(ds.isSelected(w)) {
            wayColor = selectedColor;
        } else if (!w.isTagged()) {
            wayColor = untaggedWayColor;
        } else {
            wayColor = dfltWayColor;
        }

        Iterator<Node> it = w.getNodes().iterator();
        if (it.hasNext()) {
            Point lastP = nc.getPoint(it.next());
            for (int orderNumber = 1; it.hasNext(); orderNumber++) {
                Point p = nc.getPoint(it.next());
                drawSegment(lastP, p, wayColor,
                        showOnlyHeadArrowOnly ? !it.hasNext() : showThisDirectionArrow);
                if (showOrderNumber && !isInactiveMode) {
                    drawOrderNumber(lastP, p, orderNumber);
                }
                lastP = p;
            }
        }
    }

    private Stroke relatedWayStroke = new BasicStroke(
            4, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_BEVEL);
    public void visit(Relation r) {
        if (r.isIncomplete()) return;

        Color col;
        if (isInactiveMode || r.isDisabled()) {
            col = inactiveColor;
        } else if (ds.isSelected(r)) {
            col = selectedColor;
        } else {
            col = relationColor;
        }
        g.setColor(col);

        for (RelationMember m : r.getMembers()) {
            if (m.getMember().isIncomplete() || m.getMember().isDeleted()) {
                continue;
            }

            if (m.isNode()) {
                Point p = nc.getPoint(m.getNode());
                if (p.x < 0 || p.y < 0
                        || p.x > nc.getWidth() || p.y > nc.getHeight()) {
                    continue;
                }

                g.drawOval(p.x-3, p.y-3, 6, 6);
            } else if (m.isWay()) {
                GeneralPath path = new GeneralPath();

                boolean first = true;
                for (Node n : m.getWay().getNodes()) {
                    if (n.isIncomplete() || n.isDeleted()) {
                        continue;
                    }
                    Point p = nc.getPoint(n);
                    if (first) {
                        path.moveTo(p.x, p.y);
                        first = false;
                    } else {
                        path.lineTo(p.x, p.y);
                    }
                }

                g.draw(relatedWayStroke.createStrokedShape(path));
            }
        }
    }

    @Override
    public void visit(Changeset cs) {/* ignore */}

    /**
     * Draw an number of the order of the two consecutive nodes within the
     * parents way
     */
    protected void drawOrderNumber(Point p1, Point p2, int orderNumber) {
        if (isSegmentVisible(p1, p2) && isLargeSegment(p1, p2, segmentNumberSpace)) {
            String on = Integer.toString(orderNumber);
            int strlen = on.length();
            int x = (p1.x+p2.x)/2 - 4*strlen;
            int y = (p1.y+p2.y)/2 + 4;

            if(virtualNodeSize != 0 && isLargeSegment(p1, p2, virtualNodeSpace))
            {
                y = (p1.y+p2.y)/2 - virtualNodeSize - 3;
            }

            displaySegments(); /* draw nodes on top! */
            Color c = g.getColor();
            g.setColor(backgroundColor);
            g.fillRect(x-1, y-12, 8*strlen+1, 14);
            g.setColor(c);
            g.drawString(on, x, y);
        }
    }

    /**
     * Draw the node as small rectangle with the given color.
     *
     * @param n     The node to draw.
     * @param color The color of the node.
     */
    public void drawNode(Node n, Color color, int size, boolean fill) {
        if (size > 1) {
            int radius = size / 2;
            Point p = nc.getPoint(n);
            if ((p.x < 0) || (p.y < 0) || (p.x > nc.getWidth())
                    || (p.y > nc.getHeight()))
                return;
            g.setColor(color);
            if (fill) {
                g.fillRect(p.x - radius, p.y - radius, size, size);
                g.drawRect(p.x - radius, p.y - radius, size, size);
            } else {
                g.drawRect(p.x - radius, p.y - radius, size, size);
            }
        }
    }

    private static final double PHI = Math.toRadians(20);
    private static final double cosPHI = Math.cos(PHI);
    private static final double sinPHI = Math.sin(PHI);

    protected void drawSegment(GeneralPath path, Point p1, Point p2, boolean showDirection) {
        Rectangle bounds = g.getClipBounds();
        bounds.grow(100, 100);                  // avoid arrow heads at the border
        LineClip clip = new LineClip(p1, p2, bounds);
        if (clip.execute()) {
            p1 = clip.getP1();
            p2 = clip.getP2();
            path.moveTo(p1.x, p1.y);
            path.lineTo(p2.x, p2.y);

            if (showDirection) {
                final double l =  10. / p1.distance(p2);

                final double sx = l * (p1.x - p2.x);
                final double sy = l * (p1.y - p2.y);

                path.lineTo (p2.x + (int) Math.round(cosPHI * sx - sinPHI * sy), p2.y + (int) Math.round(sinPHI * sx + cosPHI * sy));
                path.moveTo (p2.x + (int) Math.round(cosPHI * sx + sinPHI * sy), p2.y + (int) Math.round(- sinPHI * sx + cosPHI * sy));
                path.lineTo(p2.x, p2.y);
            }
        }
    }

    /**
     * Draw a line with the given color.
     */
    protected void drawSegment(Point p1, Point p2, Color col, boolean showDirection) {
        if (col != currentColor) {
            displaySegments(col);
        }
        drawSegment(currentPath, p1, p2, showDirection);
    }

    protected boolean isSegmentVisible(Point p1, Point p2) {
        if ((p1.x < 0) && (p2.x < 0)) return false;
        if ((p1.y < 0) && (p2.y < 0)) return false;
        if ((p1.x > nc.getWidth()) && (p2.x > nc.getWidth())) return false;
        if ((p1.y > nc.getHeight()) && (p2.y > nc.getHeight())) return false;
        return true;
    }

    protected boolean isPolygonVisible(Polygon polygon) {
        Rectangle bounds = polygon.getBounds();
        if (bounds.width == 0 && bounds.height == 0) return false;
        if (bounds.x > nc.getWidth()) return false;
        if (bounds.y > nc.getHeight()) return false;
        if (bounds.x + bounds.width < 0) return false;
        if (bounds.y + bounds.height < 0) return false;
        return true;
    }

    protected void displaySegments() {
        displaySegments(null);
    }
    protected void displaySegments(Color newColor) {
        if (currentPath != null) {
            g.setColor(currentColor);
            g.draw(currentPath);
            currentPath = new GeneralPath();
            currentColor = newColor;
        }
    }
}
