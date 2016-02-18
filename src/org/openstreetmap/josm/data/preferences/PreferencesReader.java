// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.preferences;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.xml.XMLConstants;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.io.CachedFile;
import org.xml.sax.SAXException;

/**
 * Loads preferences from XML.
 */
public class PreferencesReader {

    private static final String XSI_NS = "http://www.w3.org/2001/XMLSchema-instance";

    private final SortedMap<String, Setting<?>> settings = new TreeMap<>();
    private XMLStreamReader parser;
    private int version = 0;
    private Reader reader;
    private File file;

    private final boolean defaults;

    /**
     * Constructs a new {@code PreferencesReader}.
     * @param file the file
     * @param defaults true when reading from the cache file for default preferences,
     * false for the regular preferences config file
     * @throws IOException if any I/O error occurs
     * @throws XMLStreamException if any XML stream error occurs
     */
    public PreferencesReader(File file, boolean defaults) throws IOException, XMLStreamException {
        this.defaults = defaults;
        this.reader = null;
        this.file = file;
    }

    /**
     * Constructs a new {@code PreferencesReader}.
     * @param reader the {@link Reader}
     * @param defaults true when reading from the cache file for default preferences,
     * false for the regular preferences config file
     * @throws XMLStreamException if any XML stream error occurs
     */
    public PreferencesReader(Reader reader, boolean defaults) throws XMLStreamException {
        this.defaults = defaults;
        this.reader = reader;
        this.file = null;
    }

    /**
     * Validate the XML.
     * @param f the file
     * @throws IOException if any I/O error occurs
     * @throws SAXException if any SAX error occurs
     */
    public static void validateXML(File f) throws IOException, SAXException {
        try (BufferedReader in = Files.newBufferedReader(f.toPath(), StandardCharsets.UTF_8)) {
            validateXML(in);
        }
    }

    /**
     * Validate the XML.
     * @param in the {@link Reader}
     * @throws IOException if any I/O error occurs
     * @throws SAXException if any SAX error occurs
     */
    public static void validateXML(Reader in) throws IOException, SAXException {
        try (CachedFile cf = new CachedFile("resource://data/preferences.xsd"); InputStream xsdStream = cf.getInputStream()) {
            Schema schema = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI).newSchema(new StreamSource(xsdStream));
            Validator validator = schema.newValidator();
            validator.validate(new StreamSource(in));
        }
    }

    /**
     * Return the parsed preferences as a settings map
     * @return the parsed preferences as a settings map
     */
    public SortedMap<String, Setting<?>> getSettings() {
        return settings;
    }

    /**
     * Return the version from the XML root element.
     * (Represents the JOSM version when the file was written.)
     * @return the version
     */
    public int getVersion() {
        return version;
    }


    public void parse() throws XMLStreamException, IOException {
        if (reader != null) {
            this.parser = XMLInputFactory.newInstance().createXMLStreamReader(reader);
            doParse();
        } else {
            try (BufferedReader in = Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8)) {
                this.parser = XMLInputFactory.newInstance().createXMLStreamReader(in);
                doParse();
            }
        }
    }

    private void doParse() throws XMLStreamException {
        int event = parser.getEventType();
        while (true) {
            if (event == XMLStreamConstants.START_ELEMENT) {
                String topLevelElementName = defaults ? "preferences-defaults" : "preferences";
                String localName = parser.getLocalName();
                if (!topLevelElementName.equals(localName)) {
                    throw new XMLStreamException(
                            tr("Expected element ''{0}'', but got ''{1}''", topLevelElementName, localName),
                            parser.getLocation());
                }
                try {
                    version = Integer.parseInt(parser.getAttributeValue(null, "version"));
                } catch (NumberFormatException e) {
                    if (Main.isDebugEnabled()) {
                        Main.debug(e.getMessage());
                    }
                }
                parseRoot();
            } else if (event == XMLStreamConstants.END_ELEMENT) {
                return;
            }
            if (parser.hasNext()) {
                event = parser.next();
            } else {
                break;
            }
        }
        parser.close();
    }

    private void parseRoot() throws XMLStreamException {
        while (true) {
            int event = parser.next();
            if (event == XMLStreamConstants.START_ELEMENT) {
                String localName = parser.getLocalName();
                switch(localName) {
                case "tag":
                    Setting setting;
                    if (defaults && isNil()) {
                        setting = new StringSetting(null);
                    } else {
                        String value = parser.getAttributeValue(null, "value");
                        if (value == null) {
                            throw new XMLStreamException(tr("value expected"), parser.getLocation());
                        }
                        setting = new StringSetting(value);
                    }
                    if (defaults) {
                        setting.setTime(Math.round(Double.parseDouble(parser.getAttributeValue(null, "time"))));
                    }
                    settings.put(parser.getAttributeValue(null, "key"), setting);
                    jumpToEnd();
                    break;
                case "list":
                case "lists":
                case "maps":
                    parseToplevelList();
                    break;
                default:
                    throwException("Unexpected element: "+localName);
                }
            } else if (event == XMLStreamConstants.END_ELEMENT) {
                return;
            }
        }
    }

    private void jumpToEnd() throws XMLStreamException {
        while (true) {
            int event = parser.next();
            if (event == XMLStreamConstants.START_ELEMENT) {
                jumpToEnd();
            } else if (event == XMLStreamConstants.END_ELEMENT) {
                return;
            }
        }
    }

    private void parseToplevelList() throws XMLStreamException {
        String key = parser.getAttributeValue(null, "key");
        Long time = null;
        if (defaults) {
            time = Math.round(Double.parseDouble(parser.getAttributeValue(null, "time")));
        }
        String name = parser.getLocalName();

        List<String> entries = null;
        List<List<String>> lists = null;
        List<Map<String, String>> maps = null;
        if (defaults && isNil()) {
            Setting setting;
            switch (name) {
                case "lists":
                    setting = new ListListSetting(null);
                    break;
                case "maps":
                    setting = new MapListSetting(null);
                    break;
                default:
                    setting = new ListSetting(null);
                    break;
            }
            setting.setTime(time);
            settings.put(key, setting);
            jumpToEnd();
        } else {
            while (true) {
                int event = parser.next();
                if (event == XMLStreamConstants.START_ELEMENT) {
                    String localName = parser.getLocalName();
                    switch(localName) {
                    case "entry":
                        if (entries == null) {
                            entries = new ArrayList<>();
                        }
                        entries.add(parser.getAttributeValue(null, "value"));
                        jumpToEnd();
                        break;
                    case "list":
                        if (lists == null) {
                            lists = new ArrayList<>();
                        }
                        lists.add(parseInnerList());
                        break;
                    case "map":
                        if (maps == null) {
                            maps = new ArrayList<>();
                        }
                        maps.add(parseMap());
                        break;
                    default:
                        throwException("Unexpected element: "+localName);
                    }
                } else if (event == XMLStreamConstants.END_ELEMENT) {
                    break;
                }
            }
            Setting setting;
            if (entries != null) {
                setting = new ListSetting(Collections.unmodifiableList(entries));
            } else if (lists != null) {
                setting = new ListListSetting(Collections.unmodifiableList(lists));
            } else if (maps != null) {
                setting = new MapListSetting(Collections.unmodifiableList(maps));
            } else {
                switch (name) {
                    case "lists":
                        setting = new ListListSetting(Collections.<List<String>>emptyList());
                        break;
                    case "maps":
                        setting = new MapListSetting(Collections.<Map<String, String>>emptyList());
                        break;
                    default:
                        setting = new ListSetting(Collections.<String>emptyList());
                        break;
                }
            }
            if (defaults) {
                setting.setTime(time);
            }
            settings.put(key, setting);
        }
    }

    private List<String> parseInnerList() throws XMLStreamException {
        List<String> entries = new ArrayList<>();
        while (true) {
            int event = parser.next();
            if (event == XMLStreamConstants.START_ELEMENT) {
                if ("entry".equals(parser.getLocalName())) {
                    entries.add(parser.getAttributeValue(null, "value"));
                    jumpToEnd();
                } else {
                    throwException("Unexpected element: "+parser.getLocalName());
                }
            } else if (event == XMLStreamConstants.END_ELEMENT) {
                break;
            }
        }
        return Collections.unmodifiableList(entries);
    }

    private Map<String, String> parseMap() throws XMLStreamException {
        Map<String, String> map = new LinkedHashMap<>();
        while (true) {
            int event = parser.next();
            if (event == XMLStreamConstants.START_ELEMENT) {
                if ("tag".equals(parser.getLocalName())) {
                    map.put(parser.getAttributeValue(null, "key"), parser.getAttributeValue(null, "value"));
                    jumpToEnd();
                } else {
                    throwException("Unexpected element: "+parser.getLocalName());
                }
            } else if (event == XMLStreamConstants.END_ELEMENT) {
                break;
            }
        }
        return Collections.unmodifiableMap(map);
    }

    /**
     * Check if the current element is nil (meaning the value of the setting is null).
     * @return true, if the current element is nil
     * @see <a href="https://msdn.microsoft.com/en-us/library/2b314yt2(v=vs.85).aspx">Nillable Attribute on MS Developer Network</a>
     */
    private boolean isNil() {
        String nil = parser.getAttributeValue(XSI_NS, "nil");
        return "true".equals(nil) || "1".equals(nil);
    }

    /**
     * Throw RuntimeException with line and column number.
     *
     * Only use this for errors that should not be possible after schema validation.
     * @param msg the error message
     */
    private void throwException(String msg) {
        throw new RuntimeException(msg + tr(" (at line {0}, column {1})",
                parser.getLocation().getLineNumber(), parser.getLocation().getColumnNumber()));
    }
}
