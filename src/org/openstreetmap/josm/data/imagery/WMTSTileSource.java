// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.imagery;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.GridBagLayout;
import java.awt.Point;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.Stack;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.AbstractTableModel;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.openstreetmap.gui.jmapviewer.Coordinate;
import org.openstreetmap.gui.jmapviewer.Tile;
import org.openstreetmap.gui.jmapviewer.TileXY;
import org.openstreetmap.gui.jmapviewer.interfaces.ICoordinate;
import org.openstreetmap.gui.jmapviewer.interfaces.TemplatedTileSource;
import org.openstreetmap.gui.jmapviewer.tilesources.AbstractTMSTileSource;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.projection.Projection;
import org.openstreetmap.josm.data.projection.Projections;
import org.openstreetmap.josm.gui.ExtendedDialog;
import org.openstreetmap.josm.gui.layer.NativeScaleLayer.ScaleList;
import org.openstreetmap.josm.io.CachedFile;
import org.openstreetmap.josm.tools.CheckParameterUtil;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.Utils;

/**
 * Tile Source handling WMS providers
 *
 * @author Wiktor Niesiobędzki
 * @since 8526
 */
public class WMTSTileSource extends AbstractTMSTileSource implements TemplatedTileSource {
    private static final String PATTERN_HEADER  = "\\{header\\(([^,]+),([^}]+)\\)\\}";

    private static final String URL_GET_ENCODING_PARAMS = "SERVICE=WMTS&REQUEST=GetTile&VERSION=1.0.0&LAYER={layer}&STYLE={style}&"
            + "FORMAT={format}&tileMatrixSet={TileMatrixSet}&tileMatrix={TileMatrix}&tileRow={TileRow}&tileCol={TileCol}";

    private static final String[] ALL_PATTERNS = {
        PATTERN_HEADER,
    };

    private static final String OWS_NS_URL = "http://www.opengis.net/ows/1.1";
    private static final String WMTS_NS_URL = "http://www.opengis.net/wmts/1.0";
    private static final String XLINK_NS_URL = "http://www.w3.org/1999/xlink";

    private static class TileMatrix {
        private String identifier;
        private double scaleDenominator;
        private EastNorth topLeftCorner;
        private int tileWidth;
        private int tileHeight;
        private int matrixWidth = -1;
        private int matrixHeight = -1;
    }

    private static class TileMatrixSetBuilder {
        SortedSet<TileMatrix> tileMatrix = new TreeSet<>(new Comparator<TileMatrix>() {
            @Override
            public int compare(TileMatrix o1, TileMatrix o2) {
                // reverse the order, so it will be from greatest (lowest zoom level) to lowest value (highest zoom level)
                return -1 * Double.compare(o1.scaleDenominator, o2.scaleDenominator);
            }
        }); // sorted by zoom level
        private String crs;
        private String identifier;

        TileMatrixSet build() {
            return new TileMatrixSet(this);
        }
    }

    private static class TileMatrixSet {

        private final List<TileMatrix> tileMatrix;
        private final String crs;
        private final String identifier;

        TileMatrixSet(TileMatrixSet tileMatrixSet) {
            if (tileMatrixSet != null) {
                tileMatrix = new ArrayList<>(tileMatrixSet.tileMatrix);
                crs = tileMatrixSet.crs;
                identifier = tileMatrixSet.identifier;
            } else {
                tileMatrix = Collections.emptyList();
                crs = null;
                identifier = null;
            }
        }

        TileMatrixSet(TileMatrixSetBuilder builder) {
            tileMatrix = new ArrayList<>(builder.tileMatrix);
            crs = builder.crs;
            identifier = builder.identifier;
        }

    }

    private static class Layer {
        private String format;
        private String name;
        private TileMatrixSet tileMatrixSet;
        private String baseUrl;
        private String style;
        public Collection<String> tileMatrixSetLinks = new ArrayList<>();

        Layer(Layer l) {
            if (l != null) {
                format = l.format;
                name = l.name;
                baseUrl = l.baseUrl;
                style = l.style;
                tileMatrixSet = new TileMatrixSet(l.tileMatrixSet);
            }
        }

        Layer() {
        }
    }

    private enum TransferMode {
        KVP("KVP"),
        REST("RESTful");

        private final String typeString;

        TransferMode(String urlString) {
            this.typeString = urlString;
        }

        private String getTypeString() {
            return typeString;
        }

        private static TransferMode fromString(String s) {
            for (TransferMode type : TransferMode.values()) {
                if (type.getTypeString().equals(s)) {
                    return type;
                }
            }
            return null;
        }
    }

    private static final class SelectLayerDialog extends ExtendedDialog {
        private final transient Layer[] layers;
        private final JTable list;

        SelectLayerDialog(Collection<Layer> layers) {
            super(Main.parent, tr("Select WMTS layer"), new String[]{tr("Add layers"), tr("Cancel")});
            this.layers = layers.toArray(new Layer[]{});
            //getLayersTable(layers, Main.getProjection())
            this.list = new JTable(
                    new AbstractTableModel() {
                        @Override
                        public Object getValueAt(int rowIndex, int columnIndex) {
                            switch (columnIndex) {
                            case 0:
                                return SelectLayerDialog.this.layers[rowIndex].name;
                            case 1:
                                return SelectLayerDialog.this.layers[rowIndex].tileMatrixSet.crs;
                            case 2:
                                return SelectLayerDialog.this.layers[rowIndex].tileMatrixSet.identifier;
                            default:
                                throw new IllegalArgumentException();
                            }
                        }

                        @Override
                        public int getRowCount() {
                            return SelectLayerDialog.this.layers.length;
                        }

                        @Override
                        public int getColumnCount() {
                            return 3;
                        }

                        @Override
                        public String getColumnName(int column) {
                            switch (column) {
                            case 0: return tr("Layer name");
                            case 1: return tr("Projection");
                            case 2: return tr("Matrix set identifier");
                            default:
                                throw new IllegalArgumentException();
                            }
                        }

                        @Override
                        public boolean isCellEditable(int row, int column) {
                            return false;
                        }
                    });
            this.list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            this.list.setRowSelectionAllowed(true);
            this.list.setColumnSelectionAllowed(false);
            JPanel panel = new JPanel(new GridBagLayout());
            panel.add(new JScrollPane(this.list), GBC.eol().fill());
            setContent(panel);
        }

        public Layer getSelectedLayer() {
            int index = list.getSelectedRow();
            if (index < 0) {
                return null; //nothing selected
            }
            return layers[index];
        }
    }

    private final Map<String, String> headers = new ConcurrentHashMap<>();
    private Collection<Layer> layers;
    private Layer currentLayer;
    private TileMatrixSet currentTileMatrixSet;
    private double crsScale;
    private TransferMode transferMode;

    private ScaleList nativeScaleList;

    /**
     * Creates a tile source based on imagery info
     * @param info imagery info
     * @throws IOException if any I/O error occurs
     */
    public WMTSTileSource(ImageryInfo info) throws IOException {
        super(info);
        this.baseUrl = normalizeCapabilitiesUrl(handleTemplate(info.getUrl()));
        this.layers = getCapabilities();
        if (this.layers.isEmpty())
            throw new IllegalArgumentException(tr("No layers defined by getCapabilities document: {0}", info.getUrl()));
    }

    private Layer userSelectLayer(Collection<Layer> layers) {
        if (layers.size() == 1)
            return layers.iterator().next();
        Layer ret = null;

        final SelectLayerDialog layerSelection = new SelectLayerDialog(layers);
        if (layerSelection.showDialog().getValue() == 1) {
            ret = layerSelection.getSelectedLayer();
            // TODO: save layer information into ImageryInfo / ImageryPreferences?
        }
        if (ret == null) {
            // user canceled operation or did not choose any layer
            throw new IllegalArgumentException(tr("No layer selected"));
        }
        return ret;
    }

    private String handleTemplate(String url) {
        Pattern pattern = Pattern.compile(PATTERN_HEADER);
        StringBuffer output = new StringBuffer(); // NOSONAR
        Matcher matcher = pattern.matcher(url);
        while (matcher.find()) {
            this.headers.put(matcher.group(1), matcher.group(2));
            matcher.appendReplacement(output, "");
        }
        matcher.appendTail(output);
        return output.toString();
    }

    private Collection<Layer> getCapabilities() {
        XMLInputFactory factory = XMLInputFactory.newFactory();
        // do not try to load external entities, nor validate the XML
        factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, Boolean.FALSE);
        factory.setProperty(XMLInputFactory.IS_VALIDATING, Boolean.FALSE);
        factory.setProperty(XMLInputFactory.SUPPORT_DTD, Boolean.FALSE);

        try (CachedFile cf = new CachedFile(baseUrl); InputStream in = cf.setHttpHeaders(headers).
                setMaxAge(7 * CachedFile.DAYS).
                setCachingStrategy(CachedFile.CachingStrategy.IfModifiedSince).
                getInputStream()) {
            byte[] data = Utils.readBytesFromStream(in);
            if (data == null || data.length == 0) {
                throw new IllegalArgumentException("Could not read data from: " + baseUrl);
            }
            XMLStreamReader reader = factory.createXMLStreamReader(new ByteArrayInputStream(data));

            Collection<Layer> ret = new ArrayList<>();
            for (int event = reader.getEventType(); reader.hasNext(); event = reader.next()) {
                if (event == XMLStreamReader.START_ELEMENT) {
                    if (new QName(OWS_NS_URL, "OperationsMetadata").equals(reader.getName())) {
                        parseOperationMetadata(reader);
                    }

                    if (new QName(WMTS_NS_URL, "Contents").equals(reader.getName())) {
                        ret = parseContents(reader);
                    }
                }
            }
            return ret;
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
    }

    /**
     * Parse Contents tag. Renturns when reader reaches Contents closing tag
     *
     * @param reader StAX reader instance
     * @return collection of layers within contents with properly linked TileMatrixSets
     * @throws XMLStreamException See {@link XMLStreamReader}
     */
    private static Collection<Layer> parseContents(XMLStreamReader reader) throws XMLStreamException {
        Map<String, TileMatrixSet> matrixSetById = new ConcurrentHashMap<>();
        Collection<Layer> layers = new ArrayList<>();
        for (int event = reader.getEventType();
                reader.hasNext() && !(event == XMLStreamReader.END_ELEMENT && new QName(WMTS_NS_URL, "Contents").equals(reader.getName()));
                event = reader.next()) {
            if (event == XMLStreamReader.START_ELEMENT) {
                if (new QName(WMTS_NS_URL, "Layer").equals(reader.getName())) {
                    layers.add(parseLayer(reader));
                }
                if (new QName(WMTS_NS_URL, "TileMatrixSet").equals(reader.getName())) {
                    TileMatrixSet entry = parseTileMatrixSet(reader);
                    matrixSetById.put(entry.identifier, entry);
                }
            }
        }
        Collection<Layer> ret = new ArrayList<>();
        // link layers to matrix sets
        for (Layer l: layers) {
            for (String tileMatrixId: l.tileMatrixSetLinks) {
                Layer newLayer = new Layer(l); // create a new layer object for each tile matrix set supported
                newLayer.tileMatrixSet = matrixSetById.get(tileMatrixId);
                ret.add(newLayer);
            }
        }
        return ret;
    }

    /**
     * Parse Layer tag. Returns when reader will reach Layer closing tag
     *
     * @param reader StAX reader instance
     * @return Layer object, with tileMatrixSetLinks and no tileMatrixSet attribute set.
     * @throws XMLStreamException See {@link XMLStreamReader}
     */
    private static Layer parseLayer(XMLStreamReader reader) throws XMLStreamException {
        Layer layer = new Layer();
        Stack<QName> tagStack = new Stack<>();

        for (int event = reader.getEventType();
                reader.hasNext() && !(event == XMLStreamReader.END_ELEMENT && new QName(WMTS_NS_URL, "Layer").equals(reader.getName()));
                event = reader.next()) {
            if (event == XMLStreamReader.START_ELEMENT) {
                tagStack.push(reader.getName());
                if (tagStack.size() == 2) {
                    if (new QName(WMTS_NS_URL, "Format").equals(reader.getName())) {
                        layer.format = reader.getElementText();
                    } else if (new QName(OWS_NS_URL, "Identifier").equals(reader.getName())) {
                        layer.name = reader.getElementText();
                    } else if (new QName(WMTS_NS_URL, "ResourceURL").equals(reader.getName()) &&
                            "tile".equals(reader.getAttributeValue("", "resourceType"))) {
                        layer.baseUrl = reader.getAttributeValue("", "template");
                    } else if (new QName(WMTS_NS_URL, "Style").equals(reader.getName()) &&
                            "true".equals(reader.getAttributeValue("", "isDefault"))) {
                        if (moveReaderToTag(reader, new QName[] {new QName(OWS_NS_URL, "Identifier")})) {
                            layer.style = reader.getElementText();
                            tagStack.push(reader.getName()); // keep tagStack in sync
                        }
                    } else if (new QName(WMTS_NS_URL, "TileMatrixSetLink").equals(reader.getName())) {
                        layer.tileMatrixSetLinks.add(praseTileMatrixSetLink(reader));
                    } else {
                        moveReaderToEndCurrentTag(reader);
                    }
                }
            }
            // need to get event type from reader, as parsing might have change position of reader
            if (reader.getEventType() == XMLStreamReader.END_ELEMENT) {
                QName start = tagStack.pop();
                if (!start.equals(reader.getName())) {
                    throw new IllegalStateException(tr("WMTS Parser error - start element {0} has different name than end element {2}",
                            start, reader.getName()));
                }
            }
        }
        if (layer.style == null) {
            layer.style = "";
        }
        return layer;
    }

    /**
     * Moves the reader to the closing tag of current tag.
     * @param reader XML stream reader positioned on XMLStreamReader.START_ELEMENT
     * @throws XMLStreamException when parse exception occurs
     */
    private static void moveReaderToEndCurrentTag(XMLStreamReader reader) throws XMLStreamException {
        int level = 0;
        QName tag = reader.getName();
        for (int event = reader.getEventType(); reader.hasNext(); event = reader.next()) {
            switch (event) {
            case XMLStreamReader.START_ELEMENT:
                level += 1;
                break;
            case XMLStreamReader.END_ELEMENT:
                level -= 1;
                if (level == 0 && tag.equals(reader.getName())) {
                    return;
                }
            }
            if (level < 0) {
                throw new IllegalStateException("WMTS Parser error - moveReaderToEndCurrentTag failed to find closing tag");
            }
        }
        throw new IllegalStateException("WMTS Parser error - moveReaderToEndCurrentTag failed to find closing tag");

    }

    /**
     * Gets TileMatrixSetLink value. Returns when reader is on TileMatrixSetLink closing tag
     *
     * @param reader StAX reader instance
     * @return TileMatrixSetLink identifier
     * @throws XMLStreamException See {@link XMLStreamReader}
     */
    private static String praseTileMatrixSetLink(XMLStreamReader reader) throws XMLStreamException {
        String ret = null;
        for (int event = reader.getEventType();
                reader.hasNext() && !(event == XMLStreamReader.END_ELEMENT &&
                        new QName(WMTS_NS_URL, "TileMatrixSetLink").equals(reader.getName()));
                event = reader.next()) {
            if (event == XMLStreamReader.START_ELEMENT && new QName(WMTS_NS_URL, "TileMatrixSet").equals(reader.getName())) {
                ret = reader.getElementText();
            }
        }
        return ret;
    }

    /**
     * Parses TileMatrixSet section. Returns when reader is on TileMatrixSet closing tag
     * @param reader StAX reader instance
     * @return TileMatrixSet object
     * @throws XMLStreamException See {@link XMLStreamReader}
     */
    private static TileMatrixSet parseTileMatrixSet(XMLStreamReader reader) throws XMLStreamException {
        TileMatrixSetBuilder matrixSet = new TileMatrixSetBuilder();
        for (int event = reader.getEventType();
                reader.hasNext() && !(event == XMLStreamReader.END_ELEMENT && new QName(WMTS_NS_URL, "TileMatrixSet").equals(reader.getName()));
                event = reader.next()) {
                    if (event == XMLStreamReader.START_ELEMENT) {
                        if (new QName(OWS_NS_URL, "Identifier").equals(reader.getName())) {
                            matrixSet.identifier = reader.getElementText();
                        }
                        if (new QName(OWS_NS_URL, "SupportedCRS").equals(reader.getName())) {
                            matrixSet.crs = crsToCode(reader.getElementText());
                        }
                        if (new QName(WMTS_NS_URL, "TileMatrix").equals(reader.getName())) {
                            matrixSet.tileMatrix.add(parseTileMatrix(reader, matrixSet.crs));
                        }
                    }
        }
        return matrixSet.build();
    }

    /**
     * Parses TileMatrix section. Returns when reader is on TileMatrix closing tag.
     * @param reader StAX reader instance
     * @param matrixCrs projection used by this matrix
     * @return TileMatrix object
     * @throws XMLStreamException See {@link XMLStreamReader}
     */
    private static TileMatrix parseTileMatrix(XMLStreamReader reader, String matrixCrs) throws XMLStreamException {
        Projection matrixProj = Projections.getProjectionByCode(matrixCrs);
        TileMatrix ret = new TileMatrix();

        if (matrixProj == null) {
            // use current projection if none found. Maybe user is using custom string
            matrixProj = Main.getProjection();
        }
        for (int event = reader.getEventType();
                reader.hasNext() && !(event == XMLStreamReader.END_ELEMENT && new QName(WMTS_NS_URL, "TileMatrix").equals(reader.getName()));
                event = reader.next()) {
            if (event == XMLStreamReader.START_ELEMENT) {
                if (new QName(OWS_NS_URL, "Identifier").equals(reader.getName())) {
                    ret.identifier = reader.getElementText();
                }
                if (new QName(WMTS_NS_URL, "ScaleDenominator").equals(reader.getName())) {
                    ret.scaleDenominator = Double.parseDouble(reader.getElementText());
                }
                if (new QName(WMTS_NS_URL, "TopLeftCorner").equals(reader.getName())) {
                    String[] topLeftCorner = reader.getElementText().split(" ");
                    if (matrixProj.switchXY()) {
                        ret.topLeftCorner = new EastNorth(Double.parseDouble(topLeftCorner[1]), Double.parseDouble(topLeftCorner[0]));
                    } else {
                        ret.topLeftCorner = new EastNorth(Double.parseDouble(topLeftCorner[0]), Double.parseDouble(topLeftCorner[1]));
                    }
                }
                if (new QName(WMTS_NS_URL, "TileHeight").equals(reader.getName())) {
                    ret.tileHeight = Integer.parseInt(reader.getElementText());
                }
                if (new QName(WMTS_NS_URL, "TileWidth").equals(reader.getName())) {
                    ret.tileWidth = Integer.parseInt(reader.getElementText());
                }
                if (new QName(WMTS_NS_URL, "MatrixHeight").equals(reader.getName())) {
                    ret.matrixHeight = Integer.parseInt(reader.getElementText());
                }
                if (new QName(WMTS_NS_URL, "MatrixWidth").equals(reader.getName())) {
                    ret.matrixWidth = Integer.parseInt(reader.getElementText());
                }
            }
        }
        if (ret.tileHeight != ret.tileWidth) {
            throw new AssertionError(tr("Only square tiles are supported. {0}x{1} returned by server for TileMatrix identifier {2}",
                    ret.tileHeight, ret.tileWidth, ret.identifier));
        }
        return ret;
    }

    /**
     * Parses OperationMetadata section. Returns when reader is on OperationsMetadata closing tag.
     * Sets this.baseUrl and this.transferMode
     *
     * @param reader StAX reader instance
     * @throws XMLStreamException See {@link XMLStreamReader}
     */
    private void parseOperationMetadata(XMLStreamReader reader) throws XMLStreamException {
        for (int event = reader.getEventType();
                reader.hasNext() && !(event == XMLStreamReader.END_ELEMENT &&
                        new QName(OWS_NS_URL, "OperationsMetadata").equals(reader.getName()));
                event = reader.next()) {
            if (event == XMLStreamReader.START_ELEMENT) {
                if (new QName(OWS_NS_URL, "Operation").equals(reader.getName()) && "GetTile".equals(reader.getAttributeValue("", "name")) &&
                        moveReaderToTag(reader, new QName[]{
                                new QName(OWS_NS_URL, "DCP"),
                                new QName(OWS_NS_URL, "HTTP"),
                                new QName(OWS_NS_URL, "Get"),

                        })) {
                    this.baseUrl = reader.getAttributeValue(XLINK_NS_URL, "href");
                    this.transferMode = getTransferMode(reader);
                }
            }
        }
    }

    /**
     * Parses Operation[@name='GetTile']/DCP/HTTP/Get section. Returns when reader is on Get closing tag.
     * @param reader StAX reader instance
     * @return TransferMode coded in this section
     * @throws XMLStreamException See {@link XMLStreamReader}
     */
    private static TransferMode getTransferMode(XMLStreamReader reader) throws XMLStreamException {
        QName getQname = new QName(OWS_NS_URL, "Get");

        Utils.ensure(getQname.equals(reader.getName()), "WMTS Parser state invalid. Expected element %s, got %s",
                getQname, reader.getName());
        for (int event = reader.getEventType();
                reader.hasNext() && !(event == XMLStreamReader.END_ELEMENT && getQname.equals(reader.getName()));
                event = reader.next()) {
            if (event == XMLStreamReader.START_ELEMENT && new QName(OWS_NS_URL, "Constraint").equals(reader.getName())
             && "GetEncoding".equals(reader.getAttributeValue("", "name"))) {
                moveReaderToTag(reader, new QName[]{
                        new QName(OWS_NS_URL, "AllowedValues"),
                        new QName(OWS_NS_URL, "Value")
                });
                return TransferMode.fromString(reader.getElementText());
            }
        }
        return null;
    }

    /**
     * Moves reader to first occurrence of the structure equivalent of Xpath tags[0]/tags[1]../tags[n]. If fails to find
     * moves the reader to the closing tag of current tag
     *
     * @param reader StAX reader instance
     * @param tags array of tags
     * @return true if tag was found, false otherwise
     * @throws XMLStreamException See {@link XMLStreamReader}
     */
    private static boolean moveReaderToTag(XMLStreamReader reader, QName[] tags) throws XMLStreamException {
        QName stopTag = reader.getName();
        int currentLevel = 0;
        QName searchTag = tags[currentLevel];
        QName parentTag = null;
        QName skipTag = null;

        for (int event = 0; //skip current element, so we will not skip it as a whole
                reader.hasNext() && !(event == XMLStreamReader.END_ELEMENT && stopTag.equals(reader.getName()));
                event = reader.next()) {
            if (event == XMLStreamReader.END_ELEMENT && skipTag != null && skipTag.equals(reader.getName())) {
                skipTag = null;
            }
            if (skipTag == null) {
                if (event == XMLStreamReader.START_ELEMENT) {
                    if (searchTag.equals(reader.getName())) {
                        currentLevel += 1;
                        if (currentLevel >= tags.length) {
                            return true; // found!
                        }
                        parentTag = searchTag;
                        searchTag = tags[currentLevel];
                    } else {
                        skipTag = reader.getName();
                    }
                }

                if (event == XMLStreamReader.END_ELEMENT && parentTag != null && parentTag.equals(reader.getName())) {
                    currentLevel -= 1;
                    searchTag = parentTag;
                    if (currentLevel >= 0) {
                        parentTag = tags[currentLevel];
                    } else {
                        parentTag = null;
                    }
                }
            }
        }
        return false;
    }

    private static String normalizeCapabilitiesUrl(String url) throws MalformedURLException {
        URL inUrl = new URL(url);
        URL ret = new URL(inUrl.getProtocol(), inUrl.getHost(), inUrl.getPort(), inUrl.getFile());
        return ret.toExternalForm();
    }

    private static String crsToCode(String crsIdentifier) {
        if (crsIdentifier.startsWith("urn:ogc:def:crs:")) {
            return crsIdentifier.replaceFirst("urn:ogc:def:crs:([^:]*):.*:(.*)$", "$1:$2");
        }
        return crsIdentifier;
    }

    /**
     * Initializes projection for this TileSource with projection
     * @param proj projection to be used by this TileSource
     */
    public void initProjection(Projection proj) {
        // getLayers will return only layers matching the name, if the user already choose the layer
        // so we will not ask the user again to chose the layer, if he just changes projection
        Collection<Layer> candidates = getLayers(currentLayer != null ? currentLayer.name : null, proj.toCode());
        if (!candidates.isEmpty()) {
            Layer newLayer = userSelectLayer(candidates);
            if (newLayer != null) {
                this.currentTileMatrixSet = newLayer.tileMatrixSet;
                this.currentLayer = newLayer;
                Collection<Double> scales = new ArrayList<>(currentTileMatrixSet.tileMatrix.size());
                for (TileMatrix tileMatrix : currentTileMatrixSet.tileMatrix) {
                    scales.add(tileMatrix.scaleDenominator * 0.28e-03);
                }
                this.nativeScaleList = new ScaleList(scales);
            }
        }
        this.crsScale = getTileSize() * 0.28e-03 / proj.getMetersPerUnit();
    }

    /**
     *
     * @param name of the layer to match
     * @param projectionCode projection code to match
     * @return Collection of layers matching the name of the layer and projection, or only projection if name is not provided
     */
    private Collection<Layer> getLayers(String name, String projectionCode) {
        Collection<Layer> ret = new ArrayList<>();
        if (this.layers != null) {
            for (Layer layer: this.layers) {
                if ((name == null || name.equals(layer.name)) && (projectionCode == null || projectionCode.equals(layer.tileMatrixSet.crs))) {
                    ret.add(layer);
                }
            }
        }
        return ret;
    }

    @Override
    public int getTileSize() {
        // no support for non-square tiles (tileHeight != tileWidth)
        // and for different tile sizes at different zoom levels
        Collection<Layer> layers = getLayers(null, Main.getProjection().toCode());
        if (!layers.isEmpty()) {
            return layers.iterator().next().tileMatrixSet.tileMatrix.get(0).tileHeight;
        }
        // if no layers is found, fallback to default mercator tile size. Maybe it will work
        Main.warn("WMTS: Could not determine tile size. Using default tile size of: {0}", getDefaultTileSize());
        return getDefaultTileSize();
    }

    @Override
    public String getTileUrl(int zoom, int tilex, int tiley) {
        String url;
        if (currentLayer == null) {
            return "";
        }

        if (currentLayer.baseUrl != null && transferMode == null) {
            url = currentLayer.baseUrl;
        } else {
            switch (transferMode) {
            case KVP:
                url = baseUrl + URL_GET_ENCODING_PARAMS;
                break;
            case REST:
                url = currentLayer.baseUrl;
                break;
            default:
                url = "";
                break;
            }
        }

        TileMatrix tileMatrix = getTileMatrix(zoom);

        if (tileMatrix == null) {
            return ""; // no matrix, probably unsupported CRS selected.
        }

        return url.replaceAll("\\{layer\\}", this.currentLayer.name)
                .replaceAll("\\{format\\}", this.currentLayer.format)
                .replaceAll("\\{TileMatrixSet\\}", this.currentTileMatrixSet.identifier)
                .replaceAll("\\{TileMatrix\\}", tileMatrix.identifier)
                .replaceAll("\\{TileRow\\}", Integer.toString(tiley))
                .replaceAll("\\{TileCol\\}", Integer.toString(tilex))
                .replaceAll("(?i)\\{style\\}", this.currentLayer.style);
    }

    /**
     *
     * @param zoom zoom level
     * @return TileMatrix that's working on this zoom level
     */
    private TileMatrix getTileMatrix(int zoom) {
        if (zoom > getMaxZoom()) {
            return null;
        }
        if (zoom < 0) {
            return null;
        }
        return this.currentTileMatrixSet.tileMatrix.get(zoom);
    }

    @Override
    public double getDistance(double lat1, double lon1, double lat2, double lon2) {
        throw new UnsupportedOperationException("Not implemented");
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
        TileMatrix matrix = getTileMatrix(zoom);
        if (matrix == null) {
            return Main.getProjection().getWorldBoundsLatLon().getCenter().toCoordinate();
        }
        double scale = matrix.scaleDenominator * this.crsScale;
        EastNorth ret = new EastNorth(matrix.topLeftCorner.east() + x * scale, matrix.topLeftCorner.north() - y * scale);
        return Main.getProjection().eastNorth2latlon(ret).toCoordinate();
    }

    @Override
    public TileXY latLonToTileXY(double lat, double lon, int zoom) {
        TileMatrix matrix = getTileMatrix(zoom);
        if (matrix == null) {
            return new TileXY(0, 0);
        }

        Projection proj = Main.getProjection();
        EastNorth enPoint = proj.latlon2eastNorth(new LatLon(lat, lon));
        double scale = matrix.scaleDenominator * this.crsScale;
        return new TileXY(
                (enPoint.east() - matrix.topLeftCorner.east()) / scale,
                (matrix.topLeftCorner.north() - enPoint.north()) / scale
                );
    }

    @Override
    public TileXY latLonToTileXY(ICoordinate point, int zoom) {
        return latLonToTileXY(point.getLat(),  point.getLon(), zoom);
    }

    @Override
    public int getTileXMax(int zoom) {
        return getTileXMax(zoom, Main.getProjection());
    }

    @Override
    public int getTileXMin(int zoom) {
        return 0;
    }

    @Override
    public int getTileYMax(int zoom) {
        return getTileYMax(zoom, Main.getProjection());
    }

    @Override
    public int getTileYMin(int zoom) {
        return 0;
    }

    @Override
    public Point latLonToXY(double lat, double lon, int zoom) {
        TileMatrix matrix = getTileMatrix(zoom);
        if (matrix == null) {
            return new Point(0, 0);
        }
        double scale = matrix.scaleDenominator * this.crsScale;
        EastNorth point = Main.getProjection().latlon2eastNorth(new LatLon(lat, lon));
        return new Point(
                    (int) Math.round((point.east() - matrix.topLeftCorner.east())   / scale),
                    (int) Math.round((matrix.topLeftCorner.north() - point.north()) / scale)
                );
    }

    @Override
    public Point latLonToXY(ICoordinate point, int zoom) {
        return latLonToXY(point.getLat(), point.getLon(), zoom);
    }

    @Override
    public Coordinate xyToLatLon(Point point, int zoom) {
        return xyToLatLon(point.x, point.y, zoom);
    }

    @Override
    public Coordinate xyToLatLon(int x, int y, int zoom) {
        TileMatrix matrix = getTileMatrix(zoom);
        if (matrix == null) {
            return new Coordinate(0, 0);
        }
        double scale = matrix.scaleDenominator * this.crsScale;
        Projection proj = Main.getProjection();
        EastNorth ret = new EastNorth(
                matrix.topLeftCorner.east() + x * scale,
                matrix.topLeftCorner.north() - y * scale
                );
        LatLon ll = proj.eastNorth2latlon(ret);
        return new Coordinate(ll.lat(), ll.lon());
    }

    @Override
    public Map<String, String> getHeaders() {
        return headers;
    }

    @Override
    public int getMaxZoom() {
        if (this.currentTileMatrixSet != null) {
            return this.currentTileMatrixSet.tileMatrix.size()-1;
        }
        return 0;
    }

    @Override
    public String getTileId(int zoom, int tilex, int tiley) {
        return getTileUrl(zoom, tilex, tiley);
    }

    /**
     * Checks if url is acceptable by this Tile Source
     * @param url URL to check
     */
    public static void checkUrl(String url) {
        CheckParameterUtil.ensureParameterNotNull(url, "url");
        Matcher m = Pattern.compile("\\{[^}]*\\}").matcher(url);
        while (m.find()) {
            boolean isSupportedPattern = false;
            for (String pattern : ALL_PATTERNS) {
                if (m.group().matches(pattern)) {
                    isSupportedPattern = true;
                    break;
                }
            }
            if (!isSupportedPattern) {
                throw new IllegalArgumentException(
                        tr("{0} is not a valid WMS argument. Please check this server URL:\n{1}", m.group(), url));
            }
        }
    }

    /**
     * @return set of projection codes that this TileSource supports
     */
    public Set<String> getSupportedProjections() {
        Set<String> ret = new HashSet<>();
        if (currentLayer == null) {
            for (Layer layer: this.layers) {
                ret.add(layer.tileMatrixSet.crs);
            }
        } else {
            for (Layer layer: this.layers) {
                if (currentLayer.name.equals(layer.name)) {
                    ret.add(layer.tileMatrixSet.crs);
                }
            }
        }
        return ret;
    }

    private int getTileYMax(int zoom, Projection proj) {
        TileMatrix matrix = getTileMatrix(zoom);
        if (matrix == null) {
            return 0;
        }

        if (matrix.matrixHeight != -1) {
            return matrix.matrixHeight;
        }

        double scale = matrix.scaleDenominator * this.crsScale;
        EastNorth min = matrix.topLeftCorner;
        EastNorth max = proj.latlon2eastNorth(proj.getWorldBoundsLatLon().getMax());
        return (int) Math.ceil(Math.abs(max.north() - min.north()) / scale);
    }

    private int getTileXMax(int zoom, Projection proj) {
        TileMatrix matrix = getTileMatrix(zoom);
        if (matrix == null) {
            return 0;
        }
        if (matrix.matrixWidth != -1) {
            return matrix.matrixWidth;
        }

        double scale = matrix.scaleDenominator * this.crsScale;
        EastNorth min = matrix.topLeftCorner;
        EastNorth max = proj.latlon2eastNorth(proj.getWorldBoundsLatLon().getMax());
        return (int) Math.ceil(Math.abs(max.east() - min.east()) / scale);
    }

    /**
     * Get native scales of tile source.
     * @return {@link ScaleList} of native scales
     */
    public ScaleList getNativeScales() {
        return nativeScaleList;
    }

}
